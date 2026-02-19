package io.canvasmc.horizon.util.tree;

import io.canvasmc.horizon.util.Util;
import org.jspecify.annotations.NonNull;

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

/**
 * Registry for type converters for the ObjectTree API
 *
 * @author dueris
 */
public final class TypeConverterRegistry {
    private final Map<Class<?>, TypeConverter<?>> converters = new ConcurrentHashMap<>();
    private final Map<Class<?>, ObjectDeserializer<?>> deserializers = new ConcurrentHashMap<>();

    TypeConverterRegistry() {
        registerDefaults();
    }

    private void registerDefaults() {
        register(String.class, Object::toString);
        register(Integer.class, obj -> {
            if (obj instanceof Number n) return n.intValue();
            return Integer.parseInt(obj.toString());
        });
        register(Byte.class, obj -> {
            if (obj instanceof Number n) return n.byteValue();
            return Byte.parseByte(obj.toString());
        });
        register(Long.class, obj -> {
            if (obj instanceof Number n) return n.longValue();
            return Long.parseLong(obj.toString());
        });
        register(Double.class, obj -> {
            if (obj instanceof Number n) return n.doubleValue();
            return Double.parseDouble(obj.toString());
        });
        register(Float.class, obj -> {
            if (obj instanceof Number n) return n.floatValue();
            return Float.parseFloat(obj.toString());
        });
        register(Boolean.class, obj -> {
            if (obj instanceof Boolean b) return b;
            String s = obj.toString().trim().toLowerCase();
            return switch (s) {
                case "true", "yes", "1" -> true;
                case "false", "no", "0" -> false;
                default -> throw new TypeConversionException("Unknown boolean value '" + s + "'");
            };
        });
        register(BigDecimal.class, obj -> {
            if (obj instanceof BigDecimal bd) return bd;
            if (obj instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
            return new BigDecimal(obj.toString());
        });
        register(BigInteger.class, obj -> {
            if (obj instanceof BigInteger bi) return bi;
            if (obj instanceof Number n) return BigInteger.valueOf(n.longValue());
            return new BigInteger(obj.toString());
        });
        registerMapped(AtomicInteger.class, Integer.class, AtomicInteger::new);
        registerMapped(AtomicBoolean.class, Boolean.class, AtomicBoolean::new);
        registerMapped(AtomicLong.class, Long.class, AtomicLong::new);
        registerMapped(File.class, String.class, Util::getOrCreateFile);
        registerMapped(UUID.class, String.class, UUID::fromString);
    }

    /**
     * Registers a type converter for a specified class type
     *
     * @param type
     *     the class type
     * @param converter
     *     the Object -> {@link T} type converter
     * @param <T>
     *     the generic type to convert to
     */
    public <T> void register(Class<T> type, TypeConverter<T> converter) {
        converters.put(type, converter);
    }

    /**
     * Registers a mapped converter for a specified class type, based off a specific class type.
     * <p>
     * Other than normal type converters, this one allows specifying a base, which will convert the Object, running
     * through that base converter, and then passing the result to the type converter provided, allowing for things
     * like:
     * <pre>
     * {@code registerMapped(AtomicInteger.class, Integer.class, AtomicInteger::new);}
     * </pre>
     * Which would use the second argument, in the example above it would be an Integer, to base the initial conversion,
     * and the result output would be passed to your type converter.
     * <p>
     * Note: you can layer mapped converters, so this can depend on another mapped type converter.
     *
     * @param newType
     *     the resulting class type to convert to
     * @param baseType
     *     the base conversion type
     * @param mapper
     *     the type converter, taking the resulting value of the base as the input, outputting the converted object
     * @param <E>
     *     the generic type representing the resulting class type
     * @param <S>
     *     the generic type representing the base class type
     *
     * @throws io.canvasmc.horizon.util.tree.TypeConversionException
     *     if the base type isn't registered
     */
    public <E, S> void registerMapped(Class<E> newType, Class<S> baseType, Function<S, E> mapper) {
        TypeConverter<S> baseConverter = get(baseType);
        register(newType, obj -> {
            S baseValue = baseConverter.convert(obj);
            return mapper.apply(baseValue);
        });
    }

    /**
     * Gets the type converter registered to the provided class type.
     * <p>
     * If the class type being requested was registered as a <b>mapped</b> type converter, it will output the compiled
     * type converter. A compiled type converter is a merged version of the base converter and the mapped type
     * converter, allowing to pass {@link java.lang.Object} instances into the returned converter, and returning the
     * result of the registered mapped converter
     *
     * @param type
     *     the converter type
     * @param <T>
     *     the generic type of the converter
     *
     * @return the registered or compiled type converter
     *
     * @throws io.canvasmc.horizon.util.tree.TypeConversionException
     *     if no converter is registered for the type
     * @apiNote Primitive types (e.g. {@code int}, {@code long}) are not supported by default. Use their boxed
     *     equivalents (e.g. {@link Integer}, {@link Long}) unless a primitive converter has been explicitly
     *     registered.
     */
    public <T> @NonNull TypeConverter<T> get(Class<T> type) {
        if (!converters.containsKey(type))
            throw new TypeConversionException("No converter registered for type: " + type.getName());
        //noinspection unchecked
        return (TypeConverter<T>) converters.get(type);
    }

    /**
     * Registers an object deserializer
     * <p>
     * Different from type converters, since type converters transform object <b>values</b>, while object deserializers
     * transform {@link io.canvasmc.horizon.util.tree.ObjectTree} instances
     *
     * @param type
     *     the class type
     * @param deserializer
     *     the {@link io.canvasmc.horizon.util.tree.ObjectTree} -> {@link T} type converter
     * @param <T>
     *     the generic type to convert to
     */
    public <T> void registerDeserializer(Class<T> type, ObjectDeserializer<T> deserializer) {
        deserializers.put(type, deserializer);
    }

    /**
     * Registers a mapped object deserializer
     * <p>
     * Different from type converters, since type converters transform object <b>values</b>, while object deserializers
     * transform {@link io.canvasmc.horizon.util.tree.ObjectTree} instances
     * <p>
     * Similar to mapped type converters, this follows the same structure, allowing to base an object deserializer and
     * then converting based on the result of the base deserializer
     *
     * @param newType
     *     the resulting class type to convert to
     * @param baseType
     *     the base conversion type
     * @param mapper
     *     the object deserializer, taking the resulting value of the base as the input, outputting the converted
     *     object
     * @param <T>
     *     the generic type representing the resulting class type
     * @param <B>
     *     the generic type representing the base class type
     */
    public <T, B> void registerDeserializerMapped(Class<T> newType, Class<B> baseType, Function<B, T> mapper) {
        ObjectDeserializer<B> baseDeserializer = getDeserializer(baseType);
        registerDeserializer(newType, node -> {
            B baseValue = baseDeserializer.deserialize(node);
            return mapper.apply(baseValue);
        });
    }

    /**
     * Gets the object deserializer registered for the provided class type
     * <p>
     * If the class type being requested was registered as a <b>mapped</b> object deserializer, it will output the
     * compiled deserializer. A compiled object deserializer is a merged version of the base deserializer and the mapped
     * deserializer, allowing to pass {@link java.lang.Object} instances into the returned deserializer, and returning
     * the result of the registered mapped deserializer
     *
     * @param type
     *     the deserializer type
     * @param <T>
     *     the generic type of the deserializer
     *
     * @return the registered or compiled deserializer
     *
     * @throws io.canvasmc.horizon.util.tree.TypeConversionException
     *     if no converter is registered for the type
     */
    public <T> @NonNull ObjectDeserializer<T> getDeserializer(Class<T> type) {
        //noinspection unchecked
        ObjectDeserializer<T> deserializer = (ObjectDeserializer<T>) deserializers.get(type);
        if (deserializer == null) {
            throw new TypeConversionException("No deserializer registered for type: " + type.getName());
        }
        return deserializer;
    }
}
