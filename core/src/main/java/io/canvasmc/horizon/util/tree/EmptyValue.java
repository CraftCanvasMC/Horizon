package io.canvasmc.horizon.util.tree;

import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Optional;

/**
 * Represents an empty or null value returned from the ObjectTree API.
 *
 * @param <E>
 *     the generic type of the value
 *
 * @author dueris
 * @apiNote All Optionals will return empty, all attempts at conversion will throw
 *     {@link io.canvasmc.horizon.util.tree.TypeConversionException}
 */
public final class EmptyValue<E> implements Value<E> {

    @Override
    public @Nullable E raw() {
        return null;
    }

    @Override
    public boolean isNull() {
        return true;
    }

    @Contract(pure = true)
    @Override
    public @Nullable String asString() {
        throw new TypeConversionException("Empty value, not String");
    }

    @Contract(pure = true)
    @Override
    public @NonNull Optional<String> asStringOptional() {
        return Optional.empty();
    }

    @Override
    public int asInt() {
        throw new TypeConversionException("Empty value, not integer");
    }

    @Contract(pure = true)
    @Override
    public @NonNull Optional<Integer> asIntOptional() {
        return Optional.empty();
    }

    @Override
    public long asLong() {
        throw new TypeConversionException("Empty value, not long");
    }

    @Contract(pure = true)
    @Override
    public @NonNull Optional<Long> asLongOptional() {
        return Optional.empty();
    }

    @Override
    public double asDouble() {
        throw new TypeConversionException("Empty value, not double");
    }

    @Contract(pure = true)
    @Override
    public @NonNull Optional<Double> asDoubleOptional() {
        return Optional.empty();
    }

    @Override
    public float asFloat() {
        throw new TypeConversionException("Empty value, not float");
    }

    @Contract(pure = true)
    @Override
    public @NonNull Optional<Float> asFloatOptional() {
        return Optional.empty();
    }

    @Override
    public boolean asBoolean() {
        throw new TypeConversionException("Empty value, not bool");
    }

    @Contract(pure = true)
    @Override
    public @NonNull Optional<Boolean> asBooleanOptional() {
        return Optional.empty();
    }

    @Override
    public BigDecimal asBigDecimal() {
        throw new TypeConversionException("Empty value, not big decimal");
    }

    @Contract(pure = true)
    @Override
    public @NonNull Optional<BigDecimal> asBigDecimalOptional() {
        return Optional.empty();
    }

    @Override
    public BigInteger asBigInteger() {
        throw new TypeConversionException("Empty value, not big integer");
    }

    @Contract(pure = true)
    @Override
    public @NonNull Optional<BigInteger> asBigIntegerOptional() {
        return Optional.empty();
    }

    @Override
    public ObjectTree asTree() {
        throw new TypeConversionException("Empty value, not object tree");
    }

    @Contract(pure = true)
    @Override
    public @NonNull Optional<ObjectTree> asTreeOptional() {
        return Optional.empty();
    }

    @Contract("_ -> fail")
    @Override
    public <T> T as(@NonNull Class<T> type) {
        throw new TypeConversionException("Empty value, not " + type.getSimpleName());
    }

    @Contract(pure = true)
    @Override
    public <T> @NonNull Optional<T> asOptional(Class<T> type) {
        return Optional.empty();
    }
}
