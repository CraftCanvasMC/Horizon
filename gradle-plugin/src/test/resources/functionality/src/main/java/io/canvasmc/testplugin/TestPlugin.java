package io.canvasmc.testplugin;

import org.bukkit.plugin.java.JavaPlugin;
import net.minecraft.CrashReport;

public class TestPlugin extends JavaPlugin {
    StackTraceElement[] element;

    public TestPlugin() {
        element = new CrashReport("test", new Throwable()).uncategorizedStackTrace;
    }

    @Override
    public void onLoad() {
        getLog4JLogger().info("Test1: {}", JavaPlugin.getPlugin(TestPlugin.class).getName());
        getLog4JLogger().info("Test2: {}", JavaPlugin.getProvidingPlugin(TestPlugin.class).getName());
    }

    @Override
    public void onEnable() {
        getLogger().info("Hello test!");
    }
}
