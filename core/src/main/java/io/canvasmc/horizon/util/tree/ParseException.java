package io.canvasmc.horizon.util.tree;

import org.jspecify.annotations.NonNull;

import java.util.Collections;
import java.util.List;

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
