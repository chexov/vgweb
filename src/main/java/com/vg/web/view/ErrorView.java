package com.vg.web.view;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.collect.ImmutableMap;

public class ErrorView implements View {

    public static final ErrorView OK = new ErrorView(200);

    public static ErrorView NotModified = new ErrorView(304);

    public final static ErrorView BadRequest = new ErrorView(400);
    public final static ErrorView Unauthorized = new ErrorView(401);
    public final static ErrorView NotFound = new ErrorView(404);
    public final static ErrorView Conflict = new ErrorView(409);
    public final static ErrorView Forbidden = new ErrorView(403);
    public final static ErrorView UnsupportedMediaType = new ErrorView(415);
    public final static JsonView Error403js = new JsonView(403, ImmutableMap.of("error", "no access"));
    public final static JsonView Error417js = new JsonView(417, ImmutableMap.of("error", "permisson violation"));
    public final static JsonView Error404js = new JsonView(404, ImmutableMap.of("error", "not found"));
    public static final ErrorView MethodNotAllowed = new ErrorView(405);

    public final static ErrorView NotImplemented = new ErrorView(501);


    public static final String CONTENT_TYPE_TEXT_HTML = "text/html";
    public static final String UTF_8 = "UTF-8";

    private int httpResponse;

    public ErrorView(int httpResponse) {
        this.httpResponse = httpResponse;
    }

    public static View badRequestJs(String descr) {
        return new JsonView(400, ImmutableMap.of("error", descr));
    }

    public void view(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.sendError(httpResponse);
    }

    public int getHttpResponse() {
        return httpResponse;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj instanceof ErrorView) {
            return getHttpResponse() == ((ErrorView) obj).getHttpResponse();
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(getHttpResponse());
    }

    @Override
    public String toString() {
        return "ErrorView(" + getHttpResponse() + ")";
    }

}
