package com.vg.web.view;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.eclipse.jetty.io.EofException;

import com.google.common.collect.ImmutableMap;

public class ErrorView implements View {

    //    public final static Logger log = LogManager.getLogger(ErrorView.class);
    public static final ErrorView OK = new ErrorView(200);
    public static ErrorView NotModified = new ErrorView(304);

    public final static ErrorView BAD_REQUEST = new ErrorView(400);
    public final static ErrorView BadRequest = BAD_REQUEST;
    public final static ErrorView Unauthorized = new ErrorView(401);

    public final static ErrorView NotFound = new ErrorView(404);
    public final static ErrorView Conflict = new ErrorView(409);
    public final static ErrorView Forbidden = new ErrorView(403);
    public final static ErrorView Unsupported_Media_Type = new ErrorView(415);
    public final static ErrorView Not_Implemented = new ErrorView(501);

    public final static JsonView Error403js = new JsonView(403, ImmutableMap.of("error", "no access"));
    public final static JsonView Error417js = new JsonView(417, ImmutableMap.of("error", "permisson violation"));
    public final static JsonView Error404js = new JsonView(404, ImmutableMap.of("error", "not found"));

    public static final String CONTENT_TYPE_TEXT_HTML = "text/html";
    public static final String UTF_8 = "UTF-8";

    private int httpResponse;
    private String contentType;

    public ErrorView(int httpResponse, String contentType) {
        this.httpResponse = httpResponse;
        this.contentType = contentType;
    }

    public ErrorView(int httpResponse) {
        this.httpResponse = httpResponse;
    }

    public static View badRequestJs(String descr) {
        return new JsonView(400, ImmutableMap.of("error", descr));
    }

    public void view(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            response.setStatus(this.httpResponse);
            response.setContentType(CONTENT_TYPE_TEXT_HTML);
            response.setCharacterEncoding(UTF_8);

            if (this.httpResponse == 403) {
                VelocityContext ctx = new VelocityContext();

                final OutputStream output = response.getOutputStream();
                final Writer writer = new OutputStreamWriter(output);
                final Template template = Velocity.getTemplate("403.vm");
                merge(ctx, writer, template);
            } else if (this.httpResponse == 415) {
                VelocityContext ctx = new VelocityContext();

                final OutputStream output = response.getOutputStream();
                final Writer writer = new OutputStreamWriter(output);
                final Template template = Velocity.getTemplate("415.vm");
                merge(ctx, writer, template);
            } else if (this.httpResponse == 404) {
                VelocityContext ctx = new VelocityContext();
                final OutputStream output = response.getOutputStream();
                final Writer writer = new OutputStreamWriter(output);
                final Template template = Velocity.getTemplate("404.vm");
                merge(ctx, writer, template);
            } else {
                if (null != contentType)
                    response.setContentType(contentType);
                response.sendError(httpResponse);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void merge(VelocityContext ctx, Writer writer, Template template) throws IOException {
        try {
            template.merge(ctx, writer);
            writer.flush();
        } catch (final EofException e) {
            //            log.warn("Browser window closed before page were ready for view.");
        }
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
