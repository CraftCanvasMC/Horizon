package io.canvasmc.horizon.util.tree;

import java.io.OutputStream;
import java.io.Writer;
import java.util.Map;

public interface FormatWriter {

    void write(Map<String, Object> data, OutputStream output) throws Exception;

    void write(Map<String, Object> data, Writer writer) throws Exception;

    String writeToString(Map<String, Object> data) throws Exception;
}
