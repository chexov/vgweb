package com.vg.web;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import org.stjs.javascript.Date;

public class JSDateAdapter implements JsonSerializer<Date>, JsonDeserializer<Date> {
    private final static String DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    private final static String JSDATE_TOSTRING_PATTERN = "EE MMM d y H:m:s 'GMT'Z (zz)";
    private final static String JSDATE_TOGMTSTRING_PATTERN = "EE, d MMM y H:m:s 'GMT'";
    
	@Override
	public Date deserialize(JsonElement elem, Type typeOfT, JsonDeserializationContext context)
			throws JsonParseException {
		if (elem == null) {
			return null;
		}
		String asString = elem.getAsString();
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(DATE_PATTERN);
            sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
            java.util.Date parse = sdf.parse(asString);
            return new Date(parse.getTime());
        } catch (ParseException e) {
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(JSDATE_TOSTRING_PATTERN);
            java.util.Date parse = sdf.parse(asString);
            return new Date(parse.getTime());
        } catch (ParseException e) {
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat(JSDATE_TOGMTSTRING_PATTERN);
            sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
            java.util.Date parse = sdf.parse(asString);
            return new Date(parse.getTime());
        } catch (ParseException e) {
        }

		return new Date(asString);
	}

	@Override
	public JsonElement serialize(Date src, Type typeOfSrc, JsonSerializationContext context) {
		if (src == null) {
			return new JsonNull();
		}
		SimpleDateFormat sdf = new SimpleDateFormat(DATE_PATTERN);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        return new JsonPrimitive(sdf.format(new java.util.Date((long) src.getTime())));
	}

}
