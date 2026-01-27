package io.canvasmc.horizon.util.tree.parser;

import com.fasterxml.jackson.dataformat.toml.TomlMapper;
import io.canvasmc.horizon.util.tree.FormatParser;
import io.canvasmc.horizon.util.tree.ParseError;
import org.jspecify.annotations.NonNull;

import java.io.InputStream;
import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TomlParser implements FormatParser {

    private final TomlMapper mapper = new TomlMapper();

    @Override
    public @NonNull Map<String, Object> parse(InputStream input, List<ParseError> errors) throws Exception {
        try {
            Object data = mapper.readValue(input, Object.class);
            return convertToMap(data, errors);
        } catch (Exception e) {
            errors.add(new ParseError("TOML parsing failed", e));
            throw e;
        }
    }

    @Override
    public @NonNull Map<String, Object> parse(Reader reader, List<ParseError> errors) throws Exception {
        try {
            Object data = mapper.readValue(reader, Object.class);
            return convertToMap(data, errors);
        } catch (Exception e) {
            errors.add(new ParseError("TOML parsing failed", e));
            throw e;
        }
    }

    @Override
    public @NonNull Map<String, Object> parse(String content, List<ParseError> errors) throws Exception {
        try {
            Object data = mapper.readValue(content, Object.class);
            return convertToMap(data, errors);
        } catch (Exception e) {
            errors.add(new ParseError("TOML parsing failed", e));
            throw e;
        }
    }

    private @NonNull Map<String, Object> convertToMap(Object data, List<ParseError> errors) {
        if (data == null) {
            return new LinkedHashMap<>();
        }

        if (!(data instanceof Map)) {
            errors.add(new ParseError("TOML root must be a table, got: " + data.getClass().getSimpleName()));
            return new LinkedHashMap<>();
        }

        //noinspection unchecked
        return (Map<String, Object>) data;
    }
}
