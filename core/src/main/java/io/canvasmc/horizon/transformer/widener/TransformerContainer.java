package io.canvasmc.horizon.transformer.widener;

import io.canvasmc.horizon.plugin.types.HorizonPlugin;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.tinylog.Logger;
import org.tinylog.TaggedLogger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Set;
import java.util.function.Supplier;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TransformerContainer {

    public static final Supplier<Throwable> COULDNT_LOCATE_FIELD = () -> new IllegalArgumentException("Couldn't locate target field when attempting to transform");
    public static final Supplier<Throwable> COULDNT_LOCATE_METHOD = () -> new IllegalArgumentException("Couldn't locate target method when attempting to transform");
    /**
     * Patterns for identifying the definition of a transformer
     * <p>
     * classes: {@code <access modifier> <fully qualified class name>}
     * <br>
     * fields: {@code <access modifier> <fully qualified class name> <field name>}
     * <br>
     * methods: {@code <access modifier> <fully qualified class name> <method name>(<parameter types>)<return type>}
     */
    private static final Pattern CLASS_REGEX =
        Pattern.compile("^\\s*(public|protected|private|default)([+-]f)?\\s+([A-Za-z_][A-Za-z0-9_]*(?:[.$][A-Za-z_][A-Za-z0-9_]*)*)\\s*$");
    private static final Pattern FIELD_REGEX =
        Pattern.compile("^\\s*(public|protected|private|default)([+-]f)?\\s+([A-Za-z_][A-Za-z0-9_]*(?:[.$][A-Za-z_][A-Za-z0-9_]*)*)\\s+([A-Za-z_][A-Za-z0-9_]*)\\s*$");
    private static final Pattern METHOD_REGEX =
        Pattern.compile("^\\s*(public|protected|private|default)([+-]f)?\\s+([A-Za-z_][A-Za-z0-9_]*(?:[.$][A-Za-z_][A-Za-z0-9_]*)*)\\s+([A-Za-z_][A-Za-z0-9_]*)\\s*\\(([^)]*)\\)\\s*([A-Za-z_][A-Za-z0-9_]*(?:\\.[A-Za-z_][A-Za-z0-9_]*)*)\\s*$");

    // just for pre-validation
    private static final Pattern MODIFIER_PREFIX =
        Pattern.compile("^\\s*(public|protected|private|default)([+-]f)?\\b");

    private static final TaggedLogger LOGGER = Logger.tag("widener");

    private static final int VIS_PUBLIC = 3;
    private static final int VIS_PROTECTED = 2;
    private static final int VIS_PRIVATE = 1;

    private final Object2ObjectOpenHashMap<String, ObjectOpenHashSet<Definition>> definitionRegistry =
        new Object2ObjectOpenHashMap<>(8, 0.75F);
    private volatile boolean locked = false;

    private static int visibilityRank(TransformOperation.@NonNull Access access) {
        return switch (access) {
            case PUBLIC -> VIS_PUBLIC;
            case PROTECTED -> VIS_PROTECTED;
            // while it is probably not ideal to do this, it is best when we are resolving conflicts
            // since when conflicts arise, at the time of resolving these we don't know the access
            // of the field/method/class anyway, so it is best just to make it public so it works
            // for everyone guaranteed
            case DEFAULT -> VIS_PUBLIC;
            case PRIVATE -> VIS_PRIVATE;
        };
    }

    private static @NonNull TransformOperation tryParseOperation(@NonNull String tOperation) {
        boolean removesOrAddsFinal = tOperation.endsWith("-f") || tOperation.endsWith("+f");
        int length = tOperation.length();
        String op = removesOrAddsFinal ? tOperation.substring(0, length - 2) : tOperation;
        String f = removesOrAddsFinal ? tOperation.substring(length - 2, length) : "none";
        return TransformOperation.builder()
            .access(TransformOperation.Access.valueOf(op.toUpperCase()))
            .finality(f.equals("none") ? TransformOperation.Finality.NONE : f.equals("-f") ? TransformOperation.Finality.REMOVE : TransformOperation.Finality.ADD)
            .build();
    }

    public void lock() {
        if (locked) {
            return;
        }

        // for each target registered
        for (var mapEntry : definitionRegistry.object2ObjectEntrySet()) {
            ObjectOpenHashSet<Definition> defs = mapEntry.getValue();
            if (defs == null || defs.isEmpty()) {
                continue;
            }

            // this is just a map from key -> the best logical definition
            Object2ObjectOpenHashMap<String, Definition> best = new Object2ObjectOpenHashMap<>();

            for (Definition def : defs) {
                final Definition.Data data = def.data();

                // - ClassData: unique per class target only
                // - FieldData: fieldName
                // - MethodData: full descriptor
                final String key;
                switch (data) {
                    case Definition.ClassData ignored -> key = "CLASS";
                    case Definition.FieldData f -> key = f.fieldName();
                    case Definition.MethodData m -> key = m.methodDescriptor();
                    default -> {
                        continue;
                    }
                }

                Definition existing = best.get(key);
                if (existing == null) {
                    best.put(key, def);
                } else {
                    // resolve conflict - choose the highest visibility
                    TransformOperation.Access oldA = existing.operation().access();
                    TransformOperation.Access newA = def.operation().access();

                    if (visibilityRank(newA) > visibilityRank(oldA)) {
                        best.put(key, def);
                    }
                }
            }

            // rebuild set
            ObjectOpenHashSet<Definition> cleaned = new ObjectOpenHashSet<>(best.size());
            for (Definition d : best.values()) {
                TransformOperation.Access acc = d.operation().access();

                TransformOperation op = TransformOperation.builder()
                    .access(acc)
                    .finality(TransformOperation.Finality.REMOVE) // cannot guarantee anything, best to remove finality
                    .build();

                cleaned.add(new Definition(op, d.data(), d.nodeTarget()));
            }

            mapEntry.setValue(cleaned);
        }

        // trim and remove empty registries
        definitionRegistry.values().removeIf(v -> v == null || v.isEmpty());
        definitionRegistry.trim();
        definitionRegistry.forEach((k, v) -> v.trim());

        locked = true;
    }

    public void register(@NonNull HorizonPlugin plugin) {
        if (locked) {
            throw new IllegalStateException("TransformerContainer is locked and cannot be modified.");
        }
        JarFile source = plugin.file().jarFile();

        for (String entry : plugin.pluginMetadata().accessWideners()) {
            var jarEntry = source.getEntry(entry);
            if (jarEntry == null) continue;

            String line;
            int idx = 0;

            try (InputStream in = source.getInputStream(jarEntry)) {
                if (in == null) continue;
                BufferedReader r = new BufferedReader(new InputStreamReader(in));

                while ((line = r.readLine()) != null) {
                    idx++;

                    // trim the comments
                    int pos = line.indexOf('#');
                    String trimmed = (pos == -1 ? line : line.substring(0, pos)).trim();
                    if (trimmed.isBlank()) {
                        continue;
                    }

                    // classes: <access modifier> <fully qualified class name>
                    // fields: <access modifier> <fully qualified class name> <field name>
                    // methods: <access modifier> <fully qualified class name> <method name>(<parameter types>)<return type>
                    LOGGER.debug("Testing '{}' for transformer definition at line ({})", trimmed, idx);
                    Definition compiled = tryCompile(trimmed);
                    if (compiled != null) {
                        addDefinition(compiled.nodeTarget(), compiled);
                    } else LOGGER.warn("Couldn't compile target definition on line ({}), \"{}\"", idx, line);
                }
            } catch (IOException ignored) {
            } catch (CompileError compileError) {
                LOGGER.error("Failed to apply access transformer {}:{} at line ({}) due to {}",
                    plugin.pluginMetadata().name(), jarEntry.getName(), idx, compileError.getMessage());
            }
        }
    }

    public void addDefinition(String className, Definition def) {
        if (locked) {
            throw new IllegalStateException("TransformerContainer is locked and cannot be modified.");
        }
        LOGGER.debug("Compiled and adding definition {} to target clazz '{}'", def, className);
        definitionRegistry
            .computeIfAbsent(className, k -> new ObjectOpenHashSet<>())
            .add(def);
    }

    public boolean shouldTransform(@NonNull ClassNode node) {
        // node.name returns Class.getName(), where '.' are replaced by '/' as defined in ASM documentation
        return this.definitionRegistry.containsKey(node.name);
    }

    public void transformNode(@NonNull ClassNode toTransform) throws Throwable {
        LOGGER.debug("Access transforming node {}", toTransform.name);
        Set<Definition> modifiers = definitionRegistry.get(toTransform.name);
        if (modifiers == null) {
            throw new IllegalStateException("Attempted to transform unregistered class node");
        }

        for (Definition transformDef : modifiers) {
            switch (transformDef.data()) {
                case Definition.ClassData ignored -> {
                    // is class transformer, try and modify class access
                    LOGGER.debug("Applied class transformation to {}", toTransform.name);
                    toTransform.access = transformDef.operation().apply(toTransform.access);
                }
                case Definition.FieldData fdata -> {
                    String target = fdata.fieldName();
                    FieldNode targetNode = toTransform.fields.stream()
                        .filter(f -> f.name.equals(target))
                        .findFirst().orElseThrow(COULDNT_LOCATE_FIELD);

                    LOGGER.debug("Applied field transformation to {}:{}", toTransform.name, targetNode.name);
                    targetNode.access = transformDef.operation().apply(targetNode.access);
                }
                case Definition.MethodData mdata -> {
                    // the issue with this is the full method descriptor contains
                    // the name + desc in the node, so we need to ensure that that
                    // still works and such
                    String methodDescriptor = mdata.methodDescriptor();
                    int idx = methodDescriptor.indexOf('(');
                    String methodName = methodDescriptor.substring(0, idx);
                    String methodDesc = methodDescriptor.substring(idx);

                    MethodNode targetNode = toTransform.methods.stream()
                        .filter(m -> methodName.equals(m.name) && methodDesc.equals(m.desc))
                        .findFirst().orElseThrow(COULDNT_LOCATE_METHOD);

                    LOGGER.debug("Applied method transformation to {}:{}", toTransform.name, targetNode.name);
                    targetNode.access = transformDef.operation().apply(targetNode.access);
                }
                default -> {
                }
            }
        }
    }

    // Note: we already know the string isn't blank
    private @Nullable Definition tryCompile(@NonNull String trimmed) throws CompileError {
        if (!MODIFIER_PREFIX.matcher(trimmed).find()) {
            throw new CompileError("definition has invalid modifier '" + trimmed + "'");
        }

        Matcher m;

        m = CLASS_REGEX.matcher(trimmed);
        if (m.matches()) {
            String op = m.group(1);
            String f = m.group(2); // may be null
            String clazzTarget = m.group(3);

            TransformOperation operation = tryParseOperation(op + (f == null ? "" : f));

            return new Definition(
                operation,
                new Definition.ClassData(clazzTarget),
                clazzTarget.replace(".", "/")
            );
        }

        m = FIELD_REGEX.matcher(trimmed);
        if (m.matches()) {
            String op = m.group(1);
            String f = m.group(2);
            String clazzTarget = m.group(3);
            String fieldName = m.group(4);

            TransformOperation operation = tryParseOperation(op + (f == null ? "" : f));

            return new Definition(
                operation,
                new Definition.FieldData(fieldName),
                clazzTarget.replace(".", "/")
            );
        }

        m = METHOD_REGEX.matcher(trimmed);
        if (m.matches()) {
            String op = m.group(1);
            String f = m.group(2);
            String clazzTarget = m.group(3);
            String methodName = m.group(4);
            String params = m.group(5);
            String returnType = m.group(6);

            TransformOperation operation = tryParseOperation(op + (f == null ? "" : f));

            return new Definition(
                operation,
                new Definition.MethodData(methodName + "(" + params + ")" + returnType),
                clazzTarget.replace(".", "/")
            );
        }
        return null;
    }
}
