package com.vg.web;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPOutputStream;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;

public class WebUtils {

    public static File tmpFile(File orig) {
        return new File(orig.getParentFile(), "." + randomAlphanumeric(8) + "." + orig.getName());
    }

    public static byte[] gzipString(String json) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        GZIPOutputStream gz = new GZIPOutputStream(out);

        gz.write(json.getBytes("UTF-8"));
        gz.flush();
        gz.close();

        out.flush();
        byte[] array = out.toByteArray();
        out.close();
        return array;
    }

    public static void rethrow(Throwable e) {
        if (e instanceof RuntimeException) {
            throw (RuntimeException) e;
        } else {
            throw new RuntimeException(e);
        }
    }
}
