package com.vg.web;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;

import com.vg.web.socket.PrefixWebSocketServlet;
import com.vg.web.view.View;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

public class VirtualHost {
    protected ServletContextHandler context;

    public VirtualHost(ServletContextHandler context) {
        this.context = context;
    }

    public void add(String path, HttpServlet servlet) {
        context.addServlet(new ServletHolder(servlet), path);
    }

    public void add(PrefixWebSocketServlet webSocketController) {
        add(webSocketController.prefixPath, webSocketController);
    }

    public void addGetMethod(String path, Get servlet) {
        Controller controller = new Controller() {
            public View get(HttpServletRequest request, StrParser pathInfo) throws Exception {
                return servlet.get(request, pathInfo);
            }
        };
        context.addServlet(new ServletHolder(controller), path);
    }
}
