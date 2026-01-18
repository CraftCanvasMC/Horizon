package io.canvasmc.horizon.util.tree;

import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Wrapper for a value in the ObjectTree type conversion methods.
 */
public final class ObjectValue implements Value<Object> {
    private final @Nullable Object value;
    private final TypeConverterRegistry converters;

    ObjectValue(@Nullable Object value, TypeConverterRegistry converters) {
        this.value = value;
        this.converters = converters;
    }

    @Override
    public @Nullable Object raw() {
        return value;
    }

    @Override
    public boolean isNull() {
        return value == null;
    }

    @Override
    public String asString() {
        return convert(String.class);
    }

    @Override
    public Optional<String> asStringOptional() {
        return convertOptional(String.class);
    }

    @Override
    public int asInt() {
        return convert(Integer.class);
    }

    @Override
    public Optional<Integer> asIntOptional() {
        return convertOptional(Integer.class);
    }

    @Override
    public long asLong() {
        return convert(Long.class);
    }

    @Override
    public Optional<Long> asLongOptional() {
        return convertOptional(Long.class);
    }

    @Override
    public double asDouble() {
        return convert(Double.class);
    }

    @Override
    public Optional<Double> asDoubleOptional() {
        return convertOptional(Double.class);
    }

    @Override
    public float asFloat() {
        return convert(Float.class);
    }

    @Override
    public Optional<Float> asFloatOptional() {
        return convertOptional(Float.class);
    }

    @Override
    public boolean asBoolean() {
        return convert(Boolean.class);
    }

    @Override
    public Optional<Boolean> asBooleanOptional() {
        return convertOptional(Boolean.class);
    }

    @Override
    public BigDecimal asBigDecimal() {
        return convert(BigDecimal.class);
    }

    @Override
    public Optional<BigDecimal> asBigDecimalOptional() {
        return convertOptional(BigDecimal.class);
    }

    @Override
    public BigInteger asBigInteger() {
        return convert(BigInteger.class);
    }

    @Override
    public Optional<BigInteger> asBigIntegerOptional() {
        return convertOptional(BigInteger.class);
    }

    @Override
    public ObjectTree asTree() {
        if (value == null) {
            throw new TypeConversionException("Cannot convert null to ObjectTree");
        }
        if (!(value instanceof ObjectTree)) {
            throw new TypeConversionException("Value is not an ObjectTree");
        }
        return (ObjectTree) value;
    }

    @Override
    public Optional<ObjectTree> asTreeOptional() {
        if (value instanceof ObjectTree) {
            return Optional.of((ObjectTree) value);
        }
        return Optional.empty();
    }

    @Override
    public <T> T as(Class<T> type) {
        return convert(type);
    }

    @Override
    public <T> Optional<T> asOptional(Class<T> type) {
        return convertOptional(type);
    }

    private <T> T convert(Class<T> type) {
        if (value == null) {
            throw new TypeConversionException("Cannot convert null to " + type.getSimpleName());
        }

        // Handle ObjectTree specially - if someone asks for Map, give them the raw map
        if (value instanceof ObjectTree && type == Map.class) {
            //noinspection unchecked
            return (T) ((ObjectTree) value).toRawMap();
        }

        if (type.isInstance(value)) {
            //noinspection unchecked
            return (T) value;
        }

        TypeConverter<T> converter = converters.get(type);
        try {
            return converter.convert(value);
        } catch (Exception e) {
            throw new TypeConversionException(
                "Failed to convert " + value.getClass().getSimpleName() +
                    " to " + type.getSimpleName() + ": " + e.getMessage(),
                e
            );
        }
    }

    private <T> Optional<T> convertOptional(Class<T> type) {
        try {
            return Optional.of(convert(type));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ObjectValue other)) return false;
        return Objects.equals(value, other.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }
}
