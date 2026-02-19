package io.canvasmc.horizon.plugin.data;

/**
 * The parsed entry point instance defined in the Horizon plugin JSON
 *
 * @param key
 *     the key of the entrypoint used for calling, like {@code server_main}
 * @param clazz
 *     the class to instantiate and invoke
 * @param order
 *     the priority, used to sort entries for invoking
 *
 * @author dueris
 */
public record EntrypointObject(String key, String clazz, int order) {}