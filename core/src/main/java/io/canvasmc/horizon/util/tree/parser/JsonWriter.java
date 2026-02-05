package io.canvasmc.horizon.util.tree.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import io.canvasmc.horizon.util.tree.FormatWriter;

import java.io.OutputStream;
import java.io.Writer;
import java.util.Map;

/**
 * The format writer for JSON files using the Jackson library
 *
 * @author dueris
 * @apiNote The writer always pretty prints the JSON output
 */
public final class JsonWriter implements FormatWriter {
    private final ObjectWriter mapper = new ObjectMapper()
        .writerWithDefaultPrettyPrinter();

    @Override
    public void write(Map<String, Object> data, OutputStream output) throws Exception {
        mapper.writeValue(output, data);
    }

    @Override
    public void write(Map<String, Object> data, Writer writer) throws Exception {
        mapper.writeValue(writer, data);
    }

    @Override
    public String writeToString(Map<String, Object> data) throws Exception {
        return mapper.writeValueAsString(data);
    }
}
