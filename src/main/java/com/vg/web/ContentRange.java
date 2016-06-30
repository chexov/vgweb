package com.vg.web;

import static java.lang.Long.parseLong;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import com.vg.io.SeekableInputStream;
import com.vg.web.io.LimitedInputStream;

public class ContentRange {
    private final long end;
    private final long start;
    private final long count;
    private final long totalLength;

    public ContentRange(long start, long end, long totalLength) {
        super();
        this.end = end;
        this.start = start;
        this.totalLength = totalLength;
        this.count = end - start + 1;
    }

    /**
     * If the last-byte-pos value is absent, or if the value is greater than or
     * equal to the current length of the entity-body, last-byte-pos is taken to
     * be equal to one less than the current length of the entity- body in
     * bytes.
     */
    public static ContentRange parseRange(String headerValue, long fileLength) {
        String[] string = headerValue.split("=")[1].split("-");
        long start = parseLong(string[0]);
        long end = string.length == 1 ? fileLength - 1 : parseLong(string[1]);
        end = Math.min(end, fileLength - 1);
        return new ContentRange(start, end, fileLength);
    }

    public long getEnd() {
        return end;
    }

    public long getStart() {
        return start;
    }

    public long getCount() {
        return count;
    }

    @Override
    public String toString() {
        return "ContentRange [start=" + start + ", end=" + end + ", count=" + count + ", totalLength=" + totalLength
                + "]";
    }

    public String toHeaderValue() {
        return "bytes " + start + "-" + end + "/" + totalLength;
    }

    public long getContentLength() {
        return count;
    }

    public InputStream limitedInputStream(SeekableInputStream in) throws IOException {
        in.seek(getStart());
        return new LimitedInputStream(in, getCount());
    }

    public boolean overlaps(ContentRange range) {
        if (this.getCount() <= 0 || range == null || range.getCount() <= 0) {
            return false;
        }
        return range.contains(start) || range.contains(end) || contains(range.start);
    }

    public boolean contains(long value) {
        return value >= start && value <= end;
    }

    public boolean overlapsAny(Collection<ContentRange> ranges) {
        for (ContentRange r : ranges) {
            if (this.overlaps(r)) {
                return true;
            }
        }
        return false;
    }
}
