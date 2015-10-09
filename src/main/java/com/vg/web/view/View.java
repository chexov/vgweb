package com.vg.web.view;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface View {

    public static final long SEC = 1000;
    public static final long MIN = 60 * SEC;
    public static final long HOUR = 60 * MIN;
    public static final long DAY = 24 * HOUR;
    public static final long YEAR = 365 * DAY;

    void view(HttpServletRequest request, HttpServletResponse response) throws IOException;
}
