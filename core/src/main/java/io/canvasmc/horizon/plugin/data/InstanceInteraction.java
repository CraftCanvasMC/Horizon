package io.canvasmc.horizon.plugin.data;

public record InstanceInteraction(InteractionParser.Data type, Resolver resolver) {
    public enum Type {
        SPIGOT_PLUGIN,
        PAPER_PLUGIN,
        MINECRAFT,
        JAVA,
        ASM
    }

    public enum Resolver {
        REQUIRED,
        RECOMMENDED,
        BREAKS
    }
}