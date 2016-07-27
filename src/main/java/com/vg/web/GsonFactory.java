package com.vg.web;

import static org.apache.commons.io.IOUtils.closeQuietly;

import javax.servlet.http.HttpServletRequest;
import java.awt.Dimension;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.function.Consumer;
import java.util.zip.GZIPInputStream;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.stjs.server.json.gson.JSArrayAdapter;
import org.stjs.server.json.gson.JSDateAdapter;
import org.stjs.server.json.gson.JSMapAdapter;

public class GsonFactory {
    private static final FileDeserializer FILE_DESERIALIZER = new FileDeserializer();

    private static final Gson GSON_TOSTRING = builder(false).create();
    private static final Gson GSON = builder(false).create();

    public static <T> T gsonClone(T t) {
        String json = GSON_TOSTRING.toJson(t);
        T fromJson = (T) GSON_TOSTRING.fromJson(json, t.getClass());
        return fromJson;
    }

    public static GsonBuilder builder(boolean serializeNulls) {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(File.class, FILE_DESERIALIZER);
        builder.registerTypeAdapter(Dimension.class, new DimensionSerializer());
        builder.registerTypeAdapter(Enum.class, new STJSEnumDeserializer());
        builder.registerTypeAdapter(org.stjs.javascript.Map.class, new JSMapAdapter());
        builder.registerTypeAdapter(org.stjs.javascript.Array.class, new JSArrayAdapter());
        builder.registerTypeAdapter(org.stjs.javascript.Date.class, new JSDateAdapter());
        if (serializeNulls)
            builder.serializeNulls();
        return builder;
    }

    public static String toGson(Object src) {
        return GSON.toJson(src);
    }

    public static <T> T fromFile(File file, Class<T> classOf) {
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
            closeQuietly(is);
        }
    }

    public static <T> T fromFile(File file, Class<T> classOf, Consumer<Throwable> onError) {
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

    public static <T> T fromInputStream(InputStream in, Class<T> classOf) {
        return GSON.fromJson(new InputStreamReader(in), classOf);
    }

    public static <T> T fromFile(File file, java.lang.reflect.Type t) {
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
            closeQuietly(is);
        }
    }

    public static String gsonToString(Object o) {
        return GSON_TOSTRING.toJson(o);
    }

    public static <T> T fromRequest(HttpServletRequest request, Class<T> typeOfT) throws Exception {
        return GSON.fromJson(IOUtils.toString(request.getInputStream()), typeOfT);
    }

    public static <T> T fromJson(String json, Class<T> typeOfT) {
        return GSON.fromJson(json, typeOfT);
    }

    public static <T> T fromJsonQuietly(String json, Class<T> type) {
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


    @Test
    public void testFromJsonQuietly() throws Exception {
        Integer[] fromJsonQuietly = fromJsonQuietly("qwe", Integer[].class);
        System.out.println(fromJsonQuietly);
    }

}