package com.vg.web.view;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class TextView implements View {
    private String text;
    private long mtime;
    private String contentType;

    public TextView(String text) {
        this(text, System.currentTimeMillis(), "text/plain");
    }

    public TextView(String text, long mtime) {
        this(text, mtime, "text/plain");
    }

    public TextView(String text, long mtime, String contentType) {
        this.text = text;
        this.mtime = mtime;
        this.contentType = contentType;
    }

    @Override
    public void view(HttpServletRequest request, HttpServletResponse response) throws IOException {
        byte[] bytes = text.getBytes("UTF-8");
        response.setContentLength(bytes.length);
        response.setContentType(contentType);
        response.setCharacterEncoding("UTF-8");
        response.setDateHeader("Last-Modified", mtime);
        response.getOutputStream().write(bytes);
    }

}
