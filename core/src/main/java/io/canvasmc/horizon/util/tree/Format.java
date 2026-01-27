package io.canvasmc.horizon.util.tree;

import io.canvasmc.horizon.util.tree.parser.JsonParser;
import io.canvasmc.horizon.util.tree.parser.JsonWriter;
import io.canvasmc.horizon.util.tree.parser.PropertiesParser;
import io.canvasmc.horizon.util.tree.parser.PropertiesWriter;
import io.canvasmc.horizon.util.tree.parser.TomlParser;
import io.canvasmc.horizon.util.tree.parser.TomlWriter;
import io.canvasmc.horizon.util.tree.parser.YamlParser;
import io.canvasmc.horizon.util.tree.parser.YamlWriter;
import org.jspecify.annotations.NonNull;

// add more formats here if we decide to add more in the future
public enum Format {
    YAML,
    JSON,
    TOML,
    PROPERTIES;

    public @NonNull FormatParser getParser() {
        return switch (this) {
            case YAML -> new YamlParser();
            case JSON -> new JsonParser();
            case TOML -> new TomlParser();
            case PROPERTIES -> new PropertiesParser();
        };
    }

    public @NonNull FormatWriter getWriter() {
        return switch (this) {
            case YAML -> new YamlWriter();
            case JSON -> new JsonWriter();
            case TOML -> new TomlWriter();
            case PROPERTIES -> new PropertiesWriter();
        };
    }
}
