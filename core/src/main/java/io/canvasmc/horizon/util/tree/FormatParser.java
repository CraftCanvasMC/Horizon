package io.canvasmc.horizon.util.tree;

import java.io.InputStream;
import java.io.Reader;
import java.util.List;
import java.util.Map;

/**
 * The parser interface for reading various inputs and providing a raw object tree for
 * {@link io.canvasmc.horizon.util.tree.ObjectTree} constructing, allowing to add parse errors to signal failures and
 * add debugging
 *
 * @author dueris
 */
public interface FormatParser {
    /**
     * Parses an input stream to raw tree data
     *
     * @param input
     *     the in stream
     * @param errors
     *     errors to add to if parsing goes wrong
     *
     * @return the raw tree data
     *
     * @throws Exception
     *     if unable to parse and hard-fails
     */
    Map<String, Object> parse(InputStream input, List<ParseError> errors) throws Exception;

    /**
     * Parses a reader to raw tree data
     *
     * @param reader
     *     the reader
     * @param errors
     *     errors to add to if parsing goes wrong
     *
     * @return the raw tree data
     *
     * @throws Exception
     *     if unable to parse and hard-fails
     */
    Map<String, Object> parse(Reader reader, List<ParseError> errors) throws Exception;

    /**
     * Parses a string to raw tree data
     *
     * @param content
     *     the string
     * @param errors
     *     errors to add to if parsing goes wrong
     *
     * @return the raw tree data
     *
     * @throws Exception
     *     if unable to parse and hard-fails
     */
    Map<String, Object> parse(String content, List<ParseError> errors) throws Exception;
}
