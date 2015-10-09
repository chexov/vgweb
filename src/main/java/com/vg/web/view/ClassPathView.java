package com.vg.web.view;

import static com.vg.web.view.CachedFileView.isNotModified;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.vg.web.MimeTypes;
import org.apache.commons.io.IOUtils;

public class ClassPathView implements View {

    private String resource;
    private String mime;

    public ClassPathView(String resource) {
        this.resource = resource;
        this.mime = MimeTypes.INSTANCE.getByExtension(resource);
    }

    public ClassPathView(String resource, String mime) {
        this.resource = resource;
        this.mime = mime;
    }

    public void view(HttpServletRequest request, HttpServletResponse response) throws IOException {
        InputStream is = null;
        try {
            ClassLoader classLoader = this.getClass().getClassLoader();
            URL resource2 = classLoader.getResource(resource);
            if (resource2 == null) {
                response.sendError(404);
                return;
            }
            URLConnection openConnection = resource2.openConnection();
            long mtime = openConnection.getLastModified();
            if (isNotModified(request, mtime)) {
                response.sendError(304);
                return;
            }
            int contentLength = openConnection.getContentLength();
            response.setContentLength(contentLength);
            response.setContentType(mime);
            is = openConnection.getInputStream();
            response.setDateHeader("Last-Modified", mtime);
            response.addHeader("Cache-Control", "public,");
            IOUtils.copy(is, response.getOutputStream());
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

}
