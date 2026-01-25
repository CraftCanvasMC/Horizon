package io.canvasmc.horizon.util.tree.parser;

import io.canvasmc.horizon.util.tree.FormatWriter;
import org.jspecify.annotations.NonNull;

import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public final class PropertiesWriter implements FormatWriter {

    @Override
    public void write(Map<String, Object> data, OutputStream output) throws Exception {
        Properties props = flattenToProperties(data);
        props.store(output, null);
    }

    @Override
    public void write(Map<String, Object> data, Writer writer) throws Exception {
        Properties props = flattenToProperties(data);
        props.store(writer, null);
    }

    @Override
    public String writeToString(Map<String, Object> data) throws Exception {
        Properties props = flattenToProperties(data);
        StringWriter writer = new StringWriter();
        props.store(writer, null);
        return writer.toString();
    }

    private @NonNull Properties flattenToProperties(Map<String, Object> data) {
        Properties props = new Properties();
        flattenMap("", data, props);
        return props;
    }

    private void flattenMap(String prefix, @NonNull Map<String, Object> map, Properties props) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            Object value = entry.getValue();

            if (value instanceof Map) {
                //noinspection unchecked
                flattenMap(key, (Map<String, Object>) value, props);
            }
            else if (value instanceof List<?> list) {
                for (int i = 0; i < list.size(); i++) {
                    Object item = list.get(i);
                    if (item instanceof Map) {
                        //noinspection unchecked
                        flattenMap(key + "[" + i + "]", (Map<String, Object>) item, props);
                    }
                    else {
                        props.setProperty(key + "[" + i + "]", String.valueOf(item));
                    }
                }
            }
            else {
                props.setProperty(key, String.valueOf(value));
            }
        }
    }
}
