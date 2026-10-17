package dev.paperswap.runtime.loader;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/** Input stream that fails once more than a fixed number of bytes was read (zip-bomb guard). */
public final class LimitedInputStream extends FilterInputStream {

    private long remaining;

    /** @param in stream @param limit maximum number of bytes */
    public LimitedInputStream(InputStream in, long limit) {
        super(in);
        this.remaining = limit;
    }

    @Override
    public int read() throws IOException {
        int b = super.read();
        if (b >= 0 && --this.remaining < 0) {
            throw new IOException("entry exceeds size limit");
        }
        return b;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int n = super.read(b, off, len);
        if (n > 0) {
            this.remaining -= n;
            if (this.remaining < 0) {
                throw new IOException("entry exceeds size limit");
            }
        }
        return n;
    }
}
