package at.gepardec.fachtracing.agent;

import at.gepardec.fachtracing.analysis.AnalysisManifest;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.Label;

import java.lang.instrument.ClassFileTransformer;
import java.security.MessageDigest;
import java.security.ProtectionDomain;
import java.util.List;
import java.util.Map;

/**
 * Injects compact, non-throwing runtime calls only into manifest-selected methods.
 */
public final class FachtracingTransformer implements ClassFileTransformer {
    private static final String RUNTIME = "at/gepardec/fachtracing/runtime/TraceRuntime";
    private final AnalysisManifest manifest;
    private final Map<String, String> classFingerprints;

    public FachtracingTransformer(AnalysisManifest manifest, Map<String, String> classFingerprints) {
        this.manifest = manifest;
        this.classFingerprints = Map.copyOf(classFingerprints);
    }

    @Override
    public byte[] transform(Module module, ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        if (className == null || !isSelectedClass(className)) return null;
        String expected = classFingerprints.get(className);
        if (expected == null || !expected.equals(sha256(classfileBuffer))) return null;
        try {
            ClassReader reader = new ClassReader(classfileBuffer);
            ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
            reader.accept(new ProbeClassVisitor(writer, className), ClassReader.SKIP_FRAMES);
            return writer.toByteArray();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private boolean isSelectedClass(String className) {
        return manifest.probeSites().stream().anyMatch(site -> ownerMatches(className, site.ownerHint()))
                || manifest.dispatchTargets().stream().anyMatch(target -> ownerMatches(className, target.ownerHint()));
    }

    boolean selects(String binaryClassName) {
        return isSelectedClass(binaryClassName.replace('.', '/'));
    }

    private static boolean ownerMatches(String internalName, String ownerHint) {
        return internalName.replace('/', '.').replace('$', '.').equals(ownerHint);
    }

    private final class ProbeClassVisitor extends ClassVisitor {
        private final String className;

        private ProbeClassVisitor(ClassVisitor delegate, String className) {
            super(Opcodes.ASM9, delegate);
            this.className = className;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                                         String[] exceptions) {
            MethodVisitor delegate = super.visitMethod(access, name, descriptor, signature, exceptions);
            List<AnalysisManifest.ProbeSite> sites = manifest.probeSites().stream()
                    .filter(site -> ownerMatches(className, site.ownerHint()))
                    .filter(site -> memberMatches(name, site.memberHint()))
                    .toList();
            List<AnalysisManifest.DispatchTarget> targets = manifest.dispatchTargets().stream()
                    .filter(target -> ownerMatches(className, target.ownerHint()))
                    .filter(target -> target.memberHint().equals(name))
                    .toList();
            return sites.isEmpty() && targets.isEmpty() ? delegate
                    : new ProbeMethodVisitor(delegate, access, descriptor, sites, targets);
        }

        private boolean memberMatches(String bytecodeName, String memberHint) {
            if (memberHint.equals(bytecodeName)) return true;
            if (!memberHint.endsWith("#lambda")) return false;
            String sourceMethod = memberHint.substring(0, memberHint.length() - "#lambda".length());
            return bytecodeName.startsWith("lambda$" + sourceMethod + "$");
        }
    }

    private final class ProbeMethodVisitor extends MethodVisitor {
        private final Type returnType;
        private final Type[] argumentTypes;
        private final boolean staticMethod;
        private final List<AnalysisManifest.ProbeSite> predicates;
        private final AnalysisManifest.ProbeSite entry;
        private final List<AnalysisManifest.ProbeSite> outcomes;
        private final List<AnalysisManifest.ProbeSite> dispatches;
        private final List<AnalysisManifest.DispatchTarget> targets;
        private int predicateIndex;
        private int dispatchIndex;
        private int outcomeIndex;
        private long sourceLine = -1;

        private ProbeMethodVisitor(MethodVisitor delegate, int access, String descriptor,
                                   List<AnalysisManifest.ProbeSite> sites,
                                   List<AnalysisManifest.DispatchTarget> targets) {
            super(Opcodes.ASM9, delegate);
            returnType = Type.getReturnType(descriptor);
            argumentTypes = Type.getArgumentTypes(descriptor);
            staticMethod = (access & Opcodes.ACC_STATIC) != 0;
            predicates = sites.stream().filter(site -> site.kind() == AnalysisManifest.ProbeKind.PREDICATE).toList();
            entry = first(sites, AnalysisManifest.ProbeKind.ENTRY);
            outcomes = sites.stream().filter(site -> site.kind() == AnalysisManifest.ProbeKind.OUTCOME).toList();
            dispatches = sites.stream().filter(site -> site.kind() == AnalysisManifest.ProbeKind.DISPATCH).toList();
            this.targets = targets;
        }

        @Override
        public void visitCode() {
            super.visitCode();
            if (entry != null) {
                push(manifest.graphId());
                mv.visitLdcInsn(manifest.graphVersion());
                invoke("begin", "(Ljava/lang/String;J)V");
                captureArguments();
            }
            for (AnalysisManifest.DispatchTarget target : targets) {
                push(target.dispatchNodeId());
                push(target.edgeId());
                invoke("selectedEdge", "(Ljava/lang/String;Ljava/lang/String;)V");
            }
        }

        @Override
        public void visitLineNumber(int line, Label start) {
            sourceLine = line;
            super.visitLineNumber(line, start);
        }

        private void captureArguments() {
            int local = staticMethod ? 0 : 1;
            for (int index = 0; index < argumentTypes.length; index++) {
                Type argument = argumentTypes[index];
                push(entry.nodeId());
                push("input " + (index + 1));
                mv.visitVarInsn(argument.getOpcode(Opcodes.ILOAD), local);
                box(argument);
                invoke("observe", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Object;)V");
                local += argument.getSize();
            }
        }

        private void box(Type type) {
            switch (type.getSort()) {
                case Type.BOOLEAN -> box("java/lang/Boolean", "Z");
                case Type.BYTE -> box("java/lang/Byte", "B");
                case Type.CHAR -> box("java/lang/Character", "C");
                case Type.SHORT -> box("java/lang/Short", "S");
                case Type.INT -> box("java/lang/Integer", "I");
                case Type.FLOAT -> box("java/lang/Float", "F");
                case Type.LONG -> box("java/lang/Long", "J");
                case Type.DOUBLE -> box("java/lang/Double", "D");
                default -> {
                }
            }
        }

        @Override
        public void visitJumpInsn(int opcode, org.objectweb.asm.Label label) {
            if (opcode != Opcodes.GOTO && opcode != Opcodes.JSR && predicateIndex < predicates.size()
                    && matchesSourceLine(predicates.get(predicateIndex))) {
                AnalysisManifest.ProbeSite predicate = predicates.get(predicateIndex++);
                push(predicate.nodeId());
                push("evaluated");
                mv.visitInsn(Opcodes.ACONST_NULL);
                invoke("observe", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Object;)V");
            }
            super.visitJumpInsn(opcode, label);
        }

        private boolean matchesSourceLine(AnalysisManifest.ProbeSite site) {
            return site.sourceLine() < 0 || site.sourceLine() == sourceLine;
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor,
                                    boolean isInterface) {
            if ((opcode == Opcodes.INVOKEINTERFACE || opcode == Opcodes.INVOKEVIRTUAL)
                    && dispatchIndex < dispatches.size()) {
                AnalysisManifest.ProbeSite next = dispatches.get(dispatchIndex);
                boolean memberMatches = manifest.dispatchTargets().stream()
                        .filter(target -> target.dispatchNodeId().equals(next.nodeId()))
                        .anyMatch(target -> target.memberHint().equals(name));
                if (memberMatches) {
                    push(next.nodeId());
                    invoke("expectDispatch", "(Ljava/lang/String;)V");
                    dispatchIndex++;
                }
            }
            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        }

        @Override
        public void visitInsn(int opcode) {
            if (isReturn(opcode) && !outcomes.isEmpty()
                    && (outcomes.size() == 1 || outcomeIndex < outcomes.size())) {
                AnalysisManifest.ProbeSite outcome = outcomes.size() == 1
                        ? outcomes.getFirst()
                        : outcomes.get(outcomeIndex++);
                duplicateAndBox(opcode);
                pushUnderResult(outcome.nodeId());
                invoke("complete", "(Ljava/lang/String;Ljava/lang/Object;)V");
            } else if (opcode == Opcodes.ATHROW) {
                mv.visitInsn(Opcodes.DUP);
                invoke("fail", "(Ljava/lang/Throwable;)V");
            }
            super.visitInsn(opcode);
        }

        private void duplicateAndBox(int opcode) {
            switch (opcode) {
                case Opcodes.IRETURN -> {
                    mv.visitInsn(Opcodes.DUP);
                    String wrapper = returnType.getSort() == Type.BOOLEAN ? "java/lang/Boolean" : "java/lang/Integer";
                    String argument = returnType.getSort() == Type.BOOLEAN ? "Z" : "I";
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, wrapper, "valueOf",
                            "(" + argument + ")L" + wrapper + ";", false);
                }
                case Opcodes.LRETURN -> {
                    mv.visitInsn(Opcodes.DUP2);
                    box("java/lang/Long", "J");
                }
                case Opcodes.FRETURN -> {
                    mv.visitInsn(Opcodes.DUP);
                    box("java/lang/Float", "F");
                }
                case Opcodes.DRETURN -> {
                    mv.visitInsn(Opcodes.DUP2);
                    box("java/lang/Double", "D");
                }
                case Opcodes.ARETURN -> mv.visitInsn(Opcodes.DUP);
                default -> mv.visitInsn(Opcodes.ACONST_NULL);
            }
        }

        private void pushUnderResult(String nodeId) {
            push(nodeId);
            mv.visitInsn(Opcodes.SWAP);
        }

        private void box(String owner, String descriptor) {
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, owner, "valueOf",
                    "(" + descriptor + ")L" + owner + ";", false);
        }

        private void push(String value) {
            mv.visitLdcInsn(value);
        }

        private void invoke(String name, String descriptor) {
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, RUNTIME, name, descriptor, false);
        }
    }

    private static AnalysisManifest.ProbeSite first(
            List<AnalysisManifest.ProbeSite> sites, AnalysisManifest.ProbeKind kind) {
        return sites.stream().filter(site -> site.kind() == kind).findFirst().orElse(null);
    }

    private static boolean isReturn(int opcode) {
        return opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN;
    }

    private static String sha256(byte[] bytes) {
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (Exception impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
