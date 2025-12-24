package io.canvasmc.horizon.util.tree;

import org.jspecify.annotations.NonNull;

import java.util.*;
import java.util.stream.Stream;

/**
 * Represents an {@code Object[]} in an {@link ObjectTree}
 *
 * @author dueris
 */
public final class ObjectArray {
    private final List<Object> items;
    private final TypeConverterRegistry converters;
    private final RemappingContext remappingContext;

    ObjectArray(List<Object> items, TypeConverterRegistry converters, RemappingContext remappingContext) {
        this.items = Collections.unmodifiableList(new ArrayList<>(items));
        this.converters = converters;
        this.remappingContext = remappingContext;
    }

    /**
     * Gets the value at the specified index in the array
     */
    public @NonNull ObjectValue get(int index) {
        return new ObjectValue(items.get(index), converters);
    }

    /**
     * Gets the value at the specified index as an {@link Optional}
     */
    public Optional<ObjectValue> getOptional(int index) {
        if (index < 0 || index >= items.size()) {
            return Optional.empty();
        }
        return Optional.of(get(index));
    }

    /**
     * Gets a tree at the specified index in the array
     */
    public @NonNull ObjectTree getTree(int index) {
        Object value = items.get(index);
        if (!(value instanceof Map)) {
            throw new ClassCastException("Value at index " + index + " is not a tree structure");
        }
        //noinspection unchecked
        return new ObjectTree((Map<String, Object>) value, converters, remappingContext);
    }

    /**
     * Gets a nested array at the specified index in the current array
     */
    public @NonNull ObjectArray getArray(int index) {
        Object value = items.get(index);
        if (!(value instanceof List)) {
            throw new ClassCastException("Value at index " + index + " is not an array");
        }
        //noinspection unchecked
        return new ObjectArray((List<Object>) value, converters, remappingContext);
    }

    /**
     * Returns the size of the array
     */
    public int size() {
        return items.size();
    }

    /**
     * Checks if the array is empty
     */
    public boolean isEmpty() {
        return items.isEmpty();
    }

    /**
     * Returns a stream of ObjectValues in the array
     */
    public Stream<ObjectValue> stream() {
        return items.stream()
            .map(item -> new ObjectValue(item, converters));
    }

    /**
     * Converts all elements to a specific type
     */
    public @NonNull <T> List<T> asList(Class<T> type) {
        return items.stream()
            .map(item -> new ObjectValue(item, converters).as(type))
            .toList();
    }

    @Override
    public String toString() {
        return "ObjectArray" + items;
    }
}
