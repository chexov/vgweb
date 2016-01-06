package com.vg.web;

import java.lang.reflect.Type;
import java.util.Currency;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class CurrencySerializer implements JsonSerializer<Currency>, JsonDeserializer<Currency> {

    @Override
    public JsonElement serialize(Currency src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(src.toString());
    }

    @Override
    public Currency deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        return Currency.getInstance(json.getAsJsonPrimitive().getAsString());
    }
}
