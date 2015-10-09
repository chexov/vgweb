package com.vg.web.view;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.vg.web.MimeTypes;
import com.vg.web.ServletUtil;
import com.vg.web.view.View;
import org.apache.commons.io.IOUtils;

public class CachedFileView implements View {

    private static final long YEAR = 365L * 24L * 60L * 60L * 1000L;

    private final File file;

    private final String contentType;

    public CachedFileView(File file) {
        this(MimeTypes.INSTANCE.get(file), file);
    }

    public CachedFileView(String contentType, File file) {
        this.contentType = contentType;
        this.file = file;
    }

    public void view(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (file.exists()) {
            if (isNotBlank(contentType)) {
                response.setContentType(contentType);
            }

//            response.setHeader("Accept-Ranges", "bytes");
            long mtime = file.lastModified();
            if (isNotModified(request, mtime)) {
                response.sendError(304);
                return;
            }

            response.setHeader("Content-Length", String.valueOf(file.length()));
            response.setDateHeader("Last-Modified", mtime);
            response.setDateHeader("Expires", mtime + 10L * YEAR);
            if (request.getMethod().equals("GET")) {
                InputStream is = null;
                try {
                    is = new BufferedInputStream(new FileInputStream(file));
                    IOUtils.copy(is, response.getOutputStream());
                } catch (Throwable e) {
                    ServletUtil.ignoreClientReset(e);
                } finally {
                    IOUtils.closeQuietly(is);
                }
            } else {
                response.flushBuffer();
            }
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    public static boolean isNotModified(HttpServletRequest request, long mtime) {
        long cachedTime = request.getDateHeader("If-Modified-Since");
        return (cachedTime != -1 && mtime <= cachedTime);
    }

}
