package io.canvasmc.horizon.util.tree;

import org.jspecify.annotations.NonNull;

import java.util.Collections;
import java.util.List;

/**
 * Thrown when an {@link io.canvasmc.horizon.util.tree.ObjectTree} fails to parse for any number of reasons, containing
 * a list of {@link io.canvasmc.horizon.util.tree.ParseError} instances detailing why the parse may have failed for any
 * number of reasons
 *
 * @author dueris
 */
public final class ParseException extends Exception {
    private final List<ParseError> errors;

    private static @NonNull String buildMessage(@NonNull List<ParseError> errors) {
        StringBuilder sb = new StringBuilder("Failed to parse configuration (")
            .append(errors.size())
            .append(" error(s)):\n");

        for (int i = 0; i < errors.size(); i++) {
            sb.append("  - ").append(errors.get(i)).append(i == (errors.size() - 1) ? "" : "\n");
        }

        return sb.toString();
    }

    public ParseException(List<ParseError> errors) {
        super(buildMessage(errors));
        this.errors = Collections.unmodifiableList(errors);
    }

    public List<ParseError> getErrors() {
        return errors;
    }
}
