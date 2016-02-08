package com.vg.web.view;

import static javax.servlet.http.HttpServletResponse.SC_PARTIAL_CONTENT;
import static org.apache.commons.io.IOUtils.copy;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.vg.web.ContentRange;
import com.vg.web.MimeTypes;
import com.vg.web.ServletUtil;
import org.apache.commons.io.IOUtils;

import com.vg.io.SeekableFileInputStream;

public class ContentRangeFileView implements View {
    private final File file;
    private final ContentRange range;

    public ContentRangeFileView(File file, ContentRange range) {
        this.file = file;
        this.range = range;
        if (range.getEnd() + 1 > file.length()) {
            throw new IllegalArgumentException("content range outside of file");
        }
    }

    @Override
    public void view(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType(MimeTypes.INSTANCE.get(file));
        resp.setHeader("Accept-Ranges", "bytes");
        resp.setDateHeader("Last-Modified", file.lastModified());
        InputStream is = null;
        try {
            resp.setStatus(SC_PARTIAL_CONTENT);
            resp.setHeader("Content-Range", range.toHeaderValue());
            resp.setHeader("Content-Length", String.valueOf(range.getContentLength()));
            resp.setHeader("Content-Type", MimeTypes.INSTANCE.get(file));
            is = range.limitedInputStream(new SeekableFileInputStream(file));
            // System.out.println(resp);
            copy(is, resp.getOutputStream());
        } catch (Throwable e) {
            ServletUtil.ignoreClientReset(e);
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

}