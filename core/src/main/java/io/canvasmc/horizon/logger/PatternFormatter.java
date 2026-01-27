package io.canvasmc.horizon.logger;

import org.jspecify.annotations.NonNull;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PatternFormatter {

    protected final String pattern;
    private final Pattern tokenPattern = Pattern.compile("\\{([^}]*)}");
    private final ThreadLocal<Map<String, SimpleDateFormat>> dateFormatters =
        ThreadLocal.withInitial(HashMap::new);

    public PatternFormatter(String pattern) {
        this.pattern = pattern;
    }

    public String format(@NonNull LogEntry entry, Object[] args) {
        String message = formatMessage(entry.message, args);

        Matcher m = tokenPattern.matcher(pattern);
        StringBuilder sb = new StringBuilder();

        while (m.find()) {
            String token = m.group(1);
            String replacement = resolveToken(token, entry, message);
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);

        if (entry.throwable != null) {
            if (!message.isEmpty()) sb.append("\n");
            StringWriter sw = new StringWriter();
            entry.throwable.printStackTrace(new PrintWriter(sw));
            sb.append(sw);
        }

        return sb.toString();
    }

    private String formatMessage(String message, Object[] args) {
        if (args == null || args.length == 0) {
            return message;
        }

        StringBuilder result = new StringBuilder();
        int argIndex = 0;
        int i = 0;

        while (i < message.length()) {
            if (i < message.length() - 1 && message.charAt(i) == '{' && message.charAt(i + 1) == '}') {
                if (argIndex < args.length) {
                    result.append(args[argIndex++]);
                }
                else {
                    result.append("{}");
                }
                i += 2;
            }
            else {
                result.append(message.charAt(i));
                i++;
            }
        }

        return result.toString();
    }

    private String resolveToken(@NonNull String token, LogEntry entry, String formattedMessage) {
        if (token.startsWith("date:")) {
            String format = token.substring(5).trim();
            Map<String, SimpleDateFormat> formatters = dateFormatters.get();
            SimpleDateFormat sdf = formatters.get(format);
            if (sdf == null) {
                sdf = new SimpleDateFormat(format);
                formatters.put(format, sdf);
            }
            return sdf.format(new Date(entry.timestamp));
        }

        return switch (token) {
            case "level" -> entry.level.name();
            case "logger", "name", "tag" -> entry.loggerName;
            case "message", "msg" -> formattedMessage;
            case "thread" -> entry.threadName;
            case "timestamp" -> String.valueOf(entry.timestamp);
            case "class" -> entry.caller;
            default -> "{" + token + "}";
        };
    }
}
