package com.vg.web;

import static org.apache.commons.lang.StringUtils.defaultString;

public class ServletUtil {

    /**
     * Ignores exceptions caused by client reset. Prints all other exceptions to
     * stdout.
     * 
     * @param e
     */
    public static void ignoreClientReset(Throwable e) {
        Throwable cause = e.getCause();
        String message = cause != null ? defaultString(cause.getMessage()) : "";
        boolean clientReset = "Broken pipe".equals(message) || "Connection reset by peer".equals(message);
        if (!clientReset) {
            e.printStackTrace();
        }
    }

}
