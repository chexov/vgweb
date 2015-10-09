package com.vg.web.view;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.vg.web.Context;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.eclipse.jetty.io.EofException;

public class VelocityView implements View {
    //    private static Logger log = LogManager.getLogger(VelocityView.class);

    public static final String CONTENT_TYPE_TEXT_HTML = "text/html";

    public static final String UTF_8 = "UTF-8";

    private final VelocityContext context;
    private final String pageTemplate;

    public VelocityView(final VelocityContext context, final String pageTemplate) {
        this.context = context;
        this.pageTemplate = pageTemplate;
    }

    public void view(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        response.setContentType(CONTENT_TYPE_TEXT_HTML);
        response.setCharacterEncoding(UTF_8);
        final OutputStream output = response.getOutputStream();
        final Writer writer = new OutputStreamWriter(output);
        try {
            merge(writer);
            writer.flush();
        } catch (final EofException e) {
            //            log.warn("Browser window closed before page were ready for view.");
        }
    }

    private void merge(final Writer writer) throws IOException {
        try {
            final Template template = Velocity.getTemplate(pageTemplate);
            template.merge(context, writer);
        } catch (final Exception e) {
            throw new IOException(e);
        }
    }

    public void put(final String key, final Object value) {
        context.put(key, value);
    }

    public Object get(final String key) {
        return context.get(key);
    }

    public static VelocityView create(final String template) {
        return new VelocityView(new VelocityContext(), template);
    }

    public static View create(Context ctxt, String tmpl) {
        return new VelocityView(ctxt, tmpl);
    }
}
