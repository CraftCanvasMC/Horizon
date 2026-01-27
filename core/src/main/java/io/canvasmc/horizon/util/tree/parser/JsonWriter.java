package io.canvasmc.horizon.util.tree.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.canvasmc.horizon.util.tree.FormatWriter;

import java.io.OutputStream;
import java.io.Writer;
import java.util.Map;

// Note: always pretty-prints
public final class JsonWriter implements FormatWriter {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void write(Map<String, Object> data, OutputStream output) throws Exception {
        mapper.writerWithDefaultPrettyPrinter().writeValue(output, data);
    }

    @Override
    public void write(Map<String, Object> data, Writer writer) throws Exception {
        mapper.writerWithDefaultPrettyPrinter().writeValue(writer, data);
    }

    @Override
    public String writeToString(Map<String, Object> data) throws Exception {
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(data);
    }
}
