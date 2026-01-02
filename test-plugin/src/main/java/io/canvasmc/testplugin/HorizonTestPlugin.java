package io.canvasmc.testplugin;

import org.bukkit.plugin.java.JavaPlugin;

public class HorizonTestPlugin extends JavaPlugin {

    @Override
    public void onLoad() {
        getLogger().info("Test1: " + JavaPlugin.getPlugin(HorizonTestPlugin.class).getName());
        getLogger().info("Test2: " + JavaPlugin.getProvidingPlugin(HorizonTestPlugin.class).getName());
    }

    @Override
    public void onEnable() {
        getLogger().info("Hello!");
    }
}
