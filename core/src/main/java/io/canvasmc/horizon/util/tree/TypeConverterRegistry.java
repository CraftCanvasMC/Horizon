package io.canvasmc.horizon.util.tree;

import org.jspecify.annotations.NonNull;

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

/**
 * Registry for type converters
 */
public final class TypeConverterRegistry {
    private final Map<Class<?>, TypeConverter<?>> converters = new HashMap<>();
    private final Map<Class<?>, ObjectDeserializer<?>> deserializers = new HashMap<>();

    TypeConverterRegistry() {
        registerDefaults();
    }

    private void registerDefaults() {
        register(String.class, Object::toString);
        register(File.class, obj -> new File(obj.toString()));
        register(Integer.class, obj -> {
            if (obj instanceof Number n) return n.intValue();
            return Integer.parseInt(obj.toString());
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
            String s = obj.toString().toLowerCase();
            return s.equals("true") || s.equals("yes") || s.equals("1");
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
    }

    <T> void register(Class<T> type, TypeConverter<T> converter) {
        converters.put(type, converter);
    }

    <T> void registerDeserializer(Class<T> type, ObjectDeserializer<T> deserializer) {
        deserializers.put(type, deserializer);
    }

    <T> @NonNull TypeConverter<T> get(Class<T> type) {
        //noinspection unchecked
        TypeConverter<T> converter = (TypeConverter<T>) converters.get(type);
        if (converter == null) {
            throw new TypeConversionException("No converter registered for type: " + type.getName());
        }
        return converter;
    }

    <T> @NonNull ObjectDeserializer<T> getDeserializer(Class<T> type) {
        //noinspection unchecked
        ObjectDeserializer<T> deserializer = (ObjectDeserializer<T>) deserializers.get(type);
        if (deserializer == null) {
            throw new TypeConversionException("No deserializer registered for type: " + type.getName());
        }
        return deserializer;
    }
}
