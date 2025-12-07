package io.canvasmc.testplugin;

import org.bukkit.plugin.java.JavaPlugin;

public class HorizonTestPlugin extends JavaPlugin {

    @Override
    public void onLoad() {
        getLog4JLogger().info("Test1: {}", JavaPlugin.getPlugin(HorizonTestPlugin.class).getName());
        getLog4JLogger().info("Test2: {}", JavaPlugin.getProvidingPlugin(HorizonTestPlugin.class).getName());
    }

    @Override
    public void onEnable() {
        getLogger().info("Hello!");
    }
}
