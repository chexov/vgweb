package com.vg.web;

import java.io.File;
import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;

public final class FileDeserializer implements JsonDeserializer<File> {
    public static final FileDeserializer INSTANCE = new FileDeserializer();

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