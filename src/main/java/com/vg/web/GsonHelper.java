package com.vg.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import com.google.gson.Gson;
import com.vg.web.util.WebUtils;

public class GsonHelper {

    private Gson GSON_TOSTRING;
    private Gson GSON;

    public GsonHelper(Gson pretty, Gson ugly) {
        this.GSON = pretty;
        this.GSON_TOSTRING = ugly;
    }

    public <T> T gsonClone(T t) {
        String json = GSON_TOSTRING.toJson(t);
        T fromJson = (T) GSON_TOSTRING.fromJson(json, t.getClass());
        return fromJson;
    }

    public String toGson(Object src) {
        return GSON.toJson(src);
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

    public String gsonToString(Object o) {
        return GSON_TOSTRING.toJson(o);
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
            fromJson = GsonFactory.fromJson(json, type);
        } catch (Exception e) {
            e.printStackTrace();
            try {
                fromJson = GsonFactory.fromJson(type.isArray() ? "[]" : "{}", type);
            } catch (Exception e1) {
                throw new RuntimeException(e);
            }

        }
        return fromJson;
    }

    public boolean toFileAtomic(Object data, File file) throws IOException {
        String json = toGson(data);

        if (file.getName().endsWith(".gz")) {
            return atomicWriteByteToFile(WebUtils.gzipString(json), file);
        } else {
            return atomicWriteStringToFile(json, file);
        }
    }

    private boolean atomicWriteByteToFile(byte[] bytes, File file) throws IOException {
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
    public boolean atomicWriteStringToFile(String data, File file) throws IOException {
        File tmp = WebUtils.tmpFile(file);
        FileUtils.writeStringToFile(tmp, data);
        return tmp.renameTo(file);
    }

    @Test
    public void testFromJsonQuietly() throws Exception {
        Integer[] fromJsonQuietly = fromJsonQuietly("qwe", Integer[].class);
        System.out.println(fromJsonQuietly);
    }

}