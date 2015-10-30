package com.vg.web;

import static java.lang.Double.parseDouble;
import static java.lang.Integer.parseInt;
import static java.lang.Long.parseLong;

import java.util.Arrays;
import java.util.Iterator;

public class StrParser {
    private Iterator<String> iterator;

    public StrParser(String string, String splitRegex) {
        this(string.split(splitRegex));
    }

    public StrParser(String[] tokens) {
        iterator = Arrays.asList(tokens).iterator();
    }

    public int nextInt() {
        return nextInt(0);
    }

    public int nextInt(int defaultInt) {
        return iterator.hasNext() ? parseInt(iterator.next()) : defaultInt;
    }

    public long nextLong() {
        return iterator.hasNext() ? parseLong(iterator.next()) : 0;
    }

    public long nextLong(long defaultValue) {
        return iterator.hasNext() ? parseLong(iterator.next()) : defaultValue;
    }

    public double nextDouble() {
        return iterator.hasNext() ? parseDouble(iterator.next()) : 0.;
    }

    public String nextString() {
        return nextString("");
    }

    public String nextString(String defaultString) {
        return iterator.hasNext() ? iterator.next() : defaultString;
    }

    public <E extends Enum<E>> E nextEnum(Class<E> c, E defaultE) {
        String nextString = nextString().replaceAll("\"", "");
        try {
            return nextString.isEmpty() ? defaultE : Enum.valueOf(c, nextString);
        } catch (Exception e) {
            return defaultE;
        }
    }

    public static StrParser pathInfoParser(String pathInfo) {
        if (pathInfo == null)
            pathInfo = "";
        if (pathInfo.startsWith("/")) {
            pathInfo = pathInfo.substring(1);
        }
        return new StrParser(pathInfo, "/");
    }

    public float parseFloat(float defaultValue) {
        return iterator.hasNext() ? Float.parseFloat(iterator.next()) : defaultValue;
    }

    public boolean nextBoolean() {
        return nextBoolean(false);
    }

    public boolean nextBoolean(boolean defaultValue) {
        return iterator.hasNext() ? Boolean.parseBoolean(iterator.next()) : defaultValue;
    }

}
