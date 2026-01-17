package io.canvasmc.horizon;

import io.canvasmc.horizon.instrument.JvmAgent;
import io.canvasmc.horizon.util.resolver.Artifact;
import io.canvasmc.horizon.util.resolver.DependencyResolver;
import io.canvasmc.horizon.util.resolver.Repository;
import io.canvasmc.horizon.util.Util;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class Main {

    public static void main(String[] args) {
        // TODO - can we support this..?
        if (Boolean.getBoolean("paper.useLegacyPluginLoading")) {
            throw new IllegalStateException("Legacy plugin loading is unsupported with Horizon");
        }
        String version;
        JarFile sourceJar;
        try {
            //noinspection resource
            sourceJar = new JarFile(Main.class.getProtectionDomain().getCodeSource().getLocation().getFile());
            Manifest manifest = sourceJar.getManifest();
            version = manifest.getMainAttributes().getValue(Attributes.Name.IMPLEMENTATION_VERSION);
        } catch (IOException e) {
            throw new RuntimeException("Couldn't fetch source jar", e);
        }

        List<Path> initialClasspath = new ArrayList<>();

        // first, boot dependency resolver so we can actually run things without dying
        new DependencyResolver(new File("libraries"), () -> {
            return Util.parseFrom(sourceJar, "META-INF/artifacts.context", (line) -> {
                String[] split = line.split("\t");
                String id = split[0];
                String path = split[1];
                String sha256 = split[2];
                return new Artifact(id, path, sha256);
            }, Artifact.class);
        }, () -> {
            return Util.parseFrom(sourceJar, "META-INF/repositories.context", (line) -> {
                String[] split = line.split("\t");
                String name = split[0];
                URL url = URI.create(split[1]).toURL();
                return new Repository(name, url);
            }, Repository.class);
        }).resolve().forEach((jar) -> {
            initialClasspath.add(jar.ioFile().toPath());
            JvmAgent.addJar(jar.jarFile());
        });

        // load properties and start horizon init
        ServerProperties properties = ServerProperties.load(args);

        // cleanup directory for plugins
        File cacheDirectory = properties.cacheLocation();
        Util.clearDirectory(cacheDirectory);

        new HorizonLoader(properties, version, JvmAgent.INSTRUMENTATION, initialClasspath, args);
    }
}
