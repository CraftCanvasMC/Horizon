package io.canvasmc.horizon.transformer.widener;

import org.jspecify.annotations.NonNull;
import org.objectweb.asm.Opcodes;

public interface TransformOperation {

    static @NonNull Builder builder() {
        return new Builder();
    }

    int apply(int flags);

    Access access();

    enum Access implements TransformOperation {
        PUBLIC {
            public int apply(int f) {
                return (f & ~(Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED)) | Opcodes.ACC_PUBLIC;
            }
        },

        PROTECTED {
            public int apply(int f) {
                if ((f & Opcodes.ACC_PUBLIC) != 0) return f;
                return (f & ~Opcodes.ACC_PRIVATE) | Opcodes.ACC_PROTECTED;
            }
        },

        DEFAULT {
            public int apply(int f) {
                return f & ~(Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED | Opcodes.ACC_PRIVATE);
            }
        },

        PRIVATE {
            public int apply(int f) {
                return (f & ~(Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED)) | Opcodes.ACC_PRIVATE;
            }
        };

        @Override
        public Access access() {
            throw new UnsupportedOperationException("Must use TransformOperation$OpImpl#access for this");
        }
    }

    enum Finality implements TransformOperation {
        NONE {
            public int apply(int f) {
                return f;
            }
        },

        ADD {
            public int apply(int f) {
                return f | Opcodes.ACC_FINAL;
            }
        },

        REMOVE {
            public int apply(int f) {
                return f & ~Opcodes.ACC_FINAL;
            }
        };

        @Override
        public Access access() {
            throw new UnsupportedOperationException("Must use TransformOperation$OpImpl#access for this");
        }
    }

    record OpImpl(Access access, Finality finality) implements TransformOperation {

        public int apply(int f) {
            f = access.apply(f);
            return finality.apply(f);
        }
    }

    final class Builder {

        private Access access = Access.DEFAULT;
        private Finality finality = Finality.NONE;

        public Builder access(Access access) {
            this.access = access;
            return this;
        }

        public Builder finality(Finality finality) {
            this.finality = finality;
            return this;
        }

        public @NonNull TransformOperation build() {
            return new OpImpl(access, finality);
        }
    }
}
