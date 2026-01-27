package io.canvasmc.horizon.util;

import io.canvasmc.horizon.util.tree.ObjectDeserializer;
import io.canvasmc.horizon.util.tree.ObjectTree;
import io.canvasmc.horizon.util.tree.ParseError;
import io.canvasmc.horizon.util.tree.ParseException;
import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * Represents the paperclip launcher version meta. The raw data looks something like this:
 * <pre>{@code
 * {
 *     "id": "1.21.11",
 *     "name": "1.21.11",
 *     "world_version": 4671,
 *     "series_id": "main",
 *     "protocol_version": 774,
 *     "pack_version": {
 *         "resource_major": 75,
 *         "resource_minor": 0,
 *         "data_major": 94,
 *         "data_minor": 1
 *     },
 *     "build_time": "2025-12-09T12:20:42+00:00",
 *     "java_component": "java-runtime-delta",
 *     "java_version": 21,
 *     "stable": true,
 *     "use_editor": false
 * }
 * }</pre>
 * <p>
 * This was primarily documented based on <a
 * href="https://minecraft.wiki/w/Version.json#JSON_format">minecraft.wiki</a>, and the wiki was also updated from these
 * docs, it is recommended that you take a look at the wiki link along with these docs for a full in-depth look on what
 * this is and represents.
 *
 * @param id
 *     The version's unique identifier. May sometimes display the build has as well
 * @param name
 *     The version's user-friendly name, often identical to {@code id}
 * @param world_version
 *     The <a href="https://minecraft.wiki/w/Data_version">data version</a> of this version
 * @param series_id
 *     Identifies which branch this version is from, the more common one being "main", and other values are used when a
 *     version isn't from the main branch. {@code deep_dark_preview} was used for the deep dark experimental snapshot
 *     for example, and {@code april<YYYY>} is used for April Fools snapshots
 * @param protocol_version
 *     The protocol version of this version
 * @param pack_version
 *     The resource and data pack formats of this version.
 * @param build_time
 *     The release time of this version in {@code ISO 8601} format
 * @param java_component
 *     Unused in the server. Identifies which Java runtime package the game expects to use
 * @param java_version
 *     The Java version used to compile the server
 * @param stable
 *     If the release is stable or a development version
 * @param use_editor
 *     Unknown use
 *
 * @author dueris
 * @see PackVersion pack_version docs
 */
public record PaperclipVersion(
    String id,
    String name,
    int world_version,
    String series_id,
    int protocol_version,
    PackVersion pack_version,
    String build_time,
    String java_component,
    int java_version,
    boolean stable,
    boolean use_editor
) {

    /**
     * The resource and data pack formats of this version
     *
     * @param resource_major
     *     The max resource pack format number supported
     * @param resource_minor
     *     The minimum resource pack format number supported
     * @param data_major
     *     The max data pack format number supported
     * @param data_minor
     *     The minimum data pack format number supported
     *
     * @apiNote The {@code major} and {@code minor} fields were added in {@code 1.21.9} snapshot {@code 25w31a}. For
     *     versions before this, Horizon automatically defaults the {@link PackVersion#data_major()} and
     *     {@link PackVersion#resource_major()} to the {@code resource} and {@code data} values, with the {@code minor}
     *     values being defaulted to 0
     */
    public record PackVersion(
        int resource_major,
        int resource_minor,
        int data_major,
        int data_minor
    ) {}

    public static final class PaperclipVersionDeserializer implements ObjectDeserializer<PaperclipVersion> {

        @Override
        public @NonNull PaperclipVersion deserialize(@NonNull ObjectTree tree) throws ParseException {
            try {
                ObjectTree pack_version = tree.getTree("pack_version");
                PackVersion pack = new PackVersion(
                    // we use an alias mapping to remap 'resource' -> 'resource_major', and 'data' -> 'data_major'
                    // that way older Minecraft versions won't die when trying to parse this version data
                    pack_version.getValueOrThrow("resource_major").asInt(),
                    pack_version.getValueSafe("resource_minor").asIntOptional().orElse(0),
                    pack_version.getValueOrThrow("data_major").asInt(),
                    pack_version.getValueSafe("data_minor").asIntOptional().orElse(0)
                );

                return new PaperclipVersion(
                    tree.getValueOrThrow("id").asString(),
                    tree.getValueOrThrow("name").asString(),
                    tree.getValueOrThrow("world_version").asInt(),
                    tree.getValueOrThrow("series_id").asString(),
                    tree.getValueOrThrow("protocol_version").asInt(),
                    pack,
                    tree.getValueOrThrow("build_time").asString(),
                    tree.getValueOrThrow("java_component").asString(),
                    tree.getValueOrThrow("java_version").asInt(),
                    tree.getValueOrThrow("stable").asBoolean(),
                    tree.getValueOrThrow("use_editor").asBoolean()
                );
            } catch (Exception exe) {
                String out = tree.toString();
                throw new ParseException(List.of(new ParseError("Couldn't read paperclip version: " + out), new ParseError(exe.getMessage())));
            }
        }
    }
}
