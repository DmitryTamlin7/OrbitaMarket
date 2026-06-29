package org.example.component;

public interface MessageBrokerClient {
    void send(String topic, String payload);
}
