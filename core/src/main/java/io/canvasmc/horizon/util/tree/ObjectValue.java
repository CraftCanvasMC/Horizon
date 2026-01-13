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
public final class ObjectValue {
    private final @Nullable Object value;
    private final TypeConverterRegistry converters;

    ObjectValue(@Nullable Object value, TypeConverterRegistry converters) {
        this.value = value;
        this.converters = converters;
    }

    /**
     * Gets the raw value
     */
    public @Nullable Object raw() {
        return value;
    }

    /**
     * Checks if the value is null
     */
    public boolean isNull() {
        return value == null;
    }

    /**
     * Converts to String
     *
     * @throws TypeConversionException if conversion fails
     */
    public String asString() {
        return convert(String.class);
    }

    public Optional<String> asStringOptional() {
        return convertOptional(String.class);
    }

    /**
     * Converts to integer
     *
     * @throws TypeConversionException if conversion fails
     */
    public int asInt() {
        return convert(Integer.class);
    }

    public Optional<Integer> asIntOptional() {
        return convertOptional(Integer.class);
    }

    /**
     * Converts to long
     */
    public long asLong() {
        return convert(Long.class);
    }

    public Optional<Long> asLongOptional() {
        return convertOptional(Long.class);
    }

    /**
     * Converts to double
     */
    public double asDouble() {
        return convert(Double.class);
    }

    public Optional<Double> asDoubleOptional() {
        return convertOptional(Double.class);
    }

    /**
     * Converts to float
     */
    public float asFloat() {
        return convert(Float.class);
    }

    public Optional<Float> asFloatOptional() {
        return convertOptional(Float.class);
    }

    /**
     * Converts to boolean
     */
    public boolean asBoolean() {
        return convert(Boolean.class);
    }

    public Optional<Boolean> asBooleanOptional() {
        return convertOptional(Boolean.class);
    }

    /**
     * Converts to {@link BigDecimal}
     */
    public BigDecimal asBigDecimal() {
        return convert(BigDecimal.class);
    }

    public Optional<BigDecimal> asBigDecimalOptional() {
        return convertOptional(BigDecimal.class);
    }

    /**
     * Converts to {@link BigInteger}
     */
    public BigInteger asBigInteger() {
        return convert(BigInteger.class);
    }

    public Optional<BigInteger> asBigIntegerOptional() {
        return convertOptional(BigInteger.class);
    }

    /**
     * Converts to ObjectTree if the value is a tree structure
     *
     * @throws TypeConversionException if the value is not an ObjectTree
     */
    public ObjectTree asTree() {
        if (value == null) {
            throw new TypeConversionException("Cannot convert null to ObjectTree");
        }
        if (!(value instanceof ObjectTree)) {
            throw new TypeConversionException("Value is not an ObjectTree");
        }
        return (ObjectTree) value;
    }

    /**
     * Converts to ObjectTree if the value is a tree structure, returns empty Optional otherwise
     */
    public Optional<ObjectTree> asTreeOptional() {
        if (value instanceof ObjectTree) {
            return Optional.of((ObjectTree) value);
        }
        return Optional.empty();
    }

    /**
     * Converts to a custom type using registered converter
     */
    public <T> T as(Class<T> type) {
        return convert(type);
    }

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
