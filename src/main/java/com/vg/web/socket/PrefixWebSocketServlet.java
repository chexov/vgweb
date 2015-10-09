package com.vg.web.socket;

import static com.vg.web.StrParser.pathInfoParser;

import javax.servlet.Servlet;

import com.vg.web.StrParser;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.eclipse.jetty.websocket.common.WebSocketSession;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

public class PrefixWebSocketServlet extends WebSocketServlet implements Servlet {

    public String prefixPath;

    public PrefixWebSocketServlet(String path) {
        this.prefixPath = path;
    }

    public void onConnect(WebSocketSession session, StrParser pathInfo) {

    }

    public void onClose(WebSocketSession session, int statusCode, String reason) {

    }

    public void onText(WebSocketSession session, StrParser pathInfo, String message) {

    }

    public void onError(Throwable cause) {

    }

    @Override
    public void configure(WebSocketServletFactory factory) {
        WebSocketCreator wsc = (req, resp) -> new WebSocketAdapter() {
            String fullURI = "";

            public String getPath() {
                return fullURI.replace(prefixPath.replace("*", ""), "");
            }

            @Override
            public void onWebSocketBinary(byte[] payload, int offset, int len) {
                super.onWebSocketBinary(payload, offset, len);
            }

            @Override
            public void onWebSocketClose(int statusCode, String reason) {
                onClose((WebSocketSession) super.getSession(), statusCode, reason);
                super.onWebSocketClose(statusCode, reason);
            }

            @Override
            public void onWebSocketConnect(Session session) {
                super.onWebSocketConnect(session);
                WebSocketSession wsession = (WebSocketSession) session;
                fullURI = wsession.getRequestURI().getPath();
                onConnect(wsession, pathInfoParser(getPath()));
            }

            @Override
            public void onWebSocketError(Throwable cause) {
                super.onWebSocketError(cause);
                onError(cause);
            }

            @Override
            public void onWebSocketText(String message) {
                super.onWebSocketText(message);
                WebSocketSession wsession = (WebSocketSession) getSession();
                onText(wsession, pathInfoParser(getPath()), message);
            }
        };

        factory.setCreator(wsc);
    }

}
