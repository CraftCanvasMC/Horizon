package io.canvasmc.horizon.service;

import org.spongepowered.asm.service.IMixinServiceBootstrap;

public class MixinBootstrapService implements IMixinServiceBootstrap {
    @Override
    public String getName() {
        return "Horizon";
    }

    @Override
    public String getServiceClassName() {
        return "io.canvasmc.horizon.ember.EmberMixinService";
    }

    @Override
    public void bootstrap() {
    }
}
