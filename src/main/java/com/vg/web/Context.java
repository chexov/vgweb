package com.vg.web;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.velocity.VelocityContext;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class Context extends VelocityContext {

    private static Map<String, Object> global = new HashMap<String, Object>();

    public Context() {
        for (Map.Entry<String, Object> e : global.entrySet()) {
            this.put(e.getKey(), e.getValue());
        }
    }

    public Context put(String key, Object value) {
        super.put(key, value);
        return this;
    }

    public static Context create() {
        Context ctxt = new Context();
        ctxt.put("esc", new Escaper());
        return ctxt;
    }

    public static Context create(HttpServletRequest req) {
        Context ctxt = Context.create();
        Enumeration names = req.getParameterNames();
        while (names.hasMoreElements()) {
            String name = (String) names.nextElement();
            ctxt.put(name, StringEscapeUtils.escapeHtml4(req.getParameter(name)));
        }
        Cookie[] cookies = req.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                String name = cookie.getName();
                String value = cookie.getValue();
                ctxt.put(name, StringEscapeUtils.escapeHtml4(value));
            }
        }
        return ctxt;
    }

    public static void set(String key, Object value) {
        global.put(key, value);
    }
}
