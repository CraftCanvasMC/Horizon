package io.canvasmc.horizon.util.tree;

import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.*;

/**
 * Immutable tree structure representing hierarchical configuration data. Supports reading from YAML, JSON, TOML, and
 * Properties formats.
 *
 * @author dueris
 */
public final class ObjectTree {
    private final Map<String, Object> data;
    private final TypeConverterRegistry converters;
    private final RemappingContext remappingContext;

    /**
     * Normalizes the data map by converting all Map instances to ObjectTree instances
     */
    private static @NonNull Map<String, Object> normalizeData(@NonNull Map<String, Object> data, TypeConverterRegistry converters, RemappingContext remappingContext) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            normalized.put(entry.getKey(), normalizeValue(entry.getValue(), converters, remappingContext));
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
            List<Object> list = (List<Object>) value;
            List<Object> normalized = new ArrayList<>();
            for (Object item : list) {
                normalized.add(normalizeValue(item, converters, remappingContext));
            }
            return normalized;
        }
        return value;
    }

    /**
     * Converts ObjectTree instances back to Maps for serialization
     */
    private static Object denormalizeValue(Object value) {
        if (value instanceof ObjectTree) {
            return ((ObjectTree) value).toRawMap();
        }
        else if (value instanceof List) {
            //noinspection unchecked
            List<Object> list = (List<Object>) value;
            List<Object> denormalized = new ArrayList<>();
            for (Object item : list) {
                denormalized.add(denormalizeValue(item));
            }
            return denormalized;
        }
        return value;
    }

    ObjectTree(Map<String, Object> data, TypeConverterRegistry converters, RemappingContext remappingContext) {
        this.data = Collections.unmodifiableMap(normalizeData(data, converters, remappingContext));
        this.converters = converters;
        this.remappingContext = remappingContext;
    }

    /**
     * Creates a new read builder for parsing data
     */
    public static @NonNull ReadBuilder read() {
        return new ReadBuilder();
    }

    /**
     * Creates a new write builder for serializing data
     */
    public static @NonNull WriteBuilder write(ObjectTree tree) {
        return new WriteBuilder(tree);
    }

    /**
     * Creates a new builder for constructing an ObjectTree programmatically
     */
    public static @NonNull Builder builder() {
        return new Builder();
    }

    /**
     * Converts this tree back to a raw map structure (for serialization)
     */
    @NonNull Map<String, Object> toRawMap() {
        Map<String, Object> raw = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            raw.put(entry.getKey(), denormalizeValue(entry.getValue()));
        }
        return raw;
    }

    /**
     * Gets a value from the tree
     *
     * @param key
     *     the key to retrieve
     *
     * @return the value wrapper
     *
     * @throws NoSuchElementException
     *     if the key does not exist
     * @apiNote This does not support nesting, so you cannot use the input {@code "thing.nestedthing"} to find a
     *     nested value
     */
    public @NonNull Value<?> getValueOrThrow(String key) {
        if (!data.containsKey(key)) {
            throw new NoSuchElementException("Key not found: " + key);
        }
        return new ObjectValue(data.get(key), converters);
    }

    /**
     * Gets a value from the tree safely.
     * <p>
     * What quantifies "safely" is that this doesn't throw a {@link NoSuchElementException} if not found, and instead it
     * returns an {@link EmptyValue}, which essentially is a wrapper of "null". This is generally recommended for when
     * trying to access keys that may not exist. It is always recommended to use {@link Value#asOptional(Class)} after
     * parsing this to avoid potential parse issues, since this can potentially be an empty value.
     *
     * @param key
     *     the key to retrieve
     *
     * @return the value wrapper
     *
     * @see EmptyValue
     */
    public @NonNull Value<?> getValueSafe(String key) {
        return getValueOptional(key).orElse(new EmptyValue<>());
    }

    /**
     * Gets an optional value from this tree. Will return {@link Optional#empty()} if the key does not exist
     *
     * @apiNote This does not support nesting, so you cannot use the input {@code "thing.nestedthing"} to find a
     *     nested value
     */
    public Optional<Value<?>> getValueOptional(String key) {
        return Optional.ofNullable(data.get(key))
            .map(v -> new ObjectValue(v, converters));
    }

    /**
     * Gets a nested tree from this tree
     *
     * @throws ClassCastException
     *     if the value is not a Map
     * @throws NoSuchElementException
     *     if the key does not exist
     * @apiNote This does not support nesting, so you cannot use the input {@code "thing.nestedtree"} to find a
     *     nested tree within a nested tree
     */
    public @NonNull ObjectTree getTree(String key) {
        if (!data.containsKey(key)) {
            throw new NoSuchElementException("Key not found: " + key);
        }
        Object value = data.get(key);
        if (!(value instanceof ObjectTree)) {
            throw new ClassCastException("Value at key '" + key + "' is not a tree structure");
        }
        return (ObjectTree) value;
    }

    /**
     * Gets an optional nested tree from this tree
     *
     * @apiNote This does not support nesting, so you cannot use the input {@code "thing.nestedtree"} to find a
     *     nested tree within a nested tree
     */
    public Optional<ObjectTree> getTreeOptional(String key) {
        return Optional.ofNullable(data.get(key))
            .filter(v -> v instanceof ObjectTree)
            .map(v -> (ObjectTree) v);
    }

    /**
     * Gets an array from this tree
     *
     * @throws ClassCastException
     *     if the value is not a List
     * @throws NoSuchElementException
     *     if the key does not exist
     * @apiNote This does not support nesting, so you cannot use the input {@code "thing.nestedarray"} to find a
     *     nested array
     */
    public @NonNull ObjectArray getArray(String key) {
        if (!data.containsKey(key)) {
            throw new NoSuchElementException("Key not found: " + key);
        }
        Object value = data.get(key);
        if (!(value instanceof List)) {
            throw new ClassCastException("Value at key '" + key + "' is not an array");
        }
        //noinspection unchecked
        return new ObjectArray((List<Object>) value, converters, remappingContext);
    }

    /**
     * Gets an optional array from this tree
     *
     * @apiNote This does not support nesting, so you cannot use the input {@code "thing.nestedarray"} to find a
     *     nested array
     */
    public Optional<ObjectArray> getArrayOptional(String key) {
        //noinspection unchecked
        return Optional.ofNullable(data.get(key))
            .filter(v -> v instanceof List)
            .map(v -> new ObjectArray((List<Object>) v, converters, remappingContext));
    }

    /**
     * Checks if a key exists in this tree
     */
    public boolean containsKey(String key) {
        return data.containsKey(key);
    }

    /**
     * Returns all keys in this tree
     */
    public @NonNull Set<String> keys() {
        return data.keySet();
    }

    /**
     * Returns all values in this tree as ObjectValue wrappers
     */
    public @NonNull @Unmodifiable Collection<? extends Value<?>> values() {
        return data.values().stream()
            .map(v -> new ObjectValue(v, converters))
            .toList();
    }

    /**
     * Returns the number of entries in this tree
     */
    public int size() {
        return data.size();
    }

    /**
     * Checks if this tree is empty
     */
    public boolean isEmpty() {
        return data.isEmpty();
    }

    /**
     * Attempts to deserialize this tree into a custom object using a registered deserializer and returns as an
     * {@link Optional}
     */
    public <T> Optional<T> asOptional(Class<T> type) {
        try {
            return Optional.of(as(type));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Deserializes this tree into a custom object using a registered deserializer
     */
    public <T> T as(Class<T> type) throws Exception {
        ObjectDeserializer<T> deserializer = converters.getDeserializer(type);
        return deserializer.deserialize(this);
    }

    public TypeConverterRegistry getConverters() {
        return converters;
    }

    public RemappingContext getInterpolationContext() {
        return remappingContext;
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ObjectTree other)) return false;
        return data.equals(other.data);
    }

    @Override
    public String toString() {
        return "ObjectTree" + data;
    }

    /**
     * Builder for reading and parsing data
     */
    public static final class ReadBuilder {

        private final TypeConverterRegistry converters = new TypeConverterRegistry();
        private final Map<String, Set<String>> aliases = new HashMap<>();
        private final Map<String, String> remapVars = new HashMap<>();
        private final Map<String, String> overrideKeys = new HashMap<>();
        private Format format;
        private @Nullable FormatParser customParser;

        private ReadBuilder() {
        }

        /**
         * Sets the format to parse.
         */
        public ReadBuilder format(Format format) {
            this.format = format;
            return this;
        }

        /**
         * Registers a custom format parser
         */
        public ReadBuilder customFormat(FormatParser parser) {
            this.customParser = parser;
            return this;
        }

        /**
         * Registers a type converter. Should be used when using {@link Value#as(Class)} to parse your object
         */
        public <T> ReadBuilder registerConverter(Class<T> type, TypeConverter<T> converter) {
            converters.register(type, converter);
            return this;
        }

        /**
         * Registers a custom object deserializer. Should be used when parsing a full {@link ObjectTree} using
         * {@link ObjectTree#as(Class)}
         */
        public <T> ReadBuilder registerDeserializer(Class<T> type, ObjectDeserializer<T> deserializer) {
            converters.registerDeserializer(type, deserializer);
            return this;
        }

        /**
         * Adds an alias that maps to a primary key. When parsing, if any alias is found, it will be remapped to the
         * primary key.
         */
        public ReadBuilder alias(String primaryKey, String... aliasKeys) {
            aliases.computeIfAbsent(primaryKey, k -> new HashSet<>())
                .addAll(Arrays.asList(aliasKeys));
            return this;
        }

        /**
         * Adds variables for remapping (e.g., ${var.name})
         */
        public ReadBuilder withRemapKey(String name, String value) {
            remapVars.put(name, value);
            return this;
        }

        /**
         * Adds multiple variables for remapping
         */
        public ReadBuilder withRemapKeys(Map<String, String> variables) {
            remapVars.putAll(variables);
            return this;
        }

        /**
         * Parses from an InputStream
         */
        public @NonNull ObjectTree from(InputStream input) throws ParseException {
            return parse(input);
        }

        /**
         * Parses from a Reader
         */
        public @NonNull ObjectTree from(Reader reader) throws ParseException {
            return parse(reader);
        }

        /**
         * Parses from a String
         */
        public @NonNull ObjectTree fromString(String content) throws ParseException {
            return parse(content);
        }

        /**
         * Registers an override for a key-value pair using a JVM system property. If the system property exists, its
         * value will replace the parsed value.
         *
         * @param property
         *     the config key to override
         * @param systemProp
         *     the JVM system property name
         */
        public @NonNull ReadBuilder registerOverrideKey(String property, String systemProp) {
            overrideKeys.put(property, systemProp);
            return this;
        }

        private @NonNull ObjectTree parse(Object source) throws ParseException {
            if (format == null && customParser == null) {
                throw new IllegalStateException("Format or custom parser must be specified");
            }

            FormatParser parser = customParser != null ? customParser : format.getParser();

            List<ParseError> errors = new ArrayList<>();
            Map<String, Object> rawData;

            try {
                rawData = switch (source) {
                    case InputStream inputStream -> parser.parse(inputStream, errors);
                    case Reader reader -> parser.parse(reader, errors);
                    case String s -> parser.parse(s, errors);
                    case null, default -> throw new IllegalArgumentException("Unsupported source type");
                };
            } catch (Exception e) {
                errors.add(new ParseError("Fatal parsing error", e));
                throw new ParseException(errors);
            }

            // apply aliases and remapping
            rawData = applyAliases(rawData);
            RemappingContext remappingContext = new RemappingContext(
                remapVars
            );

            rawData = remap(rawData, remappingContext, errors);

            if (!errors.isEmpty()) {
                throw new ParseException(errors);
            }

            return new ObjectTree(rawData, converters, remappingContext);
        }

        private @NonNull Map<String, Object> applyAliases(@NonNull Map<String, Object> data) {
            Map<String, Object> result = new LinkedHashMap<>();

            for (Map.Entry<String, Object> entry : data.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();

                String primaryKey = key;
                for (Map.Entry<String, Set<String>> aliasEntry : aliases.entrySet()) {
                    if (aliasEntry.getValue().contains(key)) {
                        primaryKey = aliasEntry.getKey();
                        break;
                    }
                }

                // Recursively apply to nested structures
                if (value instanceof Map) {
                    //noinspection unchecked
                    value = applyAliases((Map<String, Object>) value);
                }
                else if (value instanceof List) {
                    //noinspection ReassignedVariable,unchecked
                    value = applyAliasesToList((List<Object>) value);
                }

                result.put(primaryKey, value);
            }

            return result;
        }

        private @NonNull List<Object> applyAliasesToList(@NonNull List<Object> list) {
            List<Object> result = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Map) {
                    //noinspection unchecked
                    result.add(applyAliases((Map<String, Object>) item));
                }
                else if (item instanceof List) {
                    //noinspection unchecked
                    result.add(applyAliasesToList((List<Object>) item));
                }
                else {
                    result.add(item);
                }
            }
            return result;
        }

        private @NonNull Map<String, Object> remap(@NonNull Map<String, Object> data, RemappingContext context, List<ParseError> errors) {
            Map<String, Object> result = new LinkedHashMap<>();

            for (Map.Entry<String, Object> entry : data.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();

                String systemProp = overrideKeys.get(key);
                if (systemProp != null) {
                    String overrideValue = System.getProperty(systemProp);
                    if (overrideValue != null) {
                        value = overrideValue;
                    }
                }

                if (value instanceof String) {
                    try {
                        value = context.interpolate((String) value);
                    } catch (Exception e) {
                        errors.add(new ParseError("Interpolation error for key '" + key + "'", e));
                    }
                }
                else if (value instanceof Map) {
                    //noinspection unchecked
                    value = remap((Map<String, Object>) value, context, errors);
                }
                else if (value instanceof List) {
                    //noinspection ReassignedVariable,unchecked
                    value = interpolateList((List<Object>) value, context, errors);
                }

                result.put(key, value);
            }

            return result;
        }

        private @NonNull List<Object> interpolateList(@NonNull List<Object> list, RemappingContext context, List<ParseError> errors) {
            List<Object> result = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof String) {
                    try {
                        result.add(context.interpolate((String) item));
                    } catch (Exception e) {
                        errors.add(new ParseError("Interpolation error in list", e));
                        result.add(item);
                    }
                }
                else if (item instanceof Map) {
                    //noinspection unchecked
                    result.add(remap((Map<String, Object>) item, context, errors));
                }
                else if (item instanceof List) {
                    //noinspection unchecked
                    result.add(interpolateList((List<Object>) item, context, errors));
                }
                else {
                    result.add(item);
                }
            }
            return result;
        }
    }

    /**
     * Builder for writing/serializing data
     */
    public static final class WriteBuilder {

        private final ObjectTree tree;
        private final Map<String, String> keyMappings = new HashMap<>();
        private Format format;
        private @Nullable FormatWriter customWriter;

        private WriteBuilder(ObjectTree tree) {
            this.tree = tree;
        }

        /**
         * Sets the output format
         */
        public WriteBuilder format(Format format) {
            this.format = format;
            return this;
        }

        /**
         * Registers a custom format writer
         */
        public WriteBuilder customFormat(FormatWriter writer) {
            this.customWriter = writer;
            return this;
        }

        /**
         * Maps a key to a different name during serialization
         */
        public WriteBuilder mapKey(String fromKey, String toKey) {
            keyMappings.put(fromKey, toKey);
            return this;
        }

        /**
         * Writes to an OutputStream
         */
        public void to(OutputStream output) throws WriteException {
            write(output);
        }

        /**
         * Writes to a Writer
         */
        public void to(Writer writer) throws WriteException {
            write(writer);
        }

        /**
         * Writes to a String
         */
        public String asString() throws WriteException {
            return writeToString();
        }

        private void write(Object target) throws WriteException {
            if (format == null && customWriter == null) {
                throw new IllegalStateException("Format or custom writer must be specified");
            }

            FormatWriter writer = customWriter != null ? customWriter : format.getWriter();
            Map<String, Object> data = applyKeyMappings(tree.data);

            try {
                if (target instanceof OutputStream) {
                    writer.write(data, (OutputStream) target);
                }
                else if (target instanceof Writer) {
                    writer.write(data, (Writer) target);
                }
                else {
                    throw new IllegalArgumentException("Unsupported target type");
                }
            } catch (Exception e) {
                throw new WriteException("Error writing data", e);
            }
        }

        private String writeToString() throws WriteException {
            if (format == null && customWriter == null) {
                throw new IllegalStateException("Format or custom writer must be specified");
            }

            FormatWriter writer = customWriter != null ? customWriter : format.getWriter();
            Map<String, Object> data = applyKeyMappings(tree.data);

            try {
                return writer.writeToString(data);
            } catch (Exception e) {
                throw new WriteException("Error writing data", e);
            }
        }

        private @NonNull Map<String, Object> applyKeyMappings(@NonNull Map<String, Object> data) {
            Map<String, Object> result = new LinkedHashMap<>();

            for (Map.Entry<String, Object> entry : data.entrySet()) {
                String key = keyMappings.getOrDefault(entry.getKey(), entry.getKey());
                Object value = entry.getValue();

                if (value instanceof Map) {
                    //noinspection unchecked
                    value = applyKeyMappings((Map<String, Object>) value);
                }
                else if (value instanceof List) {
                    //noinspection unchecked
                    value = applyKeyMappingsToList((List<Object>) value);
                }

                result.put(key, value);
            }

            return result;
        }

        private @NonNull List<Object> applyKeyMappingsToList(@NonNull List<Object> list) {
            List<Object> result = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Map) {
                    //noinspection unchecked
                    result.add(applyKeyMappings((Map<String, Object>) item));
                }
                else if (item instanceof List) {
                    //noinspection unchecked
                    result.add(applyKeyMappingsToList((List<Object>) item));
                }
                else {
                    result.add(item);
                }
            }
            return result;
        }
    }

    /**
     * Builder for constructing an {@link ObjectTree} programmatically
     */
    public static final class Builder {

        private final Map<String, Object> data = new LinkedHashMap<>();
        private final TypeConverterRegistry converters = new TypeConverterRegistry();
        private final Map<String, String> interpolationVariables = new HashMap<>();

        private Builder() {
        }

        /**
         * Puts a value into the tree
         */
        public Builder put(String key, @Nullable Object value) {
            data.put(key, value);
            return this;
        }

        /**
         * Puts all entries from a map
         */
        public Builder putAll(Map<String, Object> map) {
            data.putAll(map);
            return this;
        }

        /**
         * Registers a type converter
         */
        public <T> Builder registerConverter(Class<T> type, TypeConverter<T> converter) {
            converters.register(type, converter);
            return this;
        }

        /**
         * Registers a custom object deserializer
         */
        public <T> Builder registerDeserializer(Class<T> type, ObjectDeserializer<T> deserializer) {
            converters.registerDeserializer(type, deserializer);
            return this;
        }

        /**
         * Adds a variable for interpolation
         */
        public Builder withVariable(String name, String value) {
            interpolationVariables.put(name, value);
            return this;
        }

        /**
         * Builds the ObjectTree
         */
        public @NonNull ObjectTree build() {
            RemappingContext context = new RemappingContext(interpolationVariables);
            return new ObjectTree(data, converters, context);
        }
    }
}
