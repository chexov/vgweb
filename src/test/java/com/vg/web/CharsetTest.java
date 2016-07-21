package com.vg.web;

import static org.junit.Assert.assertEquals;

import javax.servlet.ServletOutputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.io.Files;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

public class CharsetTest {

    private HttpServer server;

    @Before
    public void setup() {
        server = new HttpServer(8043, Files.createTempDir());
    }

    @After
    public void teardown() {
        try {
            server.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testCharset() throws Exception {
        server.addGetMethod("/text", (r, p) -> (request, response) -> {
            response.setContentType("text/plain");
            ServletOutputStream outputStream = response.getOutputStream();
            outputStream.write("hello world".getBytes());
            outputStream.close();
        });
        server.start();
        Response execute = new OkHttpClient()
                .newCall(new Request.Builder().url("http://localhost:8043/text").build())
                .execute();
        System.out.println(execute.headers());
        assertEquals("text/plain", execute.headers().get("Content-Type"));
        assertEquals("hello world", execute.body().string());

    }

}
