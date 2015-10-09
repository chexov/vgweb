package com.vg.web.io;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class LimitedInputStream extends FilterInputStream {

    private final long limit;

    private long pos;

    public LimitedInputStream(InputStream in, long limit) {
        super(in);
        this.limit = limit < 0 ? 0 : limit;
    }

    public int read() throws IOException {
        if (pos < limit) {
            pos++;
            return super.read();
        }
        return -1;
    }

    public int read(byte[] b, int off, int len) throws IOException {
        if (limit - pos < len) {
            len = (int) (limit - pos);
            if (len <= 0) {
                return -1;
            }
        }

        int res = super.read(b, off, len);
        if (res > 0) {
            pos += res;
        }
        return res;
    }

    @Override
    public long skip(long n) throws IOException {
        if (n > (limit - pos)) {
            n = limit - pos;
        }
        long s = in.skip(n);
        pos += s;
        return s;
    }

    @Override
    public void reset() throws IOException {
        pos = 0;
        in.reset();
    }

}
