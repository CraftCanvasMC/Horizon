package io.canvasmc.horizon.util.resolver;

import org.jspecify.annotations.NonNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Represents the artifact to be downloaded from the resolver
 *
 * @param artifactId
 *     the artifact id
 * @param path
 *     the artifact path
 * @param sha256
 *     the sha256 of the artifact
 *
 * @author dueris
 */
public record Artifact(String artifactId, String path, String sha256) {

    private static byte @NonNull [] readAllBytes(@NonNull InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }

    private static boolean verifySha256(byte[] data, String expectedHex) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            return toHex(hash).equalsIgnoreCase(expectedHex);
        } catch (NoSuchAlgorithmException e) {
            // never should happen on any sane JVM
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static @NonNull String toHex(byte @NonNull [] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }

    byte @NonNull [] download(@NonNull URL repository) throws RejectedRepositoryException, SecurityException, IOException {
        URL artifactUrl = URI.create(
            repository.toString().endsWith("/")
                ? repository + path
                : repository + "/" + path
        ).toURL();

        HttpURLConnection connection = (HttpURLConnection) artifactUrl.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(10_000);
        connection.setReadTimeout(30_000);
        connection.setInstanceFollowRedirects(true);

        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new RejectedRepositoryException();
        }

        byte[] data;
        try (InputStream in = connection.getInputStream()) {
            data = readAllBytes(in);
        } finally {
            connection.disconnect();
        }

        if (!verifySha256(data, sha256)) {
            throw new SecurityException("SHA-256 mismatch for artifact: " + artifactId + " @ " + artifactUrl);
        }

        return data;
    }
}
