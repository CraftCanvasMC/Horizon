package io.canvasmc.horizon.plugin.phase.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import io.canvasmc.horizon.Horizon;
import io.canvasmc.horizon.plugin.LoadContext;
import io.canvasmc.horizon.plugin.data.HorizonMetadata;
import io.canvasmc.horizon.plugin.phase.Phase;
import io.canvasmc.horizon.plugin.phase.PhaseException;
import io.canvasmc.horizon.plugin.types.HorizonPlugin;
import io.canvasmc.horizon.plugin.types.PluginCandidate;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.io.Reader;
import java.util.*;

public class BuilderPhase implements Phase<Set<PluginCandidate>, List<HorizonPlugin>> {
    @Override
    public List<HorizonPlugin> execute(@NonNull Set<PluginCandidate> input, LoadContext context) throws PhaseException {
        List<HorizonPlugin> completed = new ArrayList<>();

        Gson gson = new GsonBuilder()
                .disableHtmlEscaping()
                .serializeNulls()
                .create();

        for (PluginCandidate candidate : input) {
            Map<String, Object> raw = candidate.metadata().rawData();
            // TODO - simplify?
            HorizonMetadata horizonMetadata = Horizon.GSON.fromJson(new JsonReader(new Reader() {
                private String json = gson.toJson(raw);
                private final int length = json.length();
                private int next = 0;
                private int mark = 0;

                private void ensureOpen() throws IOException {
                    if (json == null)
                        throw new IOException("Stream closed");
                }

                @Override
                public int read() throws IOException {
                    synchronized (lock) {
                        ensureOpen();
                        if (next >= length)
                            return -1;
                        return json.charAt(next++);
                    }
                }

                @Override
                public int read(char @NonNull [] cbuf, int off, int len) throws IOException {
                    synchronized (lock) {
                        ensureOpen();
                        Objects.checkFromIndexSize(off, len, cbuf.length);

                        if (len == 0) return 0;
                        if (next >= length) return -1;

                        int n = Math.min(length - next, len);
                        json.getChars(next, next + n, cbuf, off);
                        next += n;
                        return n;
                    }
                }

                @Override
                public long skip(long n) throws IOException {
                    synchronized (lock) {
                        ensureOpen();
                        if (next >= length) return 0;

                        long r = Math.min(n, (long) length - next);
                        r = Math.max(r, -next);

                        next += (int) r;
                        return r;
                    }
                }

                @Override
                public boolean ready() throws IOException {
                    synchronized (lock) {
                        ensureOpen();
                        return true;
                    }
                }

                @Override
                public boolean markSupported() {
                    return true;
                }

                @Override
                public void mark(int readAheadLimit) throws IOException {
                    if (readAheadLimit < 0)
                        throw new IllegalArgumentException("Read-ahead limit < 0");

                    synchronized (lock) {
                        ensureOpen();
                        mark = next;
                    }
                }

                @Override
                public void reset() throws IOException {
                    synchronized (lock) {
                        ensureOpen();
                        next = mark;
                    }
                }

                @Override
                public void close() {
                    synchronized (lock) {
                        json = null;
                    }
                }
            }), HorizonMetadata.class);
            if (horizonMetadata == null) {
                throw new PhaseException("Couldn't deserialize horizon metadata for candidate '" + candidate.metadata().name() + "'");
            }
            completed.add(new HorizonPlugin(horizonMetadata.name(), candidate.fileJar(), horizonMetadata));
        }

        return completed;
    }

    @Override
    public String getName() {
        return "Builder";
    }
}
