package io.canvasmc.horizon.instrument;

import io.canvasmc.horizon.Horizon;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.objectweb.asm.*;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

// TODO - this is bullshit i dont like it remove this
public record PaperclipTransformer(String target) implements ClassFileTransformer {
    public PaperclipTransformer(final @NonNull String target) {
        this.target = target;
    }

    @Override
    public byte @Nullable [] transform(final ClassLoader loader, final @NonNull String className, final Class<?> classBeingRedefined,
                                       final ProtectionDomain protectionDomain, final byte[] classFileBuffer) throws IllegalClassFormatException {
        if (!className.equals(this.target)) return null;
        final ClassReader reader = new ClassReader(classFileBuffer);
        final ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        reader.accept(new PaperclipClassVisitor(writer), ClassReader.EXPAND_FRAMES);
        return writer.toByteArray();
    }

    private static final class PaperclipClassVisitor extends ClassVisitor {
        private PaperclipClassVisitor(final @NonNull ClassVisitor visitor) {
            super(Horizon.ASM_VERSION, visitor);
        }

        @Override
        public @NonNull MethodVisitor visitMethod(final int access, final @NonNull String name, final @NonNull String descriptor, final @NonNull String signature, final @NonNull String[] exceptions) {
            final MethodVisitor mv = this.cv.visitMethod(access, name, descriptor, signature, exceptions);
            return new PaperclipMethodVisitor(descriptor, mv);
        }
    }

    private static final class PaperclipMethodVisitor extends MethodVisitor {
        private final String descriptor;

        private PaperclipMethodVisitor(final @NonNull String descriptor, final @NonNull MethodVisitor visitor) {
            super(Horizon.ASM_VERSION, visitor);

            this.descriptor = descriptor;
        }

        @Override
        public void visitMethodInsn(final int opcode, final @NonNull String owner, final @NonNull String name,
                                    final @NonNull String descriptor, final boolean isInterface) {
            if (name.equals("setupClasspath")) {
                super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                this.visitInsn(Opcodes.RETURN);
                return;
            }

            if (owner.equals("java/lang/System") && name.equals("exit")) {
                if (this.descriptor.endsWith("V")) {
                    this.visitInsn(Opcodes.RETURN);
                } else {
                    this.visitInsn(Opcodes.ACONST_NULL);
                    this.visitInsn(Opcodes.ARETURN);
                }
            }

            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        }
    }
}
