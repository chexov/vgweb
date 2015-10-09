package com.vg.web.view;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class RedirectView implements View {
    private String redirect;

    public RedirectView(String redirect) {
        this.redirect = redirect;
    }

    public void view(HttpServletRequest request, HttpServletResponse response) {
        try {
            response.sendRedirect(redirect);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
