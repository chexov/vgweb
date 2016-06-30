package com.vg.web;

import javax.servlet.http.HttpServletRequest;

import com.vg.web.view.RedirectView;
import com.vg.web.view.View;

public class RedirectController extends Controller {
    private static final long serialVersionUID = 1L;
    private final String url;

    public RedirectController(String url) {
        this.url = url;
    }

    public View post(HttpServletRequest request) throws Exception {
        return get(request, null);
    }

    public View get(HttpServletRequest request, StrParser pathInfo) throws Exception {
        return new RedirectView(url);
    }

}
