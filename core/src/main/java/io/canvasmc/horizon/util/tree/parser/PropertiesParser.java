package io.canvasmc.horizon.util.tree.parser;

import io.canvasmc.horizon.util.tree.FormatParser;
import io.canvasmc.horizon.util.tree.ParseError;
import org.jspecify.annotations.NonNull;

import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public final class PropertiesParser implements FormatParser {

    @Override
    public @NonNull Map<String, Object> parse(InputStream input, List<ParseError> errors) throws Exception {
        Properties props = new Properties();
        try {
            props.load(input);
            return convertProperties(props, errors);
        } catch (Exception e) {
            errors.add(new ParseError("Properties parsing failed", e));
            throw e;
        }
    }

    @Override
    public @NonNull Map<String, Object> parse(Reader reader, List<ParseError> errors) throws Exception {
        Properties props = new Properties();
        try {
            props.load(reader);
            return convertProperties(props, errors);
        } catch (Exception e) {
            errors.add(new ParseError("Properties parsing failed", e));
            throw e;
        }
    }

    @Override
    public @NonNull Map<String, Object> parse(String content, List<ParseError> errors) throws Exception {
        Properties props = new Properties();
        try {
            props.load(new StringReader(content));
            return convertProperties(props, errors);
        } catch (Exception e) {
            errors.add(new ParseError("Properties parsing failed", e));
            throw e;
        }
    }

    private @NonNull Map<String, Object> convertProperties(@NonNull Properties props, List<ParseError> errors) {
        Map<String, Object> result = new LinkedHashMap<>();

        for (String key : props.stringPropertyNames()) {
            String value = props.getProperty(key);

            if (key.contains(".")) {
                setNestedValue(result, key, value, errors);
            } else {
                result.put(key, parseValue(value));
            }
        }

        return result;
    }

    private void setNestedValue(Map<String, Object> root, @NonNull String path, String value, List<ParseError> errors) {
        String[] parts = path.split("\\.");
        Map<String, Object> current = root;

        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];

            if (!current.containsKey(part)) {
                current.put(part, new LinkedHashMap<String, Object>());
            }

            Object next = current.get(part);
            if (!(next instanceof Map)) {
                errors.add(new ParseError(
                    "Cannot create nested structure at '" + part + "' - value already exists and is not a map"
                ));
                return;
            }

            //noinspection unchecked
            current = (Map<String, Object>) next;
        }

        current.put(parts[parts.length - 1], parseValue(value));
    }

    private Object parseValue(String value) {
        if (value == null) {
            return null;
        }

        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
            return Boolean.parseBoolean(value);
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
        }

        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
        }

        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
        }

        return value;
    }
}
