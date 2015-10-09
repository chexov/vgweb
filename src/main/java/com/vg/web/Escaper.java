package com.vg.web;

import org.apache.commons.lang.StringEscapeUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class Escaper {
    public static String url(String url) throws UnsupportedEncodingException {
        return URLEncoder.encode(url, "utf-8").replace("+", "%20");
    }

    public static String html(String html) {
        return StringEscapeUtils.escapeHtml(html);
    }

    public static String js(String js) {
        return StringEscapeUtils.escapeJavaScript(js);
    }
}
