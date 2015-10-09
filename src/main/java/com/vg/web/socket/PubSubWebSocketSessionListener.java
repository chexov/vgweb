package com.vg.web.socket;

import org.eclipse.jetty.websocket.common.WebSocketSession;

import java.util.function.Consumer;

public class PubSubWebSocketSessionListener implements PubSubUpdateListener {

    private final WebSocketSession session;
    private final Consumer<String> _onMessage;

    public PubSubWebSocketSessionListener(WebSocketSession session, Consumer<String> onMessage) {
        this.session = session;
        this._onMessage = onMessage;
    }

    public WebSocketSession getSession() {
        return session;
    }

    @Override
    public void onMessage(String message) {
        _onMessage.accept(message);
    }
}
