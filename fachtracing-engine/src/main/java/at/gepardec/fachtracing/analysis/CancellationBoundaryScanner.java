package at.gepardec.fachtracing.analysis;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Set;

/** Finds the exact JDK future cancellation calls supported by runtime instrumentation. */
public final class CancellationBoundaryScanner {
    private static final Set<String> OWNERS = Set.of(
            "java/util/concurrent/Future",
            "java/util/concurrent/CompletableFuture",
            "java/util/concurrent/ForkJoinTask");

    private CancellationBoundaryScanner() { }

    /** Returns true for one supported bytecode invocation contract. */
    public static boolean matches(String owner, String name, String descriptor) {
        return name.equals("cancel") && descriptor.equals("(Z)Z") && OWNERS.contains(owner);
    }

    /** Returns true when class bytes contain at least one supported cancellation call. */
    public static boolean contains(byte[] bytecode) {
        var found = new boolean[1];
        new ClassReader(bytecode).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override public MethodVisitor visitMethod(
                    int access, String name, String descriptor, String signature, String[] exceptions) {
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override public void visitMethodInsn(
                            int opcode, String owner, String method, String methodDescriptor,
                            boolean isInterface) {
                        if (matches(owner, method, methodDescriptor)) found[0] = true;
                    }
                };
            }
        }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return found[0];
    }
}

