package io.canvasmc.horizon.util.tree;

import org.jspecify.annotations.NonNull;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

/**
 * Registry for type converters
 */
public final class TypeConverterRegistry {
    private final Map<Class<?>, TypeConverter<?>> converters = new HashMap<>();
    private final Map<Class<?>, ObjectDeserializer<?>> deserializers = new HashMap<>();

    TypeConverterRegistry() {
        registerDefaults();
    }

    private static @NonNull File getOrCreateFile(String path) {
        File file = new File(path);
        boolean isDirectory = path.endsWith("/") || path.endsWith("\\") || !hasFileExtension(path);

        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                if (!parent.mkdirs() && !parent.exists()) {
                    throw new IOException("Failed to create directories: " + parent);
                }
            }

            if (!file.exists()) {
                if (isDirectory) {
                    if (!file.mkdirs() && !file.exists()) {
                        throw new IOException("Failed to create directory: " + file);
                    }
                } else {
                    if (!file.createNewFile()) {
                        throw new IOException("Failed to create file: " + file);
                    }
                }
            }

            return file;
        } catch (IOException e) {
            throw new RuntimeException("Unable to get or create file: " + path, e);
        }
    }

    private static boolean hasFileExtension(String path) {
        String name = new File(path).getName();
        int lastDot = name.lastIndexOf('.');
        return lastDot > 0 && lastDot < name.length() - 1;
    }

    private void registerDefaults() {
        register(String.class, Object::toString);
        register(File.class, obj -> getOrCreateFile(obj.toString()));
        register(UUID.class, obj -> UUID.fromString(obj.toString()));
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
        registerMapped(AtomicInteger.class, Integer.class, AtomicInteger::new);
        registerMapped(AtomicBoolean.class, Boolean.class, AtomicBoolean::new);
        registerMapped(AtomicLong.class, Long.class, AtomicLong::new);
    }

    public <T> void register(Class<T> type, TypeConverter<T> converter) {
        converters.put(type, converter);
    }

    public <T, B> void registerMapped(Class<T> newType, Class<B> baseType, Function<B, T> mapper) {
        TypeConverter<B> baseConverter = get(baseType);
        register(newType, obj -> {
            B baseValue = baseConverter.convert(obj);
            return mapper.apply(baseValue);
        });
    }

    public <T> void registerDeserializer(Class<T> type, ObjectDeserializer<T> deserializer) {
        deserializers.put(type, deserializer);
    }

    public <T, B> void registerDeserializerMapped(Class<T> newType, Class<B> baseType, Function<B, T> mapper) {
        ObjectDeserializer<B> baseDeserializer = getDeserializer(baseType);
        registerDeserializer(newType, node -> {
            B baseValue = baseDeserializer.deserialize(node);
            return mapper.apply(baseValue);
        });
    }

    public <T> @NonNull TypeConverter<T> get(Class<T> type) {
        //noinspection unchecked
        TypeConverter<T> converter = (TypeConverter<T>) converters.get(type);
        if (converter == null) {
            throw new TypeConversionException("No converter registered for type: " + type.getName());
        }
        return converter;
    }

    public <T> @NonNull ObjectDeserializer<T> getDeserializer(Class<T> type) {
        //noinspection unchecked
        ObjectDeserializer<T> deserializer = (ObjectDeserializer<T>) deserializers.get(type);
        if (deserializer == null) {
            throw new TypeConversionException("No deserializer registered for type: " + type.getName());
        }
        return deserializer;
    }
}
