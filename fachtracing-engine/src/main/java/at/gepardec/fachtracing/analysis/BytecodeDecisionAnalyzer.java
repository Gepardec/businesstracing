package at.gepardec.fachtracing.analysis;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.zip.ZipFile;

/** Fail-closed bytecode fallback for simple result-relevant Boolean methods. */
public final class BytecodeDecisionAnalyzer {
    /** Analyzes one exact binary method from the configured compiler classpath. */
    public Result analyze(String ownerName, String methodName, String descriptor, List<Path> classpath) {
        String internalName = ownerName.replace('.', '/');
        Optional<ClassBytes> located = locate(internalName, classpath);
        if (located.isEmpty()) return new Gap("compiled decision class is unavailable");
        ClassBytes binary = located.orElseThrow();
        var type = new ClassNode();
        new ClassReader(binary.bytes()).accept(type, ClassReader.SKIP_FRAMES);
        MethodNode method = type.methods.stream()
                .filter(candidate -> candidate.name.equals(methodName) && candidate.desc.equals(descriptor))
                .findFirst().orElse(null);
        if (method == null) return new Gap("compiled decision method is unavailable");
        if ((method.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) != 0) {
            return new Gap("compiled decision method has no analyzable body");
        }
        if (!method.tryCatchBlocks.isEmpty()) return new Gap("binary exception flow is outside the safe fallback");
        if (!Type.getReturnType(descriptor).equals(Type.BOOLEAN_TYPE)) {
            return new Gap("binary fallback currently requires a Boolean result");
        }
        List<AbstractInsnNode> instructions = realInstructions(method);
        if (instructions.stream().anyMatch(BytecodeDecisionAnalyzer::unsafe)) {
            return new Gap("binary method contains an unsupported call, monitor, switch, or dynamic instruction");
        }
        List<JumpInsnNode> conditions = instructions.stream()
                .filter(JumpInsnNode.class::isInstance).map(JumpInsnNode.class::cast)
                .filter(jump -> jump.getOpcode() != Opcodes.GOTO).toList();
        if (conditions.size() != 1) return new Gap("binary method is outside the single-comparison fallback");

        JumpInsnNode condition = conditions.getFirst();
        int conditionIndex = instructions.indexOf(condition);
        SymbolicState state = symbolicState(method, instructions, conditionIndex);
        if (state.gap() != null) return new Gap(state.gap());
        Comparison comparison = comparison(condition.getOpcode(), state.stack());
        if (comparison == null) return new Gap("binary comparison opcode is outside the safe fallback");

        Boolean jumpResult = pathResult(condition.label, Set.of());
        Boolean fallThroughResult = pathResult(condition.getNext(), Set.of());
        if (jumpResult == null || fallThroughResult == null || jumpResult.equals(fallThroughResult)) {
            return new Gap("binary comparison does not lead to proven Boolean results");
        }
        boolean reverse = jumpResult;
        String operator = operator(condition.getOpcode(), reverse);
        if (operator == null) return new Gap("binary comparison opcode is outside the safe fallback");
        String left = comparison.left().label();
        String right = comparison.right().label();
        return new Fragment(left + operator + right, internalName,
                methodName, descriptor, binary.fingerprint());
    }

    private static SymbolicState symbolicState(
            MethodNode method, List<AbstractInsnNode> instructions, int conditionIndex) {
        var stack = new ArrayDeque<Expression>();
        var locals = new HashMap<Integer, Expression>();
        var fields = new LinkedHashMap<String, Expression>();
        boolean staticMethod = (method.access & Opcodes.ACC_STATIC) != 0;
        int slot = staticMethod ? 0 : 1;
        int input = 1;
        for (Type argument : Type.getArgumentTypes(method.desc)) {
            if (isInteger(argument)) locals.put(slot, new Expression("input " + input));
            slot += argument.getSize();
            input++;
        }
        for (int index = 0; index < conditionIndex; index++) {
            AbstractInsnNode instruction = instructions.get(index);
            int opcode = instruction.getOpcode();
            if (instruction instanceof VarInsnNode variable) {
                if (opcode == Opcodes.ALOAD) {
                    stack.push(new Expression("object"));
                } else if (opcode == Opcodes.ILOAD && locals.containsKey(variable.var)) {
                    stack.push(locals.get(variable.var));
                } else if (opcode == Opcodes.ISTORE && !stack.isEmpty()) {
                    locals.put(variable.var, stack.pop());
                } else {
                    return new SymbolicState(stack, "binary input or local value cannot be reconstructed");
                }
                continue;
            }
            Integer constant = integer(instruction);
            if (constant != null) {
                stack.push(new Expression(Integer.toString(constant)));
                continue;
            }
            if (instruction instanceof FieldInsnNode field
                    && (opcode == Opcodes.GETFIELD || opcode == Opcodes.GETSTATIC)
                    && isInteger(Type.getType(field.desc))) {
                if (opcode == Opcodes.GETFIELD && stack.isEmpty()) {
                    return new SymbolicState(stack, "binary field receiver cannot be reconstructed");
                }
                if (opcode == Opcodes.GETFIELD) stack.pop();
                String key = field.owner + "." + field.name + field.desc;
                stack.push(fields.computeIfAbsent(key,
                        ignored -> new Expression("configured value " + (fields.size() + 1))));
                continue;
            }
            String arithmetic = arithmetic(opcode);
            if (arithmetic != null) {
                if (stack.size() < 2) return new SymbolicState(stack, "binary calculation stack is incomplete");
                Expression right = stack.pop();
                Expression left = stack.pop();
                stack.push(new Expression(left.label() + arithmetic + right.label()));
                continue;
            }
            return new SymbolicState(stack, "binary method contains an unsupported calculation instruction");
        }
        return new SymbolicState(stack, null);
    }

    private static Comparison comparison(int opcode, ArrayDeque<Expression> stack) {
        if (opcode >= Opcodes.IF_ICMPEQ && opcode <= Opcodes.IF_ICMPLE) {
            if (stack.size() < 2) return null;
            Expression right = stack.pop();
            Expression left = stack.pop();
            return new Comparison(left, right);
        }
        if (opcode >= Opcodes.IFEQ && opcode <= Opcodes.IFLE) {
            if (stack.isEmpty()) return null;
            return new Comparison(stack.pop(), new Expression("0"));
        }
        return null;
    }

    private static Boolean pathResult(AbstractInsnNode start, Set<AbstractInsnNode> previous) {
        var visited = new java.util.HashSet<>(previous);
        AbstractInsnNode instruction = start;
        Boolean value = null;
        while (instruction != null && visited.add(instruction)) {
            int opcode = instruction.getOpcode();
            if (opcode == Opcodes.ICONST_0) value = false;
            else if (opcode == Opcodes.ICONST_1) value = true;
            else if (opcode == Opcodes.IRETURN) return value;
            else if (instruction instanceof JumpInsnNode jump && opcode == Opcodes.GOTO) {
                instruction = jump.label;
                continue;
            } else if (instruction instanceof JumpInsnNode) {
                return null;
            } else if (opcode >= 0 && opcode != Opcodes.NOP) {
                return null;
            }
            instruction = instruction.getNext();
        }
        return null;
    }

    private static String arithmetic(int opcode) {
        return switch (opcode) {
            case Opcodes.IADD -> " plus ";
            case Opcodes.ISUB -> " minus ";
            case Opcodes.IMUL -> " multiplied by ";
            case Opcodes.IDIV -> " divided by ";
            case Opcodes.IREM -> " remainder after division by ";
            default -> null;
        };
    }

    private static boolean isInteger(Type type) {
        return switch (type.getSort()) {
            case Type.BOOLEAN, Type.BYTE, Type.CHAR, Type.SHORT, Type.INT -> true;
            default -> false;
        };
    }

    private static boolean unsafe(AbstractInsnNode instruction) {
        int opcode = instruction.getOpcode();
        return instruction instanceof MethodInsnNode
                || opcode == Opcodes.INVOKEDYNAMIC
                || opcode == Opcodes.MONITORENTER
                || opcode == Opcodes.MONITOREXIT
                || instruction.getType() == AbstractInsnNode.TABLESWITCH_INSN
                || instruction.getType() == AbstractInsnNode.LOOKUPSWITCH_INSN;
    }

    private static List<AbstractInsnNode> realInstructions(MethodNode method) {
        var result = new ArrayList<AbstractInsnNode>();
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction.getOpcode() >= 0) result.add(instruction);
        }
        return List.copyOf(result);
    }

    private static Integer integer(AbstractInsnNode instruction) {
        if (instruction instanceof IntInsnNode value) return value.operand;
        if (instruction instanceof LdcInsnNode value && value.cst instanceof Integer number) return number;
        return switch (instruction.getOpcode()) {
            case Opcodes.ICONST_M1 -> -1;
            case Opcodes.ICONST_0 -> 0;
            case Opcodes.ICONST_1 -> 1;
            case Opcodes.ICONST_2 -> 2;
            case Opcodes.ICONST_3 -> 3;
            case Opcodes.ICONST_4 -> 4;
            case Opcodes.ICONST_5 -> 5;
            default -> null;
        };
    }

    private static String operator(int opcode, boolean directJumpResult) {
        return switch (opcode) {
            case Opcodes.IF_ICMPEQ, Opcodes.IFEQ -> directJumpResult ? " equals " : " does not equal ";
            case Opcodes.IF_ICMPNE, Opcodes.IFNE -> directJumpResult ? " does not equal " : " equals ";
            case Opcodes.IF_ICMPLT, Opcodes.IFLT -> directJumpResult ? " is below " : " is at least ";
            case Opcodes.IF_ICMPGE, Opcodes.IFGE -> directJumpResult ? " is at least " : " is below ";
            case Opcodes.IF_ICMPGT, Opcodes.IFGT -> directJumpResult ? " is above " : " is at most ";
            case Opcodes.IF_ICMPLE, Opcodes.IFLE -> directJumpResult ? " is at most " : " is above ";
            default -> null;
        };
    }

    private static Optional<ClassBytes> locate(String internalName, List<Path> classpath) {
        String entry = internalName + ".class";
        for (Path location : classpath) {
            try {
                byte[] bytes = null;
                if (Files.isDirectory(location)) {
                    Path file = location.resolve(entry);
                    if (Files.isRegularFile(file)) bytes = Files.readAllBytes(file);
                } else if (Files.isRegularFile(location) && location.toString().endsWith(".jar")) {
                    try (var archive = new ZipFile(location.toFile())) {
                        var item = archive.getEntry(entry);
                        if (item != null) try (InputStream input = archive.getInputStream(item)) {
                            bytes = input.readAllBytes();
                        }
                    }
                }
                if (bytes != null) return Optional.of(new ClassBytes(bytes, sha256(bytes)));
            } catch (IOException failure) {
                throw new IllegalArgumentException("could not read binary decision input " + location, failure);
            }
        }
        return Optional.empty();
    }

    private static String sha256(byte[] bytes) {
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }

    /** Binary fallback result. */
    public sealed interface Result permits Fragment, Gap { }
    /** Proven simple Boolean fragment. */
    public record Fragment(
            String predicateLabel, String ownerName, String methodName, String descriptor, String classFingerprint)
            implements Result { }
    /** Precise reason why the safe fallback refused a binary method. */
    public record Gap(String reason) implements Result { }
    private record ClassBytes(byte[] bytes, String fingerprint) { }
    private record Expression(String label) { }
    private record Comparison(Expression left, Expression right) { }
    private record SymbolicState(ArrayDeque<Expression> stack, String gap) { }
}
