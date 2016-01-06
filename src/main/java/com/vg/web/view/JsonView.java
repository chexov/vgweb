package com.vg.web.view;

import com.vg.web.GsonFactory;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class JsonView implements View {

    private final Object obj;
    private int httpResponse;

    public JsonView(int httpResponse, Object obj) {
        this.httpResponse = httpResponse;
        this.obj = obj;
    }

    public JsonView(Object obj) {
        this.httpResponse = 200;
        this.obj = obj;
    }

    public void view(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setHeader("Content-Type", "application/json");
        response.setStatus(this.httpResponse);

        OutputStream os = response.getOutputStream();
        os.write(GsonFactory.toGson(this.obj).getBytes("UTF-8"));
        os.flush();
        os.close();
    }

    public Object getObject() {
        return obj;
    }
    
    public static View prettyJson(Object obj) {
        return new JsonView(obj);
    }

}
