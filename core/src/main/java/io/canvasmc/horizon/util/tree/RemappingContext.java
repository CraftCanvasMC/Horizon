package io.canvasmc.horizon.util.tree;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record RemappingContext(Map<String, String> variables) {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{([^}]+)}");

    public RemappingContext(Map<String, String> variables) {
        this.variables = new HashMap<>(variables);

        // add properties and env vars
        System.getProperties().forEach((key, value) ->
            this.variables.putIfAbsent("sys." + key, String.valueOf(value))
        );
        System.getenv().forEach((key, value) ->
            this.variables.putIfAbsent("env." + key, value)
        );
    }

    public String interpolate(String value) {
        if (value == null) {
            return null;
        }

        Matcher matcher = VARIABLE_PATTERN.matcher(value);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String varName = matcher.group(1);
            String varValue = variables.get(varName);

            if (varValue == null) {
                throw new InterpolationException("Variable not found: " + varName);
            }

            matcher.appendReplacement(result, Matcher.quoteReplacement(varValue));
        }

        matcher.appendTail(result);
        return result.toString();
    }
}
