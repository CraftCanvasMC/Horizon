package io.canvasmc.horizon.util.resolver;

/**
 * Represents when the artifact is unable to download from the attempted repository, commonly meaning that the artifact
 * does not belong to the repository, but belongs to a different one
 *
 * @author dueris
 */
public class RejectedRepositoryException extends RuntimeException {
    RejectedRepositoryException() {
        super();
    }
}
