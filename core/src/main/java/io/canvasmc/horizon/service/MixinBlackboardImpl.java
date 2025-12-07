package io.canvasmc.horizon.service;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.service.IGlobalPropertyService;
import org.spongepowered.asm.service.IPropertyKey;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class MixinBlackboardImpl implements IGlobalPropertyService {

    private final Map<String, IPropertyKey> keys = new HashMap<>();
    private final PropertyStore store = new PropertyStore();

    @Override
    public IPropertyKey resolveKey(final @NonNull String name) {
        return this.keys.computeIfAbsent(name, key -> new Property<>(key, Object.class));
    }

    @Override
    public <T> T getProperty(final @NonNull IPropertyKey key) {
        return this.getProperty(key, null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setProperty(final @NonNull IPropertyKey key, final @NonNull Object value) {
        this.store.set((Property<Object>) key, value);
    }

    @Override
    @Nullable
    @SuppressWarnings("unchecked")
    public String getPropertyString(final @NonNull IPropertyKey key, final @Nullable String defaultValue) {
        return this.store.get((Property<String>) key, defaultValue);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> @Nullable T getProperty(final @NonNull IPropertyKey key, final @Nullable T defaultValue) {
        return this.store.get((Property<T>) key, defaultValue);
    }

    private static final class PropertyStore {
        private final ConcurrentHashMap<Property<?>, Object> values = new ConcurrentHashMap<>();

        <T> void set(Property<T> property, T value) {
            values.put(property, value);
        }

        @SuppressWarnings("unchecked")
        <T> T get(Property<T> property, T def) {
            return (T) values.getOrDefault(property, def);
        }
    }

    private record Property<T>(String name, Class<T> type) implements IPropertyKey {
    }
}
