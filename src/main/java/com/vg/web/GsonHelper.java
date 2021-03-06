package com.vg.web;

import static org.apache.commons.io.IOUtils.closeQuietly;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.function.Consumer;
import java.util.zip.GZIPInputStream;

import com.google.gson.Gson;
import com.vg.web.util.WebUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

public class GsonHelper {

    private Gson GSON_PRETTY;
    private Gson GSON;

    public GsonHelper(Gson pretty, Gson compact) {
        this.GSON_PRETTY = pretty;
        this.GSON = compact;
    }

    public <T> T gsonClone(T t) {
        String json = GSON.toJson(t);
        T fromJson = (T) GSON.fromJson(json, t.getClass());
        return fromJson;
    }

    public String toGson(Object src) {
        return GSON.toJson(src);
    }

    public String toGsonPretty(Object src) {
        return GSON_PRETTY.toJson(src);
    }

    public <T> T fromFile(File file, Class<T> classOf) {
        InputStream is = null;
        try {
            is = new FileInputStream(file);
            if (file.getName().endsWith(".gz")) {
                is = new GZIPInputStream(is);
            }
            return GSON.fromJson(IOUtils.toString(is), classOf);
        } catch (Exception e) {
            System.err.println("can not read gson from file " + file.getAbsolutePath());
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    public <T> T fromInputStream(InputStream in, Class<T> classOf) {
        return GSON.fromJson(new InputStreamReader(in), classOf);
    }

    public <T> T fromFile(File file, java.lang.reflect.Type t) {
        InputStream is = null;
        try {
            is = new FileInputStream(file);
            if (file.getName().endsWith(".gz")) {
                is = new GZIPInputStream(is);
            }
            return GSON.fromJson(IOUtils.toString(is), t);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    public <T> T fromFile(File file, Class<T> classOf, Consumer<Throwable> onError) {
        InputStream is = null;
        try {
            is = new FileInputStream(file);
            if (file.getName().endsWith(".gz")) {
                is = new GZIPInputStream(is);
            }
            return GSON.fromJson(IOUtils.toString(is), classOf);
        } catch (Throwable e) {
            if (onError != null) {
                onError.accept(e);
            }
            return null;
        } finally {
            closeQuietly(is);
        }
    }

    public String gsonToString(Object o) {
        return GSON.toJson(o);
    }

    public <T> T fromRequest(HttpServletRequest request, Class<T> typeOfT) throws Exception {
        return GSON.fromJson(IOUtils.toString(request.getInputStream()), typeOfT);
    }

    public <T> T fromJson(String json, Class<T> typeOfT) {
        return GSON.fromJson(json, typeOfT);
    }

    public <T> T fromJsonQuietly(String json, Class<T> type) {
        T fromJson = null;
        json = StringUtils.defaultIfBlank(json, type.isArray() ? "[]" : "{}");
        try {
            fromJson = GSON.fromJson(json, type);
        } catch (Exception e) {
            e.printStackTrace();
            try {
                fromJson = GSON.fromJson(type.isArray() ? "[]" : "{}", type);
            } catch (Exception e1) {
                throw new RuntimeException(e);
            }

        }
        return fromJson;
    }


    private static boolean atomicWriteByteToFile(byte[] bytes, File file) throws IOException {
        File tmp = WebUtils.tmpFile(file);
        FileUtils.writeByteArrayToFile(tmp, bytes);
        return tmp.renameTo(file);
    }

    /**
     * writes string to temp file, renames temp file to original
     *
     * @param data
     * @param file
     * @return if rename was successful
     * @throws IOException
     */
    public static boolean atomicWriteStringToFile(String data, File file) throws IOException {
        File tmp = WebUtils.tmpFile(file);
        FileUtils.writeStringToFile(tmp, data);
        return tmp.renameTo(file);
    }

    @Test
    public void testFromJsonQuietly() throws Exception {
        Integer[] fromJsonQuietly = fromJsonQuietly("qwe", Integer[].class);
        System.out.println(fromJsonQuietly);
    }

    public boolean toFileAtomic(Object data, File file) throws IOException {
        String json = toGson(data);

        if (file.getName().endsWith(".gz")) {
            return atomicWriteByteToFile(WebUtils.gzipString(json), file);
        } else {
            return atomicWriteStringToFile(json, file);
        }
    }

}