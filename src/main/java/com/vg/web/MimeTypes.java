package com.vg.web;

import static org.apache.commons.io.FilenameUtils.getExtension;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class MimeTypes {
    public static final MimeTypes INSTANCE = new MimeTypes();
    private static final Map<String, String> mimetypes = new HashMap<String, String>();

    static {
        mimetypes.put("css", "text/css");
        mimetypes.put("mov", "video/quicktime");
        mimetypes.put("js", "application/javascript");
        mimetypes.put("png", "image/png");
        mimetypes.put("svg", "image/svg+xml");
        mimetypes.put("svgz", "image/svg+xml");

        mimetypes.put("woff", "application/font-woff");
        mimetypes.put("woff2", "application/font-woff2");
        mimetypes.put("ttf", "application/x-font-truetype");
        mimetypes.put("otf", "application/x-font-opentype");

        mimetypes.put("jpeg", "image/jpeg");
        mimetypes.put("jpg", "image/jpeg");
        mimetypes.put("gif", "image/gif");
        mimetypes.put("ico", "image/x-icon");
        mimetypes.put("pdf", "application/pdf");
        mimetypes.put("htm", "text/html");
        mimetypes.put("txt", "text/plain");
        mimetypes.put("hbs", "text/plain");
        mimetypes.put("html", "text/html");
        mimetypes.put("mht", "text/html");
        mimetypes.put("xml", "text/xml");
        mimetypes.put("rtf", "application/rtf");
        mimetypes.put("doc", "application/msword");
        mimetypes.put("xls", "application/ms-excel");
        mimetypes.put("wav", "audio/x-wav");
        mimetypes.put("ogg", "audio/ogg");
        mimetypes.put("mp3", "audio/mpeg");
        mimetypes.put("mp4", "video/mp4");
        mimetypes.put("webm", "video/webm");
        mimetypes.put("swf", "application/x-shockwave-flash");
    }

    public String get(String extension) {
        extension = extension.toLowerCase();
        String mime = "application/octet-stream";
        if (mimetypes.containsKey(extension)) {
            mime = mimetypes.get(extension);
        }
        return mime;
    }

    public String get(File file) {
        return get(getExtension(file.getName().toLowerCase()));
    }

    public String get(URL url) {
        return get(getExtension(url.getFile().toLowerCase()));
    }

    public String getByExtension(String filename) {
        return get(getExtension(filename.toLowerCase()));
    }

}
