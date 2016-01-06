package com.vg.web;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Currency;

import javax.servlet.http.HttpServletRequest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class GsonFactory {

    private static final Gson GSON_TOSTRING = createNoPretty(false);
    private static final Gson GSON = create(false);

    private static Gson create(boolean serializeNulls) {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(File.class, FileDeserializer.INSTANCE);
        builder.registerTypeAdapter(Dimension.class, new DimensionSerializer());
        builder.registerTypeAdapter(Currency.class, new CurrencySerializer());
        builder.setPrettyPrinting();
        if (serializeNulls)
            builder.serializeNulls();
        return builder.create();
    }

    private static Gson createNoPretty(boolean serializeNulls) {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(File.class, FileDeserializer.INSTANCE);
        builder.registerTypeAdapter(Currency.class, new CurrencySerializer());
        if (serializeNulls)
            builder.serializeNulls();
        return builder.create();
    }

    private static final GsonHelper helper = new GsonHelper(GSON, GSON_TOSTRING);

    public static <T> T gsonClone(T t) {
        return helper.gsonClone(t);
    }

    public static String toGson(Object src) {
        return helper.toGson(src);
    }

    public static <T> T fromFile(File file, Class<T> classOf) {
        return helper.fromFile(file, classOf);
    }

    public static <T> T fromInputStream(InputStream in, Class<T> classOf) {
        return helper.fromInputStream(in, classOf);
    }

    public static <T> T fromFile(File file, java.lang.reflect.Type t) {
        return helper.fromFile(file, t);
    }

    public static String gsonToString(Object o) {
        return helper.gsonToString(o);
    }

    public static <T> T fromRequest(HttpServletRequest request, Class<T> typeOfT) throws Exception {
        return helper.fromRequest(request, typeOfT);
    }

    public static <T> T fromJson(String json, Class<T> typeOfT) {
        return helper.fromJson(json, typeOfT);
    }

    public static <T> T fromJsonQuietly(String json, Class<T> type) {
        return helper.fromJsonQuietly(json, type);
    }

    public static boolean toFileAtomic(Object data, File file) throws IOException {
        return helper.toFileAtomic(data, file);
    }

}