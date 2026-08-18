package at.gepardec.fachtracing.agent;

import at.gepardec.fachtracing.analysis.AnalysisManifest;
import at.gepardec.fachtracing.analysis.CancellationBoundaryScanner;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.Label;
import org.objectweb.asm.Handle;

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
        if (className == null) return null;
        String expected = classFingerprints.get(className);
        if (expected == null || !expected.equals(sha256(classfileBuffer))) return null;
        try {
            byte[] transformed = classfileBuffer;
            Map<MethodBinding, Set<MethodBinding>> lambdaTargets = lambdaTargets(classfileBuffer);
            boolean changed = false;
            for (AnalysisManifest manifest : manifests) {
                if (!isSelectedClass(manifest, className)) continue;
                ClassReader reader = new ClassReader(transformed);
                ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
                reader.accept(new ProbeClassVisitor(writer, className, manifest, lambdaTargets,
                                methodMaxLocals(transformed)),
                        ClassReader.SKIP_FRAMES);
                transformed = writer.toByteArray();
                changed = true;
            }
            if (CancellationBoundaryScanner.contains(transformed)) {
                ClassReader cancellationReader = new ClassReader(transformed);
                ClassWriter cancellationWriter = new ClassWriter(
                        cancellationReader, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
                cancellationReader.accept(new CancellationClassVisitor(cancellationWriter), ClassReader.SKIP_FRAMES);
                transformed = cancellationWriter.toByteArray();
                changed = true;
            }
            return changed ? transformed : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static final class CancellationClassVisitor extends ClassVisitor {
        private CancellationClassVisitor(ClassVisitor delegate) { super(Opcodes.ASM9, delegate); }

        @Override public MethodVisitor visitMethod(
                int access, String name, String descriptor, String signature, String[] exceptions) {
            return new MethodVisitor(Opcodes.ASM9,
                    super.visitMethod(access, name, descriptor, signature, exceptions)) {
                @Override public void visitMethodInsn(
                        int opcode, String owner, String method, String methodDescriptor,
                        boolean isInterface) {
                    boolean cancellation = cancellationBoundary(owner, method, methodDescriptor);
                    if (cancellation) mv.visitInsn(Opcodes.DUP2);
                    super.visitMethodInsn(opcode, owner, method, methodDescriptor, isInterface);
                    if (cancellation) {
                        mv.visitMethodInsn(Opcodes.INVOKESTATIC, RUNTIME, "asyncFutureCancelled",
                                "(Ljava/util/concurrent/Future;ZZ)Z", false);
                    }
                }
            };
        }
    }

    private static boolean cancellationBoundary(String owner, String name, String descriptor) {
        return CancellationBoundaryScanner.matches(owner, name, descriptor);
    }

    private boolean isSelectedClass(String className) {
        return manifests.stream().anyMatch(manifest -> isSelectedClass(manifest, className));
    }

    private static boolean isSelectedClass(AnalysisManifest manifest, String className) {
        return manifest.probeSites().stream().anyMatch(site -> ownerMatches(className, site.ownerHint()))
                || manifest.dispatchTargets().stream().anyMatch(target -> ownerMatches(className, target.ownerHint()))
                || manifest.branchTargets().stream().anyMatch(target -> ownerMatches(className, target.ownerHint()))
                || manifest.controlTargets().stream().anyMatch(target -> ownerMatches(className, target.ownerHint()))
                || manifest.evidenceTargets().stream().anyMatch(target -> ownerMatches(className, target.ownerHint()));
    }

    boolean selects(String binaryClassName) {
        return isSelectedClass(binaryClassName.replace('.', '/'));
    }

    private static boolean ownerMatches(String internalName, String ownerHint) {
        return internalName.replace('/', '.').replace('$', '.').equals(ownerHint);
    }

    private static Map<MethodBinding, Integer> methodMaxLocals(byte[] bytecode) {
        var result = new java.util.LinkedHashMap<MethodBinding, Integer>();
        new ClassReader(bytecode).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override public MethodVisitor visitMethod(
                    int access, String name, String descriptor, String signature, String[] exceptions) {
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override public void visitMaxs(int maxStack, int maxLocals) {
                        result.put(new MethodBinding(name, descriptor), maxLocals);
                    }
                };
            }
        }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return Map.copyOf(result);
    }

    private final class ProbeClassVisitor extends ClassVisitor {
        private final String className;
        private final AnalysisManifest manifest;
        private final Map<MethodBinding, Set<MethodBinding>> lambdaTargets;
        private final Map<MethodBinding, Integer> methodMaxLocals;

        private ProbeClassVisitor(
                ClassVisitor delegate,
                String className,
                AnalysisManifest manifest,
                Map<MethodBinding, Set<MethodBinding>> lambdaTargets,
                Map<MethodBinding, Integer> methodMaxLocals) {
            super(Opcodes.ASM9, delegate);
            this.className = className;
            this.manifest = manifest;
            this.lambdaTargets = lambdaTargets;
            this.methodMaxLocals = methodMaxLocals;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                                         String[] exceptions) {
            MethodVisitor delegate = super.visitMethod(access, name, descriptor, signature, exceptions);
            List<AnalysisManifest.ProbeSite> sites = manifest.probeSites().stream()
                    .filter(site -> ownerMatches(className, site.ownerHint()))
                    .filter(site -> memberMatches(name, descriptor, site.memberHint(), site.descriptorHint()))
                    .toList();
            List<AnalysisManifest.DispatchTarget> targets = manifest.dispatchTargets().stream()
                    .filter(target -> ownerMatches(className, target.ownerHint()))
                    .filter(target -> memberMatches(
                            name, descriptor, target.memberHint(), target.descriptorHint()))
                    .toList();
            List<AnalysisManifest.BranchTarget> branches = manifest.branchTargets().stream()
                    .filter(target -> ownerMatches(className, target.ownerHint()))
                    .filter(target -> memberMatches(
                            name, descriptor, target.memberHint(), target.descriptorHint()))
                    .toList();
            List<AnalysisManifest.ControlTarget> controls = manifest.controlTargets().stream()
                    .filter(target -> ownerMatches(className, target.ownerHint()))
                    .filter(target -> memberMatches(
                            name, descriptor, target.memberHint(), target.descriptorHint()))
                    .toList();
            List<AnalysisManifest.EvidenceTarget> evidence = manifest.evidenceTargets().stream()
                    .filter(target -> ownerMatches(className, target.ownerHint()))
                    .filter(target -> memberMatches(
                            name, descriptor, target.memberHint(), target.descriptorHint()))
                    .toList();
            return sites.isEmpty() && targets.isEmpty() && branches.isEmpty()
                    && controls.isEmpty() && evidence.isEmpty() ? delegate
                    : new ProbeMethodVisitor(delegate, access, descriptor, manifest,
                            sites, targets, branches, controls, evidence,
                            methodMaxLocals.getOrDefault(new MethodBinding(name, descriptor), 0));
        }

        private boolean memberMatches(
                String bytecodeName,
                String bytecodeDescriptor,
                String memberHint,
                String descriptorHint) {
            if (memberHint.equals(bytecodeName)) {
                return descriptorHint.isBlank() || descriptorHint.equals(bytecodeDescriptor);
            }
            if (!memberHint.endsWith("#lambda")) return false;
            String sourceMethod = memberHint.substring(0, memberHint.length() - "#lambda".length());
            if (descriptorHint.isBlank()) return bytecodeName.startsWith("lambda$" + sourceMethod + "$");
            return lambdaTargets.getOrDefault(new MethodBinding(sourceMethod, descriptorHint), Set.of())
                    .contains(new MethodBinding(bytecodeName, bytecodeDescriptor));
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
        private final List<AnalysisManifest.ControlTarget> controls;
        private final List<AnalysisManifest.EvidenceTarget> evidenceTargets;
        private final Label failureStart = new Label();
        private final Label failureEnd = new Label();
        private final Label failureHandler = new Label();
        private int predicateIndex;
        private int dispatchIndex;
        private int outcomeIndex;
        private int previousOriginalOpcode = -1;
        private long sourceLine = -1;
        private final Set<Label> visitedLabels = new java.util.HashSet<>();
        private final int asyncHandleLocal;
        private int nextAsyncLocal;

        private ProbeMethodVisitor(MethodVisitor delegate, int access, String descriptor,
                                   AnalysisManifest manifest,
                                   List<AnalysisManifest.ProbeSite> sites,
                                   List<AnalysisManifest.DispatchTarget> targets,
                                   List<AnalysisManifest.BranchTarget> branches,
                                   List<AnalysisManifest.ControlTarget> controls,
                                   List<AnalysisManifest.EvidenceTarget> evidenceTargets,
                                   int originalMaxLocals) {
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
            this.controls = controls;
            this.evidenceTargets = evidenceTargets;
            this.asyncHandleLocal = originalMaxLocals;
            this.nextAsyncLocal = originalMaxLocals + 1;
        }

        private List<AnalysisManifest.BranchTarget> completeBranchGroups(
                List<AnalysisManifest.BranchTarget> candidates) {
            Map<String, Set<Integer>> expectedIndexes = new java.util.LinkedHashMap<>();
            for (int index = 0; index < predicates.size(); index++) {
                expectedIndexes.computeIfAbsent(predicates.get(index).nodeId(),
                                ignored -> new java.util.LinkedHashSet<>())
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
            controls.stream().filter(target -> target.point() == AnalysisManifest.ControlPoint.LINE)
                    .filter(target -> target.sourceLine() == line).forEach(this::emitControl);
        }

        @Override
        public void visitLabel(Label label) {
            visitedLabels.add(label);
            super.visitLabel(label);
        }

        private void emitReturnControls() {
            controls.stream().filter(target -> target.point() == AnalysisManifest.ControlPoint.RETURN)
                    .filter(target -> target.sourceLine() == sourceLine).forEach(target -> {
                pushGraph();
                push(target.nodeId());
                push(target.edgeId());
                invoke("edgeFor", "(Ljava/lang/String;JLjava/lang/String;Ljava/lang/String;)V");
            });
        }

        private void captureEvidenceArguments(String nodeId) {
            for (AnalysisManifest.EvidenceTarget target : evidenceTargets) {
                if (!target.nodeId().equals(nodeId)) continue;
                if (target.argumentIndex() == -1) {
                    pushGraph();
                    push(target.evidenceLabel());
                    invoke("exactPathUnavailableFor", "(Ljava/lang/String;JLjava/lang/String;)V");
                    continue;
                }
                if (target.argumentIndex() >= argumentTypes.length) continue;
                Type argument = argumentTypes[target.argumentIndex()];
                pushGraph();
                push(target.nodeId());
                push(target.evidenceLabel());
                int local = staticMethod ? 0 : 1;
                for (int index = 0; index < target.argumentIndex(); index++) {
                    local += argumentTypes[index].getSize();
                }
                mv.visitVarInsn(argument.getOpcode(Opcodes.ILOAD), local);
                box(argument);
                invoke("observeEvidenceFor",
                        "(Ljava/lang/String;JLjava/lang/String;Ljava/lang/String;Ljava/lang/Object;)V");
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
            boolean constantBooleanBranch = (opcode == Opcodes.IFEQ || opcode == Opcodes.IFNE)
                    && (previousOriginalOpcode == Opcodes.ICONST_0 || previousOriginalOpcode == Opcodes.ICONST_1);
            if (opcode == Opcodes.GOTO && !visitedLabels.contains(label)) {
                controls.stream().filter(target -> target.point() == AnalysisManifest.ControlPoint.CASE_EXIT)
                        .filter(target -> target.sourceLine() == sourceLine).forEach(this::emitControl);
            }
            if (!constantBooleanBranch && opcode != Opcodes.GOTO && opcode != Opcodes.JSR
                    && predicateIndex < predicates.size()
                    && matchesPredicateSite()) {
                AnalysisManifest.ProbeSite predicate = predicates.get(predicateIndex++);
                int currentPredicateIndex = predicateIndex - 1;
                AnalysisManifest.BranchTarget branch = branches.stream()
                        .filter(target -> target.nodeId().equals(predicate.nodeId()))
                        .filter(target -> target.predicateIndex() == currentPredicateIndex)
                        .findFirst().orElse(null);
                if (branch != null) {
                    captureEvidenceArguments(predicate.nodeId());
                    previousOriginalOpcode = opcode;
                    emitBranch(opcode, label, branch);
                    return;
                }
                emitExactPathGap(predicate);
            }
            previousOriginalOpcode = opcode;
            super.visitJumpInsn(opcode, label);
        }

        @Override
        public void visitIntInsn(int opcode, int operand) {
            previousOriginalOpcode = opcode;
            super.visitIntInsn(opcode, operand);
        }

        @Override
        public void visitVarInsn(int opcode, int variable) {
            previousOriginalOpcode = opcode;
            super.visitVarInsn(opcode, variable);
        }

        @Override
        public void visitTypeInsn(int opcode, String type) {
            previousOriginalOpcode = opcode;
            super.visitTypeInsn(opcode, type);
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
            previousOriginalOpcode = opcode;
            super.visitFieldInsn(opcode, owner, name, descriptor);
        }

        @Override
        public void visitLdcInsn(Object value) {
            previousOriginalOpcode = Opcodes.LDC;
            super.visitLdcInsn(value);
        }

        @Override
        public void visitIincInsn(int variable, int increment) {
            previousOriginalOpcode = Opcodes.IINC;
            super.visitIincInsn(variable, increment);
        }

        @Override
        public void visitInvokeDynamicInsn(String name, String descriptor,
                                           org.objectweb.asm.Handle bootstrapMethodHandle,
                                           Object... bootstrapMethodArguments) {
            previousOriginalOpcode = Opcodes.INVOKEDYNAMIC;
            super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
        }

        @Override
        public void visitTableSwitchInsn(int minimum, int maximum, Label defaultTarget, Label... labels) {
            previousOriginalOpcode = Opcodes.TABLESWITCH;
            super.visitTableSwitchInsn(minimum, maximum, defaultTarget, labels);
        }

        @Override
        public void visitLookupSwitchInsn(Label defaultTarget, int[] keys, Label[] labels) {
            previousOriginalOpcode = Opcodes.LOOKUPSWITCH;
            super.visitLookupSwitchInsn(defaultTarget, keys, labels);
        }

        private void emitControl(AnalysisManifest.ControlTarget target) {
            pushGraph();
            push(target.nodeId());
            push(target.edgeId());
            invoke("edgeFor", "(Ljava/lang/String;JLjava/lang/String;Ljava/lang/String;)V");
        }

        private void emitBranch(int opcode, Label originalTarget, AnalysisManifest.BranchTarget branch) {
            if (branch.completion() == AnalysisManifest.BranchCompletion.BOTH_OUTCOMES
                    || branch.completion() == AnalysisManifest.BranchCompletion.BOTH_OUTCOMES_REVERSED) {
                emitExactBranch(opcode, originalTarget, branch,
                        branch.completion() == AnalysisManifest.BranchCompletion.BOTH_OUTCOMES_REVERSED);
                return;
            }
            String edgeId = branch.completion() == AnalysisManifest.BranchCompletion.JUMP_TRUE
                    ? branch.trueEdgeId() : branch.falseEdgeId();
            Label edgeProbe = new Label();
            Label fallThrough = new Label();
            super.visitJumpInsn(opcode, edgeProbe);
            super.visitJumpInsn(Opcodes.GOTO, fallThrough);
            super.visitLabel(edgeProbe);
            emitEdge(branch.nodeId(), edgeId,
                    branch.completion() == AnalysisManifest.BranchCompletion.JUMP_TRUE);
            super.visitJumpInsn(Opcodes.GOTO, originalTarget);
            super.visitLabel(fallThrough);
        }

        private void emitExactBranch(
                int opcode, Label originalTarget, AnalysisManifest.BranchTarget branch, boolean reversed) {
            Label falseProbe = new Label();
            Label fallThrough = new Label();
            super.visitJumpInsn(opcode, falseProbe);
            emitEdge(branch.nodeId(), reversed ? branch.falseEdgeId() : branch.trueEdgeId(), !reversed);
            super.visitJumpInsn(Opcodes.GOTO, fallThrough);
            super.visitLabel(falseProbe);
            emitEdge(branch.nodeId(), reversed ? branch.trueEdgeId() : branch.falseEdgeId(), reversed);
            super.visitJumpInsn(Opcodes.GOTO, originalTarget);
            super.visitLabel(fallThrough);
        }

        private void emitEdge(String nodeId, String edgeId) {
            emitEdge(nodeId, edgeId, false);
        }

        private void emitEdge(String nodeId, String edgeId, boolean value) {
            pushGraph();
            push(nodeId);
            push(edgeId);
            mv.visitInsn(value ? Opcodes.ICONST_1 : Opcodes.ICONST_0);
            invoke("predicateFor", "(Ljava/lang/String;JLjava/lang/String;Ljava/lang/String;Z)V");
            if (value) {
                controls.stream().filter(target ->
                                target.point() == AnalysisManifest.ControlPoint.PREDICATE_TRUE)
                        .filter(target -> target.sourceLine() == sourceLine).forEach(this::emitControl);
            }
        }

        private void emitExactPathGap(AnalysisManifest.ProbeSite predicate) {
            pushGraph();
            push(predicate.sourceLine() > 0
                    ? "exact Boolean path correlation is unavailable at source line " + predicate.sourceLine()
                    : "exact Boolean path correlation is unavailable at an unknown source line");
            invoke("exactPathUnavailableFor", "(Ljava/lang/String;JLjava/lang/String;)V");
        }

        private boolean matchesSourceLine(AnalysisManifest.ProbeSite site) {
            return site.sourceLine() < 0 || site.sourceLine() == sourceLine;
        }

        private boolean matchesPredicateSite() {
            AnalysisManifest.ProbeSite site = predicates.get(predicateIndex);
            if (matchesSourceLine(site)) return true;
            if (continuesMultilineDisjunction(site)) return true;
            return predicateIndex > 0
                    && site.nodeId().equals(predicates.get(predicateIndex - 1).nodeId());
        }

        private boolean continuesMultilineDisjunction(AnalysisManifest.ProbeSite site) {
            if (predicateIndex < 1 || site.sourceLine() < 1 || sourceLine < 1
                    || site.sourceLine() != sourceLine + 1) return false;
            return branches.stream().anyMatch(branch ->
                    branch.predicateIndex() == predicateIndex - 1
                            && branch.completion()
                            == AnalysisManifest.BranchCompletion.BOTH_OUTCOMES_REVERSED);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor,
                                    boolean isInterface) {
            previousOriginalOpcode = opcode;
            boolean threadStart = owner.equals("java/lang/Thread")
                    && name.equals("start") && descriptor.equals("()V");
            if (threadStart) mv.visitInsn(Opcodes.DUP);
            AsyncInvocationCatalog.Binding async = AsyncInvocationCatalog.find(owner, name, descriptor).orElse(null);
            PreparedAsync prepared = async == null ? null : prepareAsyncArgument(opcode, async);
            boolean asyncPrepared = prepared != null;
            if ((async == null && AsyncInvocationCatalog.isUnmatchedBoundary(owner, name, descriptor))
                    || (async != null && !asyncPrepared)) {
                push(owner.replace('/', '.') + "." + name);
                invoke("unsupportedAsyncBoundary", "(Ljava/lang/String;)V");
            }
            if ((opcode == Opcodes.INVOKEINTERFACE || opcode == Opcodes.INVOKEVIRTUAL)
                    && dispatchIndex < dispatches.size()) {
                AnalysisManifest.ProbeSite next = dispatches.get(dispatchIndex);
                boolean memberMatches = matchesSourceLine(next) && manifest.dispatchTargets().stream()
                        .anyMatch(target -> target.dispatchNodeId().equals(next.nodeId()));
                if (memberMatches) {
                    pushGraph();
                    push(next.nodeId());
                    invoke("expectDispatchFor", "(Ljava/lang/String;JLjava/lang/String;)V");
                    dispatchIndex++;
                }
            }
            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
            if (asyncPrepared && threadConstructor(async)) {
                mv.visitInsn(Opcodes.DUP);
                mv.visitVarInsn(Opcodes.ALOAD, prepared.local());
                invoke("asyncThreadCreated", "(Ljava/lang/Thread;Ljava/lang/Object;)V");
            } else if (asyncPrepared) {
                completeAsyncSubmission(prepared);
            }
            if (threadStart) {
                invoke("asyncThreadStarted", "(Ljava/lang/Thread;)V");
            }
        }

        private PreparedAsync prepareAsyncArgument(int opcode, AsyncInvocationCatalog.Binding binding) {
            Type[] arguments = Type.getArgumentTypes(binding.descriptor());
            int callback = binding.callbackPosition();
            if (threadConstructor(binding) && callback == arguments.length - 1) {
                prepareAsync(binding, arguments[callback]);
                mv.visitInsn(Opcodes.DUP);
                mv.visitVarInsn(Opcodes.ASTORE, asyncHandleLocal);
                return new PreparedAsync(binding, asyncHandleLocal);
            }
            int[] argumentLocals = new int[arguments.length];
            for (int index = 0; index < arguments.length; index++) {
                argumentLocals[index] = allocate(arguments[index]);
            }
            for (int index = arguments.length - 1; index >= 0; index--) {
                mv.visitVarInsn(arguments[index].getOpcode(Opcodes.ISTORE), argumentLocals[index]);
            }
            int receiverLocal = -1;
            if (opcode != Opcodes.INVOKESTATIC) {
                receiverLocal = allocate(Type.getObjectType(binding.owner()));
                mv.visitVarInsn(Opcodes.ASTORE, receiverLocal);
            }
            mv.visitVarInsn(arguments[callback].getOpcode(Opcodes.ILOAD), argumentLocals[callback]);
            prepareAsync(binding, arguments[callback]);
            mv.visitInsn(Opcodes.DUP);
            mv.visitVarInsn(Opcodes.ASTORE, asyncHandleLocal);
            mv.visitVarInsn(arguments[callback].getOpcode(Opcodes.ISTORE), argumentLocals[callback]);
            if (receiverLocal >= 0) mv.visitVarInsn(Opcodes.ALOAD, receiverLocal);
            for (int index = 0; index < arguments.length; index++) {
                mv.visitVarInsn(arguments[index].getOpcode(Opcodes.ILOAD), argumentLocals[index]);
            }
            return new PreparedAsync(binding, asyncHandleLocal);
        }

        private int allocate(Type type) {
            int local = nextAsyncLocal;
            nextAsyncLocal += type.getSize();
            return local;
        }

        private void prepareAsync(AsyncInvocationCatalog.Binding binding, Type callbackType) {
            String callbackDescriptor = callbackType.getDescriptor();
            invoke(binding.wrapper().runtimeMethod(),
                    "(" + callbackDescriptor + ")" + callbackDescriptor);
        }

        private void completeAsyncSubmission(PreparedAsync prepared) {
            if (prepared.binding().stageResult()) {
                mv.visitInsn(Opcodes.DUP);
                mv.visitVarInsn(Opcodes.ALOAD, prepared.local());
                invoke("asyncStageSubmitted",
                        "(Ljava/util/concurrent/CompletionStage;Ljava/lang/Object;)V");
            } else if (prepared.binding().futureResult()) {
                mv.visitInsn(Opcodes.DUP);
                mv.visitVarInsn(Opcodes.ALOAD, prepared.local());
                invoke("asyncFutureSubmitted",
                        "(Ljava/util/concurrent/Future;Ljava/lang/Object;)V");
            } else {
                mv.visitVarInsn(Opcodes.ALOAD, prepared.local());
                invoke("asyncSubmissionSucceeded", "(Ljava/lang/Object;)V");
            }
        }


        private boolean threadConstructor(AsyncInvocationCatalog.Binding binding) {
            return binding.owner().equals("java/lang/Thread") && binding.method().equals("<init>");
        }

        private boolean reference(Type type) {
            return type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY;
        }

        private record PreparedAsync(AsyncInvocationCatalog.Binding binding, int local) { }

        @Override
        public void visitInsn(int opcode) {
            previousOriginalOpcode = opcode;
            if (isReturn(opcode)) emitReturnControls();
            AnalysisManifest.ProbeSite outcome = isReturn(opcode) ? outcomeForCurrentReturn() : null;
            if (outcome != null) {
                captureEvidenceArguments(outcome.nodeId());
                duplicateAndBox(opcode);
                pushGraph();
                push(outcome.nodeId());
                invoke("completeFor", "(Ljava/lang/Object;Ljava/lang/String;JLjava/lang/String;)V");
            }
            super.visitInsn(opcode);
        }

        private AnalysisManifest.ProbeSite outcomeForCurrentReturn() {
            if (outcomes.isEmpty()) return null;
            List<AnalysisManifest.ProbeSite> exact = outcomes.stream()
                    .filter(outcome -> outcome.sourceLine() == sourceLine).toList();
            if (!exact.isEmpty()) return exact.getFirst();
            if (outcomes.size() == 1) return outcomes.getFirst();
            return outcomeIndex < outcomes.size() ? outcomes.get(outcomeIndex++) : null;
        }

        @Override
        public void visitMaxs(int maxStack, int maxLocals) {
            if (entry != null) {
                super.visitTryCatchBlock(failureStart, failureEnd, failureHandler, "java/lang/Throwable");
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

    private static Map<MethodBinding, Set<MethodBinding>> lambdaTargets(byte[] classfileBuffer) {
        var targets = new java.util.LinkedHashMap<MethodBinding, Set<MethodBinding>>();
        new ClassReader(classfileBuffer).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override public MethodVisitor visitMethod(
                    int access, String name, String descriptor, String signature, String[] exceptions) {
                MethodBinding source = new MethodBinding(name, descriptor);
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override public void visitInvokeDynamicInsn(
                            String dynamicName,
                            String dynamicDescriptor,
                            Handle bootstrapMethodHandle,
                            Object... bootstrapMethodArguments) {
                        if (!bootstrapMethodHandle.getOwner().equals("java/lang/invoke/LambdaMetafactory")) return;
                        for (Object argument : bootstrapMethodArguments) {
                            if (argument instanceof Handle handle && handle.getName().startsWith("lambda$")) {
                                targets.computeIfAbsent(source, ignored -> new java.util.LinkedHashSet<>())
                                        .add(new MethodBinding(handle.getName(), handle.getDesc()));
                            }
                        }
                    }
                };
            }
        }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return targets;
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
                    .forEach(site -> keys.add(site.ownerHint() + '#' + site.memberHint()
                            + site.descriptorHint()));
            for (String key : keys) {
                String previous = owners.putIfAbsent(key, manifest.graphId());
                if (previous != null && !previous.equals(manifest.graphId())) {
                    throw new IllegalArgumentException("activation manifests contain duplicate entries at " + key);
                }
            }
        }
    }

    private record MethodBinding(String name, String descriptor) { }
}
