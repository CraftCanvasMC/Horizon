package io.canvasmc.horizon.plugin.data;

// TODO - allow specifying priority via "order" argument
public record EntrypointObject(String key, String clazz, int order) {}