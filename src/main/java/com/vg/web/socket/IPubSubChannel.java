package com.vg.web.socket;

public interface IPubSubChannel {

    void subscribe(PubSubUpdateListener listener);

    void unsubscribe(PubSubUpdateListener listener);

    void publish(String message);

}