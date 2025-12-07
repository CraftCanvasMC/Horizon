package io.canvasmc.horizon.service;

import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.launch.platform.container.ContainerHandleURI;
import org.spongepowered.asm.launch.platform.container.ContainerHandleVirtual;

import java.nio.file.Path;

public class MixinContainerHandle extends ContainerHandleVirtual {

    public MixinContainerHandle(String name) {
        super(name);
    }

    public void addResource(final @NonNull String name, final @NonNull Path path) {
        this.add(new Resource(name, path));
    }

    public static class Resource extends ContainerHandleURI {
        private final String name;
        private final Path path;

        private Resource(final @NonNull String name, final @NonNull Path path) {
            super(path.toUri());

            this.name = name;
            this.path = path;
        }

        public @NonNull String name() {
            return this.name;
        }

        public @NonNull Path path() {
            return this.path;
        }

        @Override
        public String toString() {
            return "Resource{" +
                "name='" + name + '\'' +
                ", path=" + path +
                '}';
        }
    }
}
