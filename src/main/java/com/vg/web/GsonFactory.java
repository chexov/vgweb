package com.vg.web;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Currency;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.stjs.javascript.Array;
import org.stjs.server.json.gson.JSArrayAdapter;
import org.stjs.server.json.gson.JSDateAdapter;
import org.stjs.server.json.gson.JSMapAdapter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class GsonFactory {

    private static final Map<java.lang.reflect.Type, Object> adapters = new LinkedHashMap<>();

    public static void registerTypeAdapter(java.lang.reflect.Type type, Object typeAdapter) {
        adapters.put(type, typeAdapter);
    }

    static {
        registerTypeAdapter(File.class, FileDeserializer.INSTANCE);
        registerTypeAdapter(Dimension.class, new DimensionSerializer());
        registerTypeAdapter(Currency.class, new CurrencySerializer());
        registerTypeAdapter(org.stjs.javascript.Map.class, new JSMapAdapter());
        registerTypeAdapter(org.stjs.javascript.Array.class, new JSArrayAdapter());
        registerTypeAdapter(org.stjs.javascript.Date.class, new JSDateAdapter());
    }

    private static Gson create(boolean serializeNulls) {
        GsonBuilder builder = new GsonBuilder();
        adapters.forEach(builder::registerTypeAdapter);
        builder.setPrettyPrinting();
        if (serializeNulls)
            builder.serializeNulls();
        return builder.create();
    }

    private static Gson createNoPretty(boolean serializeNulls) {
        GsonBuilder builder = new GsonBuilder();
        adapters.forEach((t, u) -> {
            builder.registerTypeAdapter(t, u);
        });
        if (serializeNulls)
            builder.serializeNulls();
        return builder.create();
    }

    private static GsonHelper helper = null;

    public static <T> T gsonClone(T t) {
        return helper().gsonClone(t);
    }

    private static GsonHelper helper() {
        if (helper != null) {
            return helper;
        }
        return helper = new GsonHelper(create(false), createNoPretty(false));
    }

    public static String toGson(Object src) {
        return helper().toGson(src);
    }

    public static <T> T fromFile(File file, Class<T> classOf) {
        return helper().fromFile(file, classOf);
    }

    public static <T> T fromInputStream(InputStream in, Class<T> classOf) {
        return helper().fromInputStream(in, classOf);
    }

    public static <T> T fromFile(File file, java.lang.reflect.Type t) {
        return helper().fromFile(file, t);
    }

    public static String gsonToString(Object o) {
        return helper().gsonToString(o);
    }

    public static <T> T fromRequest(HttpServletRequest request, Class<T> typeOfT) throws Exception {
        return helper().fromRequest(request, typeOfT);
    }

    public static <T> T fromJson(String json, Class<T> typeOfT) {
        return helper().fromJson(json, typeOfT);
    }

    public static <T> T fromJsonQuietly(String json, Class<T> type) {
        return helper().fromJsonQuietly(json, type);
    }

    public static boolean toFileAtomic(Object data, File file) throws IOException {
        return helper().toFileAtomic(data, file);
    }

}