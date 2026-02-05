package io.canvasmc.horizon.util.tree.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.canvasmc.horizon.util.tree.FormatParser;
import io.canvasmc.horizon.util.tree.ParseError;
import org.jspecify.annotations.NonNull;

import java.io.InputStream;
import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The format parser for JSON files using the Jackson library
 *
 * @author dueris
 */
public final class JsonParser implements FormatParser {
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public @NonNull Map<String, Object> parse(InputStream input, List<ParseError> errors) throws Exception {
        try {
            Object data = mapper.readValue(input, Object.class);
            return convertToMap(data, errors);
        } catch (Exception e) {
            errors.add(new ParseError("JSON parsing failed at " + getErrorLocation(e), e));
            throw e;
        }
    }

    @Override
    public @NonNull Map<String, Object> parse(Reader reader, List<ParseError> errors) throws Exception {
        try {
            Object data = mapper.readValue(reader, Object.class);
            return convertToMap(data, errors);
        } catch (Exception e) {
            errors.add(new ParseError("JSON parsing failed at " + getErrorLocation(e), e));
            throw e;
        }
    }

    @Override
    public @NonNull Map<String, Object> parse(String content, List<ParseError> errors) throws Exception {
        try {
            Object data = mapper.readValue(content, Object.class);
            return convertToMap(data, errors);
        } catch (Exception e) {
            errors.add(new ParseError("JSON parsing failed at " + getErrorLocation(e), e));
            throw e;
        }
    }

    private @NonNull Map<String, Object> convertToMap(Object data, List<ParseError> errors) {
        if (data == null) {
            return new LinkedHashMap<>();
        }

        if (!(data instanceof Map)) {
            errors.add(new ParseError("JSON root must be an object, got: " + data.getClass().getSimpleName()));
            return new LinkedHashMap<>();
        }

        //noinspection unchecked
        return (Map<String, Object>) data;
    }

    private @NonNull String getErrorLocation(@NonNull Exception thrown) {
        String msg = thrown.getMessage();
        if (msg != null && msg.contains("line:")) {
            int idx = msg.indexOf("line:");
            return msg.substring(idx);
        }
        return "unknown location";
    }
}
