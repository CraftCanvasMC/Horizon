package io.canvasmc.horizon.util.tree.parser;

import io.canvasmc.horizon.util.tree.FormatWriter;
import org.yaml.snakeyaml.Yaml;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Map;

public final class YamlWriter implements FormatWriter {

    private final Yaml yaml = new Yaml();

    @Override
    public void write(Map<String, Object> data, OutputStream output) throws Exception {
        try (OutputStreamWriter writer = new OutputStreamWriter(output)) {
            yaml.dump(data, writer);
        }
    }

    @Override
    public void write(Map<String, Object> data, Writer writer) throws Exception {
        yaml.dump(data, writer);
    }

    @Override
    public String writeToString(Map<String, Object> data) throws Exception {
        return yaml.dump(data);
    }
}
