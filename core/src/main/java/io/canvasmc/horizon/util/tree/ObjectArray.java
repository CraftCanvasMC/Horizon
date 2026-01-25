package io.canvasmc.horizon.util.tree;

import org.jetbrains.annotations.Unmodifiable;
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
        this.items = Collections.unmodifiableList(normalizeList(new ArrayList<>(items), converters, remappingContext));
        this.converters = converters;
        this.remappingContext = remappingContext;
    }

    /**
     * Normalizes a list by converting all Map instances to ObjectTree instances
     */
    private static @NonNull List<Object> normalizeList(@NonNull List<Object> list, TypeConverterRegistry converters, RemappingContext remappingContext) {
        List<Object> normalized = new ArrayList<>();
        for (Object item : list) {
            normalized.add(normalizeValue(item, converters, remappingContext));
        }
        return normalized;
    }

    /**
     * Normalizes a value by converting Maps to ObjectTrees and recursively normalizing nested structures
     */
    private static Object normalizeValue(Object value, TypeConverterRegistry converters, RemappingContext remappingContext) {
        if (value instanceof Map) {
            //noinspection unchecked
            return new ObjectTree((Map<String, Object>) value, converters, remappingContext);
        }
        else if (value instanceof List) {
            //noinspection unchecked
            return normalizeList((List<Object>) value, converters, remappingContext);
        }
        return value;
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
        if (!(value instanceof ObjectTree)) {
            throw new ClassCastException("Value at index " + index + " is not a tree structure");
        }
        return (ObjectTree) value;
    }

    /**
     * Gets an optional tree at the specified index
     */
    public Optional<ObjectTree> getTreeOptional(int index) {
        if (index < 0 || index >= items.size()) {
            return Optional.empty();
        }
        Object value = items.get(index);
        if (value instanceof ObjectTree) {
            return Optional.of((ObjectTree) value);
        }
        return Optional.empty();
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
     * Gets an optional array at the specified index
     */
    public Optional<ObjectArray> getArrayOptional(int index) {
        if (index < 0 || index >= items.size()) {
            return Optional.empty();
        }
        Object value = items.get(index);
        if (value instanceof List) {
            //noinspection unchecked
            return Optional.of(new ObjectArray((List<Object>) value, converters, remappingContext));
        }
        return Optional.empty();
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
    public @NonNull <T> @Unmodifiable List<T> asList(Class<T> type) {
        return items.stream()
            .map(item -> new ObjectValue(item, converters).as(type))
            .toList();
    }

    /**
     * Returns the raw list (for internal use, primarily serialization)
     */
    List<Object> getRawList() {
        return items;
    }

    @Override
    public String toString() {
        return "ObjectArray" + items;
    }
}
