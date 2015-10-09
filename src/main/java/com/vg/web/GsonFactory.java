package com.vg.web;

import static org.apache.commons.io.FileUtils.writeByteArrayToFile;
import static org.apache.commons.io.FileUtils.writeStringToFile;
import static org.apache.commons.io.IOUtils.closeQuietly;

import java.awt.Dimension;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.zip.GZIPInputStream;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class GsonFactory {

    private static final FileDeserializer FILE_DESERIALIZER = new FileDeserializer();
    private static final Gson GSON_TOSTRING = new GsonBuilder().create();
    private static final Gson GSON = create(false);
    private static final Gson GSON_NOPRETTY = createNoPretty(false);

    private static final class FileDeserializer implements JsonDeserializer<File> {
        @Override
        public File deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            JsonPrimitive primitive = null;
            if (json instanceof JsonObject) {
                primitive = ((JsonObject) json).getAsJsonPrimitive("path");
            } else if (json instanceof JsonPrimitive) {
                primitive = (JsonPrimitive) json;
            }
            if (primitive != null) {
                String asString = primitive.getAsString();
                if (asString != null) {
                    return new File(asString);
                }
            }

            return null;
        }
    }

    private static class DimensionSerializer implements JsonSerializer<Dimension>, JsonDeserializer<Dimension> {

        @Override
        public JsonElement serialize(Dimension src, Type typeOfSrc, JsonSerializationContext context) {
            if (src == null)
                return null;
            JsonObject o = new JsonObject();
            o.add("width", new JsonPrimitive(src.width));
            o.add("height", new JsonPrimitive(src.height));
            return o;
        }

        @Override
        public Dimension deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            if (json == null)
                return null;
            JsonObject o = json.getAsJsonObject();
            int w = o.get("width").getAsInt();
            int h = o.get("height").getAsInt();
            return new Dimension(w, h);
        }

    }

    public static <T> T gsonClone(T t) {
        String json = GSON_TOSTRING.toJson(t);
        T fromJson = (T) GSON_TOSTRING.fromJson(json, t.getClass());
        return fromJson;
    }

    private static Gson create(boolean serializeNulls) {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(File.class, FILE_DESERIALIZER);
        builder.registerTypeAdapter(Dimension.class, new DimensionSerializer());
        builder.setPrettyPrinting();
        if (serializeNulls)
            builder.serializeNulls();
        return builder.create();
    }

    private static Gson createNoPretty(boolean serializeNulls) {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(File.class, FILE_DESERIALIZER);
        builder.registerTypeAdapter(Dimension.class, new DimensionSerializer());
        if (serializeNulls)
            builder.serializeNulls();
        return builder.create();
    }

    public static String toGson(Object src) {
        return GSON.toJson(src);
    }

    public static String toGsonNoPretty(Object src) {
        return GSON_NOPRETTY.toJson(src);
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
            IOUtils.closeQuietly(is);
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
            IOUtils.closeQuietly(is);
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

    public static boolean toFileAtomic(Object data, File file) throws IOException {
        String json = toGson(data);

        if (file.getName().endsWith(".gz")) {
            return atomicWriteByteToFile(Utils.gzipString(json), file);
        } else {
            return atomicWriteStringToFile(json, file);
        }
    }

    private static boolean atomicWriteByteToFile(byte[] bytes, File file) throws IOException {
        File tmp = FileUtil.tmpFile(file);
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
        File tmp = FileUtil.tmpFile(file);
        FileUtils.writeStringToFile(tmp, data);
        return tmp.renameTo(file);
    }

    @Test
    public void testFromJsonQuietly() throws Exception {
        Integer[] fromJsonQuietly = fromJsonQuietly("qwe", Integer[].class);
        System.out.println(fromJsonQuietly);
    }

}