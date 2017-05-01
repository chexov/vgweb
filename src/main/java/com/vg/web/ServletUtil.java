package com.vg.web;

import static org.apache.commons.lang3.StringUtils.defaultString;

import java.io.File;

import javax.servlet.http.HttpServletRequest;

import com.vg.web.view.CachedFileView;
import com.vg.web.view.ContentRangeFileView;
import com.vg.web.view.ErrorView;
import com.vg.web.view.View;

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
		boolean clientReset = "Broken pipe".equals(message) || "Connection reset by peer".equals(message) || (e instanceof org.eclipse.jetty.io.EofException);
		if (clientReset) {
            System.err.println("ignoreClientReset " + e);
		} else {
			e.printStackTrace();
		}
	}

	public static View getFileView(HttpServletRequest request, File f) {
		String range = request.getHeader("Range");
		if (f == null || !f.exists()) {
			return new ErrorView(404);
		}

		if (range == null) {
			return new CachedFileView(f);
		} else {
			return new ContentRangeFileView(f, ContentRange.parseRange(range, f.length()));
		}
	}

}
