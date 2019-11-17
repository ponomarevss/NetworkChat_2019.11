package ru.geekbrains.ponomarevss.controller.message;

import javafx.scene.control.TextArea;
import ru.geekbrains.ponomarevss.Message;
import ru.geekbrains.ponomarevss.message.PrivateMessage;
import ru.geekbrains.ponomarevss.message.PublicMessage;
import ru.geekbrains.ponomarevss.controller.Network;
import ru.geekbrains.ponomarevss.controller.PrimaryController;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

public class ServerMessageService implements IMessageService {

    private static final String HOST_ADDRESS_PROP = "server.address";
    private static final String HOST_PORT_PROP = "server.port";

    private String hostAddress;
    private int hostPort;

    private final TextArea chatTextArea;
    private PrimaryController primaryController;
    private boolean needStopServerOnClosed;
    private Network network;

    public ServerMessageService(PrimaryController primaryController, boolean needStopServerOnClosed) {
        this.chatTextArea = primaryController.chatTextArea;
        this.primaryController = primaryController;
        this.needStopServerOnClosed = needStopServerOnClosed;
        initialize();
    }

    private void initialize() {
        readProperties();
        startConnectionToServer();
    }

    private void startConnectionToServer() {
        try {
            this.network = new Network(hostAddress, hostPort, this);
        } catch (IOException e) {
            throw new ServerConnectionException("Failed to connect to server", e);
        }
    }

    private void readProperties() {
        Properties serverProperties = new Properties();
        try (InputStream inputStream = getClass().getResourceAsStream("/application.properties")) {
            serverProperties.load(inputStream);
            hostAddress = serverProperties.getProperty(HOST_ADDRESS_PROP);
            hostPort = Integer.parseInt(serverProperties.getProperty(HOST_PORT_PROP));
        } catch (IOException e) {
            throw new RuntimeException("Failed to read application.properties file", e);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid port value", e);
        }
    }

    @Override
    public void sendMessage(Message message) {
        network.send(message.toJson());
    }

    @Override
    public void processRetrievedMessage(Message message) {
        switch (message.command) {
            case AUTH_OK: {
                processAuthOk(message);
                break;
            }
            case PRIVATE_MESSAGE: {
                processPrivateMessage(message);
                break;
            }
            case PUBLIC_MESSAGE: {
                processPublicMessage(message);
                break;
            }
            case AUTH_ERROR: {
                primaryController.showAuthError(message.authErrorMessage.errorMsg);
                break;
            }
            case CLIENT_LIST: {
                List<String> onlineUserNicknames = message.clientListMessage.online;
                primaryController.refreshUsersList(onlineUserNicknames);
                break;
            }
            default:
                throw new IllegalArgumentException("Unknown command type: " + message.command);
        }
    }

    private void processPublicMessage(Message message) {
        PublicMessage publicMessage = message.publicMessage;
        String from = publicMessage.from;
        String msg = publicMessage.message;
        if (from != null) {
            chatTextArea.appendText(String.format("%s: %s%n", from, msg));
        } else {
            chatTextArea.appendText(String.format("%s%n", msg));
        }
    }

    private void processPrivateMessage(Message message) {
        PrivateMessage privateMessage = message.privateMessage;
        String from = privateMessage.from;
        String msg = privateMessage.message;
        String msgToView = String.format("%s (private): %s%n", from, msg);
        chatTextArea.appendText(msgToView);
    }

    private void processAuthOk(Message message) {
        primaryController.setNickName(message.authOkMessage.nickname);
        primaryController.showChatPanel();
    }

    @Override
    public void close() throws IOException {
        if (needStopServerOnClosed) {
            sendMessage(Message.serverEndMessage());
        }
        network.close();
    }


}
