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

/**
 * The file format the {@link io.canvasmc.horizon.util.tree.ObjectTree} should be parsed from. Used when reading file
 * structures or streams, and when writing an {@link io.canvasmc.horizon.util.tree.ObjectTree} to String, disk, etc.
 *
 * @author dueris
 */
public enum Format {
    /**
     * YAML format, provided by {@link io.canvasmc.horizon.util.tree.parser.YamlParser} and
     * {@link io.canvasmc.horizon.util.tree.parser.YamlWriter}
     */
    YAML,
    /**
     * JSON format, provided by {@link io.canvasmc.horizon.util.tree.parser.JsonParser} and
     * {@link io.canvasmc.horizon.util.tree.parser.JsonWriter}
     */
    JSON,
    /**
     * TOML format, provided by {@link io.canvasmc.horizon.util.tree.parser.TomlParser} and
     * {@link io.canvasmc.horizon.util.tree.parser.TomlWriter}
     */
    TOML,
    /**
     * PROPERTIES format, provided by {@link io.canvasmc.horizon.util.tree.parser.PropertiesParser} and
     * {@link io.canvasmc.horizon.util.tree.parser.PropertiesWriter}
     */
    PROPERTIES;

    /**
     * Gets the parser for this format
     *
     * @return a new parser instance
     */
    public @NonNull FormatParser getParser() {
        return switch (this) {
            case YAML -> new YamlParser();
            case JSON -> new JsonParser();
            case TOML -> new TomlParser();
            case PROPERTIES -> new PropertiesParser();
        };
    }

    /**
     * Gets the writer for this format
     *
     * @return a new writer instance
     */
    public @NonNull FormatWriter getWriter() {
        return switch (this) {
            case YAML -> new YamlWriter();
            case JSON -> new JsonWriter();
            case TOML -> new TomlWriter();
            case PROPERTIES -> new PropertiesWriter();
        };
    }
}
