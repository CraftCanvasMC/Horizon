package io.canvasmc.horizon.util.tree;

import java.io.InputStream;
import java.io.Reader;
import java.util.List;
import java.util.Map;

public interface FormatParser {

    Map<String, Object> parse(InputStream input, List<ParseError> errors) throws Exception;

    Map<String, Object> parse(Reader reader, List<ParseError> errors) throws Exception;

    Map<String, Object> parse(String content, List<ParseError> errors) throws Exception;
}
