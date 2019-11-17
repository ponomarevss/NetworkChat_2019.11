package ru.geekbrains.ponomarevss.controller.message;

import ru.geekbrains.ponomarevss.Message;

import java.io.Closeable;
import java.io.IOException;

public interface IMessageService extends Closeable {

    void sendMessage(Message message);

    void processRetrievedMessage(Message message);

    @Override
    default void close() throws IOException {
    }
}
