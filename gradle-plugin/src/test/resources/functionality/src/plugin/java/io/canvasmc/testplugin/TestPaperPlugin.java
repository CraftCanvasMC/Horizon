package io.canvasmc.testplugin;

import org.bukkit.plugin.java.JavaPlugin;
import net.minecraft.CrashReport;

public class TestPaperPlugin extends JavaPlugin {
    StackTraceElement[] element;
    TestLoader loader;

    public TestPaperPlugin() {
        element = new CrashReport("test", new Throwable()).uncategorizedStackTrace;
        loader = new TestLoader();
        loader.test();
    }

    @Override
    public void onLoad() {
        getLog4JLogger().info("Test1: {}", JavaPlugin.getPlugin(TestPaperPlugin.class).getName());
        getLog4JLogger().info("Test2: {}", JavaPlugin.getProvidingPlugin(TestPaperPlugin.class).getName());
    }

    @Override
    public void onEnable() {
        getLogger().info("Hello test!");
    }
}
