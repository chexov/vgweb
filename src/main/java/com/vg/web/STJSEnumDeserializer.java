package com.vg.web;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

public final class STJSEnumDeserializer implements JsonDeserializer<Enum> {
    @Override
    public Enum deserialize(JsonElement json, Type type, JsonDeserializationContext context)
            throws JsonParseException {
        String enumStringValue;
        if (json.isJsonPrimitive()) {
            enumStringValue = json.getAsString();
        } else {
            JsonObject obj = json.getAsJsonObject();
            JsonElement jsonElement = obj.get("_name");
            if (jsonElement == null) {
                jsonElement = obj.get("name");
            }
            if (jsonElement == null) {
                return null;
            }
            enumStringValue = jsonElement.getAsString();
        }
        try {
            Method declaredMethod = ((Class) type).getDeclaredMethod("valueOf", String.class);
            Object invoke = declaredMethod.invoke(null, enumStringValue);
            return (Enum) invoke;
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }
}