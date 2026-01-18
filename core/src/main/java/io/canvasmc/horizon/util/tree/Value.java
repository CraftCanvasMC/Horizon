package io.canvasmc.horizon.util.tree;

import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Optional;

public sealed interface Value<E> permits EmptyValue, ObjectValue {

    /**
     * Gets the raw value
     */
    @Nullable E raw();

    /**
     * Checks if the value is null
     */
    boolean isNull();

    /**
     * Converts to String
     *
     * @throws TypeConversionException if conversion fails
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
     * @throws TypeConversionException if conversion fails
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
     * @throws TypeConversionException if the value is not an ObjectTree
     */
    ObjectTree asTree();

    /**
     * Converts to ObjectTree if the value is a tree structure, returns empty Optional otherwise
     */
    Optional<ObjectTree> asTreeOptional();

    /**
     * Converts to a custom type using registered converter
     */
    <T> T as(Class<T> type);

    /**
     * Converts to a custom type using a registered converter. If unable to, it returns {@link Optional#empty()}
     *
     * @return the optional, empty if conversion fails, present if successful
     */
    <T> Optional<T> asOptional(Class<T> type);
}
