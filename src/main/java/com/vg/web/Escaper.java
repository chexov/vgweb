package com.vg.web;


import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.commons.lang3.StringEscapeUtils;

public class Escaper {
    public static String url(String url) throws UnsupportedEncodingException {
        return URLEncoder.encode(url, "utf-8").replace("+", "%20");
    }

    public static String html(String html) {
        return StringEscapeUtils.escapeHtml4(html);
    }

    public static String js(String js) {
        return StringEscapeUtils.escapeEcmaScript(js);
    }
}
