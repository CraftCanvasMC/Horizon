package io.canvasmc.horizon.transformer.widener;

import org.jspecify.annotations.NonNull;

public record Definition(@NonNull TransformOperation operation, @NonNull Data data, String nodeTarget) {

    public interface Data {
    }

    public record ClassData(String clazzName) implements Data {
    }

    public record FieldData(String fieldName) implements Data {
    }

    public record MethodData(String methodDescriptor) implements Data {
    }
}
