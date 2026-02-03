package io.canvasmc.horizon.util.tree;

import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Optional;

public sealed interface Value<E> permits EmptyValue, ObjectValue {

    /**
     * Gets the raw value
     *
     * @return the raw {@link E}
     */
    @Nullable E raw();

    /**
     * Checks if the value is null
     *
     * @return {@code true} if null, {@code false} otherwise
     */
    boolean isNull();

    /**
     * Converts to String
     *
     * @return the value as a {@link java.lang.String}
     *
     * @throws TypeConversionException
     *     if conversion fails
     */
    @Nullable String asString();

    /**
     * Converts to a String. If unable to, it returns {@link Optional#empty()}
     *
     * @return the optional, empty if conversion fails, present if successful
     */
    Optional<String> asStringOptional();

    /**
     * Converts to integer
     *
     * @return the value as an {@code int}
     *
     * @throws TypeConversionException
     *     if conversion fails
     */
    int asInt();

    /**
     * Converts to a integer. If unable to, it returns {@link Optional#empty()}
     *
     * @return the optional, empty if conversion fails, present if successful
     */
    Optional<Integer> asIntOptional();

    /**
     * Converts to long
     *
     * @return the value as a {@code long}
     *
     * @throws TypeConversionException
     *     if conversion fails
     */
    long asLong();

    /**
     * Converts to a long. If unable to, it returns {@link Optional#empty()}
     *
     * @return the optional, empty if conversion fails, present if successful
     */
    Optional<Long> asLongOptional();

    /**
     * Converts to double
     *
     * @return the value as a {@code double}
     *
     * @throws TypeConversionException
     *     if conversion fails
     */
    double asDouble();

    /**
     * Converts to a double. If unable to, it returns {@link Optional#empty()}
     *
     * @return the optional, empty if conversion fails, present if successful
     */
    Optional<Double> asDoubleOptional();

    /**
     * Converts to float
     *
     * @return the value as a {@code float}
     *
     * @throws TypeConversionException
     *     if conversion fails
     */
    float asFloat();

    /**
     * Converts to a float. If unable to, it returns {@link Optional#empty()}
     *
     * @return the optional, empty if conversion fails, present if successful
     */
    Optional<Float> asFloatOptional();

    /**
     * Converts to bool
     *
     * @return the value as a {@code boolean}
     *
     * @throws TypeConversionException
     *     if conversion fails
     */
    boolean asBoolean();

    /**
     * Converts to a bool. If unable to, it returns {@link Optional#empty()}
     *
     * @return the optional, empty if conversion fails, present if successful
     */
    Optional<Boolean> asBooleanOptional();

    /**
     * Converts to {@link BigDecimal}
     *
     * @return the value as a {@link java.math.BigDecimal}
     *
     * @throws TypeConversionException
     *     if conversion fails
     */
    BigDecimal asBigDecimal();

    /**
     * Converts to a {@link BigDecimal}. If unable to, it returns {@link Optional#empty()}
     *
     * @return the optional, empty if conversion fails, present if successful
     */
    Optional<BigDecimal> asBigDecimalOptional();

    /**
     * Converts to {@link BigInteger}
     *
     * @return the value as a {@link java.math.BigInteger}
     *
     * @throws TypeConversionException
     *     if conversion fails
     */
    BigInteger asBigInteger();

    /**
     * Converts to a {@link BigInteger}. If unable to, it returns {@link Optional#empty()}
     *
     * @return the optional, empty if conversion fails, present if successful
     */
    Optional<BigInteger> asBigIntegerOptional();

    /**
     * Converts to ObjectTree if the value is a tree structure
     *
     * @return the value as an {@link io.canvasmc.horizon.util.tree.ObjectTree}
     *
     * @throws TypeConversionException
     *     if the value is not an ObjectTree
     */
    ObjectTree asTree();

    /**
     * Converts to ObjectTree if the value is a tree structure, returns empty Optional otherwise
     *
     * @return the optional, empty if conversion fails, present if successful
     */
    Optional<ObjectTree> asTreeOptional();

    /**
     * Converts to a custom type using registered converter
     *
     * @return the value converted to a custom type {@link T}
     *
     * @throws TypeConversionException
     *     if conversion fails
     */
    <T> T as(Class<T> type);

    /**
     * Converts to a custom type using a registered converter. If unable to, it returns {@link Optional#empty()}
     *
     * @return the optional, empty if conversion fails, present if successful
     */
    <T> Optional<T> asOptional(Class<T> type);
}
