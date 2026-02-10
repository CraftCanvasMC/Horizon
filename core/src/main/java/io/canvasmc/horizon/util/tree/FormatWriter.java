package io.canvasmc.horizon.util.tree;

import java.io.OutputStream;
import java.io.Writer;
import java.util.Map;

/**
 * The writer interface for writing raw tree data to various outputs as a format, like out streams, writers, and
 * strings
 *
 * @author dueris
 */
public interface FormatWriter {
    /**
     * Writes the data to an output stream
     *
     * @param data
     *     raw data
     * @param output
     *     the out stream
     *
     * @throws Exception
     *     if unable to write
     */
    void write(Map<String, Object> data, OutputStream output) throws Exception;

    /**
     * Writes the data to a writer
     *
     * @param data
     *     raw data
     * @param writer
     *     the writer
     *
     * @throws Exception
     *     if unable to write
     */
    void write(Map<String, Object> data, Writer writer) throws Exception;

    /**
     * Writes the data to a string
     *
     * @param data
     *     raw data
     *
     * @return the data written as a string
     *
     * @throws Exception
     *     if unable to write
     */
    String writeToString(Map<String, Object> data) throws Exception;
}
