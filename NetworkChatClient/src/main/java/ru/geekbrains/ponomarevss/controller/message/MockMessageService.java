package ru.geekbrains.ponomarevss.controller.message;

import javafx.scene.control.TextArea;
import ru.geekbrains.ponomarevss.Message;

public class MockMessageService implements IMessageService {

    private TextArea chatTextArea;

    public MockMessageService(TextArea chatTextArea) {
        this.chatTextArea = chatTextArea;
    }

    @Override
    public void sendMessage(Message message) {
        System.out.printf("Message %s has been sent%n", message.toJson());
        chatTextArea.appendText(message + System.lineSeparator());
    }

    @Override
    public void processRetrievedMessage(Message message) {
        throw new UnsupportedOperationException();
    }
}
