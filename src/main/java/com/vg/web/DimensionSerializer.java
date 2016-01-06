package com.vg.web;

import java.awt.Dimension;
import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class DimensionSerializer implements JsonSerializer<Dimension>, JsonDeserializer<Dimension> {

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