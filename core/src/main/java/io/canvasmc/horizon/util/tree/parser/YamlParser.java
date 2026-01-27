package io.canvasmc.horizon.util.tree.parser;

import io.canvasmc.horizon.util.tree.FormatParser;
import io.canvasmc.horizon.util.tree.ParseError;
import org.jspecify.annotations.NonNull;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class YamlParser implements FormatParser {
    private final Yaml yaml = new Yaml();

    @Override
    public @NonNull Map<String, Object> parse(InputStream input, List<ParseError> errors) throws Exception {
        try {
            Object data = yaml.load(input);
            return convertToMap(data, errors);
        } catch (Exception e) {
            errors.add(new ParseError("YAML parsing failed", e));
            throw e;
        }
    }

    @Override
    public @NonNull Map<String, Object> parse(Reader reader, List<ParseError> errors) throws Exception {
        try {
            Object data = yaml.load(reader);
            return convertToMap(data, errors);
        } catch (Exception e) {
            errors.add(new ParseError("YAML parsing failed", e));
            throw e;
        }
    }

    @Override
    public @NonNull Map<String, Object> parse(String content, List<ParseError> errors) throws Exception {
        try {
            Object data = yaml.load(content);
            return convertToMap(data, errors);
        } catch (Exception e) {
            errors.add(new ParseError("YAML parsing failed", e));
            throw e;
        }
    }

    private @NonNull Map<String, Object> convertToMap(Object data, List<ParseError> errors) {
        if (data == null) {
            return new LinkedHashMap<>();
        }

        if (!(data instanceof Map)) {
            errors.add(new ParseError("YAML root must be a map/object, got: " + data.getClass().getSimpleName()));
            return new LinkedHashMap<>();
        }

        return normalizeMap((Map<?, ?>) data);
    }

    private @NonNull Map<String, Object> normalizeMap(@NonNull Map<?, ?> map) {
        Map<String, Object> result = new LinkedHashMap<>();

        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = String.valueOf(entry.getKey());
            Object value = entry.getValue();

            if (value instanceof Map) {
                value = normalizeMap((Map<?, ?>) value);
            }
            else if (value instanceof List) {
                value = normalizeList((List<?>) value);
            }

            result.put(key, value);
        }

        return result;
    }

    private @NonNull List<Object> normalizeList(@NonNull List<?> list) {
        List<Object> result = new ArrayList<>();

        for (Object item : list) {
            if (item instanceof Map) {
                result.add(normalizeMap((Map<?, ?>) item));
            }
            else if (item instanceof List) {
                result.add(normalizeList((List<?>) item));
            }
            else {
                result.add(item);
            }
        }

        return result;
    }
}

