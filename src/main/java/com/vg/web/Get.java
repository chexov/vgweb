package com.vg.web;

import com.vg.web.view.View;

import javax.servlet.http.HttpServletRequest;

@FunctionalInterface
public interface Get {
    View get(HttpServletRequest request, StrParser pathInfo) throws Exception;
}
