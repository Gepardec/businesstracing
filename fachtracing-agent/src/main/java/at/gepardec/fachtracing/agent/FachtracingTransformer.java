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
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Injects compact, non-throwing runtime calls only into manifest-selected methods.
 */
public final class FachtracingTransformer implements ClassFileTransformer {
    private static final String RUNTIME = "at/gepardec/fachtracing/runtime/TraceRuntime";
    private final List<AnalysisManifest> manifests;
    private final Map<String, String> classFingerprints;

    public FachtracingTransformer(AnalysisManifest manifest, Map<String, String> classFingerprints) {
        this(List.of(manifest), classFingerprints);
    }

    /** Creates one class transformer for all disjoint method plans in an activation bundle. */
    public FachtracingTransformer(List<AnalysisManifest> manifests, Map<String, String> classFingerprints) {
        this.manifests = List.copyOf(manifests);
        if (this.manifests.isEmpty()) throw new IllegalArgumentException("at least one manifest is required");
        this.classFingerprints = Map.copyOf(classFingerprints);
        rejectOverlappingEntries(this.manifests);
    }

    @Override
    public byte[] transform(Module module, ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        if (className == null || !isSelectedClass(className)) return null;
        String expected = classFingerprints.get(className);
        if (expected == null || !expected.equals(sha256(classfileBuffer))) return null;
        try {
            byte[] transformed = classfileBuffer;
            for (AnalysisManifest manifest : manifests) {
                if (!isSelectedClass(manifest, className)) continue;
                ClassReader reader = new ClassReader(transformed);
                ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
                reader.accept(new ProbeClassVisitor(writer, className, manifest), ClassReader.SKIP_FRAMES);
                transformed = writer.toByteArray();
            }
            return transformed;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private boolean isSelectedClass(String className) {
        return manifests.stream().anyMatch(manifest -> isSelectedClass(manifest, className));
    }

    private static boolean isSelectedClass(AnalysisManifest manifest, String className) {
        return manifest.probeSites().stream().anyMatch(site -> ownerMatches(className, site.ownerHint()))
                || manifest.dispatchTargets().stream().anyMatch(target -> ownerMatches(className, target.ownerHint()))
                || manifest.branchTargets().stream().anyMatch(target -> ownerMatches(className, target.ownerHint()));
    }

    boolean selects(String binaryClassName) {
        return isSelectedClass(binaryClassName.replace('.', '/'));
    }

    private static boolean ownerMatches(String internalName, String ownerHint) {
        return internalName.replace('/', '.').replace('$', '.').equals(ownerHint);
    }

    private final class ProbeClassVisitor extends ClassVisitor {
        private final String className;
        private final AnalysisManifest manifest;

        private ProbeClassVisitor(ClassVisitor delegate, String className, AnalysisManifest manifest) {
            super(Opcodes.ASM9, delegate);
            this.className = className;
            this.manifest = manifest;
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
            List<AnalysisManifest.BranchTarget> branches = manifest.branchTargets().stream()
                    .filter(target -> ownerMatches(className, target.ownerHint()))
                    .filter(target -> memberMatches(name, target.memberHint()))
                    .toList();
            return sites.isEmpty() && targets.isEmpty() && branches.isEmpty() ? delegate
                    : new ProbeMethodVisitor(delegate, access, descriptor, manifest, sites, targets, branches);
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
        private final AnalysisManifest manifest;
        private final Type[] argumentTypes;
        private final boolean staticMethod;
        private final List<AnalysisManifest.ProbeSite> predicates;
        private final AnalysisManifest.ProbeSite entry;
        private final List<AnalysisManifest.ProbeSite> outcomes;
        private final List<AnalysisManifest.ProbeSite> dispatches;
        private final List<AnalysisManifest.DispatchTarget> targets;
        private final List<AnalysisManifest.BranchTarget> branches;
        private final Label failureStart = new Label();
        private final Label failureEnd = new Label();
        private final Label failureHandler = new Label();
        private int predicateIndex;
        private int dispatchIndex;
        private int outcomeIndex;
        private long sourceLine = -1;

        private ProbeMethodVisitor(MethodVisitor delegate, int access, String descriptor,
                                   AnalysisManifest manifest,
                                   List<AnalysisManifest.ProbeSite> sites,
                                   List<AnalysisManifest.DispatchTarget> targets,
                                   List<AnalysisManifest.BranchTarget> branches) {
            super(Opcodes.ASM9, delegate);
            this.manifest = manifest;
            returnType = Type.getReturnType(descriptor);
            argumentTypes = Type.getArgumentTypes(descriptor);
            staticMethod = (access & Opcodes.ACC_STATIC) != 0;
            predicates = sites.stream().filter(site -> site.kind() == AnalysisManifest.ProbeKind.PREDICATE).toList();
            entry = first(sites, AnalysisManifest.ProbeKind.ENTRY);
            outcomes = sites.stream().filter(site -> site.kind() == AnalysisManifest.ProbeKind.OUTCOME).toList();
            dispatches = sites.stream().filter(site -> site.kind() == AnalysisManifest.ProbeKind.DISPATCH).toList();
            this.targets = targets;
            this.branches = completeBranchGroups(branches);
        }

        private List<AnalysisManifest.BranchTarget> completeBranchGroups(
                List<AnalysisManifest.BranchTarget> candidates) {
            Map<String, Set<Integer>> expectedIndexes = new java.util.LinkedHashMap<>();
            for (int index = 0; index < predicates.size(); index++) {
                expectedIndexes.computeIfAbsent(predicates.get(index).nodeId(), ignored -> new java.util.LinkedHashSet<>())
                        .add(index);
            }
            Map<String, Set<Integer>> actualIndexes = candidates.stream().collect(Collectors.groupingBy(
                    AnalysisManifest.BranchTarget::nodeId,
                    java.util.LinkedHashMap::new,
                    Collectors.mapping(AnalysisManifest.BranchTarget::predicateIndex, Collectors.toSet())));
            Map<String, Long> actualCounts = candidates.stream().collect(Collectors.groupingBy(
                    AnalysisManifest.BranchTarget::nodeId, Collectors.counting()));
            Set<String> completeNodes = expectedIndexes.entrySet().stream()
                    .filter(entry -> entry.getValue().equals(actualIndexes.get(entry.getKey())))
                    .filter(entry -> actualCounts.getOrDefault(entry.getKey(), 0L) == entry.getValue().size())
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());
            return candidates.stream().filter(candidate -> completeNodes.contains(candidate.nodeId())).toList();
        }

        @Override
        public void visitCode() {
            super.visitCode();
            if (entry != null) {
                push(manifest.graphId());
                mv.visitLdcInsn(manifest.graphVersion());
                invoke("begin", "(Ljava/lang/String;J)V");
                captureArguments();
                super.visitTryCatchBlock(failureStart, failureEnd, failureHandler, "java/lang/Throwable");
                super.visitLabel(failureStart);
            }
            for (AnalysisManifest.DispatchTarget target : targets) {
                pushGraph();
                push(target.dispatchNodeId());
                push(target.edgeId());
                invoke("selectedEdgeFor", "(Ljava/lang/String;JLjava/lang/String;Ljava/lang/String;)V");
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
                pushGraph();
                push(entry.nodeId());
                push("input " + (index + 1));
                mv.visitVarInsn(argument.getOpcode(Opcodes.ILOAD), local);
                box(argument);
                invoke("observeFor", "(Ljava/lang/String;JLjava/lang/String;Ljava/lang/String;Ljava/lang/Object;)V");
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
                    && matchesPredicateSite()) {
                AnalysisManifest.ProbeSite predicate = predicates.get(predicateIndex++);
                int currentPredicateIndex = predicateIndex - 1;
                AnalysisManifest.BranchTarget branch = branches.stream()
                        .filter(target -> target.nodeId().equals(predicate.nodeId()))
                        .filter(target -> target.predicateIndex() == currentPredicateIndex)
                        .findFirst().orElse(null);
                if (branch != null) {
                    emitBranch(opcode, label, branch);
                    return;
                }
                emitLegacyPredicate(predicate);
            }
            super.visitJumpInsn(opcode, label);
        }

        private void emitBranch(int opcode, Label originalTarget, AnalysisManifest.BranchTarget branch) {
            if (branch.completion() == AnalysisManifest.BranchCompletion.BOTH_OUTCOMES) {
                emitExactBranch(opcode, originalTarget, branch);
                return;
            }
            String edgeId = branch.completion() == AnalysisManifest.BranchCompletion.JUMP_TRUE
                    ? branch.trueEdgeId() : branch.falseEdgeId();
            Label edgeProbe = new Label();
            Label fallThrough = new Label();
            super.visitJumpInsn(opcode, edgeProbe);
            super.visitJumpInsn(Opcodes.GOTO, fallThrough);
            super.visitLabel(edgeProbe);
            emitEdge(branch.nodeId(), edgeId);
            super.visitJumpInsn(Opcodes.GOTO, originalTarget);
            super.visitLabel(fallThrough);
        }

        private void emitExactBranch(
                int opcode, Label originalTarget, AnalysisManifest.BranchTarget branch) {
            Label falseProbe = new Label();
            Label fallThrough = new Label();
            super.visitJumpInsn(opcode, falseProbe);
            emitEdge(branch.nodeId(), branch.trueEdgeId());
            super.visitJumpInsn(Opcodes.GOTO, fallThrough);
            super.visitLabel(falseProbe);
            emitEdge(branch.nodeId(), branch.falseEdgeId());
            super.visitJumpInsn(Opcodes.GOTO, originalTarget);
            super.visitLabel(fallThrough);
        }

        private void emitEdge(String nodeId, String edgeId) {
            pushGraph();
            push(nodeId);
            push(edgeId);
            invoke("edgeFor", "(Ljava/lang/String;JLjava/lang/String;Ljava/lang/String;)V");
        }

        private void emitLegacyPredicate(AnalysisManifest.ProbeSite predicate) {
            pushGraph();
            push(predicate.nodeId());
            push("evaluated");
            mv.visitInsn(Opcodes.ACONST_NULL);
            invoke("observeFor", "(Ljava/lang/String;JLjava/lang/String;Ljava/lang/String;Ljava/lang/Object;)V");
        }

        private boolean matchesSourceLine(AnalysisManifest.ProbeSite site) {
            return site.sourceLine() < 0 || site.sourceLine() == sourceLine;
        }

        private boolean matchesPredicateSite() {
            AnalysisManifest.ProbeSite site = predicates.get(predicateIndex);
            if (matchesSourceLine(site)) return true;
            return predicateIndex > 0
                    && site.nodeId().equals(predicates.get(predicateIndex - 1).nodeId());
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
                    pushGraph();
                    push(next.nodeId());
                    invoke("expectDispatchFor", "(Ljava/lang/String;JLjava/lang/String;)V");
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
                pushGraph();
                push(outcome.nodeId());
                invoke("completeFor", "(Ljava/lang/Object;Ljava/lang/String;JLjava/lang/String;)V");
            }
            super.visitInsn(opcode);
        }

        @Override
        public void visitMaxs(int maxStack, int maxLocals) {
            if (entry != null) {
                super.visitLabel(failureEnd);
                super.visitLabel(failureHandler);
                mv.visitInsn(Opcodes.DUP);
                pushGraph();
                invoke("failFor", "(Ljava/lang/Throwable;Ljava/lang/String;J)V");
                mv.visitInsn(Opcodes.ATHROW);
            }
            super.visitMaxs(maxStack, maxLocals);
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

        private void box(String owner, String descriptor) {
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, owner, "valueOf",
                    "(" + descriptor + ")L" + owner + ";", false);
        }

        private void push(String value) {
            mv.visitLdcInsn(value);
        }

        private void pushGraph() {
            push(manifest.graphId());
            mv.visitLdcInsn(manifest.graphVersion());
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

    private static void rejectOverlappingEntries(List<AnalysisManifest> manifests) {
        var owners = new java.util.LinkedHashMap<String, String>();
        for (AnalysisManifest manifest : manifests) {
            var keys = new java.util.LinkedHashSet<String>();
            manifest.probeSites().stream().filter(site -> site.kind() == AnalysisManifest.ProbeKind.ENTRY)
                    .forEach(site -> keys.add(site.ownerHint() + '#' + site.memberHint()));
            for (String key : keys) {
                String previous = owners.putIfAbsent(key, manifest.graphId());
                if (previous != null && !previous.equals(manifest.graphId())) {
                    throw new IllegalArgumentException("activation manifests contain duplicate entries at " + key);
                }
            }
        }
    }
}
