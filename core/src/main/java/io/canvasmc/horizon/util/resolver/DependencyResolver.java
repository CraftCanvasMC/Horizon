package io.canvasmc.horizon.util.resolver;

import io.canvasmc.horizon.HorizonLoader;
import io.canvasmc.horizon.util.FileJar;
import io.canvasmc.horizon.util.Util;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;
import java.util.jar.JarFile;
import java.util.stream.Stream;

public class DependencyResolver {

    private final File out;
    private final Supplier<Artifact[]> artifactSupplier;
    private final Supplier<Repository[]> repositorySupplier;

    public DependencyResolver(File out, Supplier<Artifact[]> artifactSupplier, Supplier<Repository[]> repositorySupplier) {
        this.out = out;
        this.artifactSupplier = artifactSupplier;
        this.repositorySupplier = repositorySupplier;
    }

    public Stream<FileJar> resolve() {
        // we need to gather artifacts first, then repos, iterate over artifacts
        // and for each repo we try and resolve until we can't anymore
        HorizonLoader.LOGGER.info("Resolving dependencies...");

        // get artifacts and repos
        Artifact[] artifacts = this.artifactSupplier.get();
        Repository[] repositories = this.repositorySupplier.get();

        // build threading service
        ExecutorService service = Executors.newVirtualThreadPerTaskExecutor();
        List<Future<FileJar>> futures = new ArrayList<>(artifacts.length);

        try {
            for (Artifact artifact : artifacts) {
                // in testing, this is genuinely so much faster it's not even funny...
                futures.add(service.submit(() -> {
                    for (Repository repository : repositories) {
                        String path = out.getName() + "/" + artifact.path();
                        File output = new File(path);

                        try {
                            if (output.exists()) {
                                return new FileJar(output, new JarFile(output));
                            }

                            byte[] downloaded = artifact.download(repository.url());
                            output = Util.getOrCreateFile(path);

                            try (FileOutputStream stream = new FileOutputStream(output)) {
                                stream.write(downloaded);
                            }

                            return new FileJar(output, new JarFile(output));
                        } catch (SecurityException e) {
                            // this only happens on SHA-256 failure, kill immediately
                            throw Util.kill(e.getMessage(), e);
                        } catch (RejectedRepositoryException ignored) {
                            // try next repository
                        }
                    }

                    throw new RuntimeException("Failed to resolve " + artifact.artifactId());
                }));
            }

            // collect and wait for all to complete
            List<FileJar> result = new ArrayList<>(artifacts.length);

            for (Future<FileJar> future : futures) {
                try {
                    result.add(future.get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Dependency resolution interrupted", e);
                } catch (ExecutionException e) {
                    throw new RuntimeException("Dependency resolution failed", e.getCause());
                }
            }

            // return stream
            return result.stream();
        } finally {
            // shutdown executor
            service.shutdown();
        }
    }
}
