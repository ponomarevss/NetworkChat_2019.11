package ru.geekbrains.ponomarevss.controller.message;

public class ServerConnectionException extends RuntimeException {

    public ServerConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
