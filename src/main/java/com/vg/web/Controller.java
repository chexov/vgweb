package com.vg.web;

import com.vg.web.view.ErrorView;
import com.vg.web.view.View;

import static com.vg.web.StrParser.pathInfoParser;

import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeoutException;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Controller extends HttpServlet implements Servlet {
    private static final long serialVersionUID = 1L;
    //    private final static Logger log = LogManager.getLogger(Controller.class);

    public View get(HttpServletRequest request, StrParser pathInfo) throws Exception {
        return ErrorView.MethodNotAllowed;
    }

    public View post(HttpServletRequest request) throws Exception {
        return post(request, StrParser.pathInfoParser(request.getPathInfo()));
    }

    public View post(HttpServletRequest request, StrParser pathInfo) throws Exception {
        return ErrorView.MethodNotAllowed;
    }

    public View put(HttpServletRequest request, StrParser pathInfo) throws Exception {
        return ErrorView.MethodNotAllowed;
    }

    public View delete(HttpServletRequest request, StrParser pathInfo) throws Exception {
        return ErrorView.MethodNotAllowed;
    }

    @Override
    protected final void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            View view = get(request, pathInfoParser(request.getPathInfo()));
            if (view != null) {
                view.view(request, response);
            }
        } catch (Exception e) {
            handleException(e);
        }
    }

    @Override
    protected final void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            View view = post(request);
            if (view != null) {
                view.view(request, response);
            }
        } catch (Exception e) {
            handleException(e);
        }
    }

    private void handleException(Exception e) throws ServletException {
        if (!closeOrReset(e)) {
            e.printStackTrace();
            //            log.error("unhandled exception", e);
            throw new ServletException(e);
        } else {
            e.printStackTrace();
            //            log.debug(String.valueOf(e));
        }
    }

    private boolean closeOrReset(Throwable t) {
        if (t == null)
            return false;
        return t instanceof java.nio.channels.ClosedChannelException || t instanceof TimeoutException
                || t instanceof SocketTimeoutException || t instanceof SocketException
                || t instanceof java.io.InterruptedIOException || t instanceof org.eclipse.jetty.io.EofException;
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            View view = delete(request, pathInfoParser(request.getPathInfo()));
            if (view != null) {
                view.view(request, response);
            }
        } catch (Exception e) {
            handleException(e);
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            View view = put(request, pathInfoParser(request.getPathInfo()));
            if (view != null) {
                view.view(request, response);
            }
        } catch (Exception e) {
            handleException(e);
        }
    }
}
