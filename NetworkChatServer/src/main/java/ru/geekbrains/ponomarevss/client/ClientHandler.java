package ru.geekbrains.ponomarevss.client;

import ru.geekbrains.ponomarevss.*;
import ru.geekbrains.ponomarevss.Command;
import ru.geekbrains.ponomarevss.Message;
import ru.geekbrains.ponomarevss.MyServer;
import ru.geekbrains.ponomarevss.message.AuthMessage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

public class ClientHandler {

    public static final int TIMEOUT = 120 * 1000;
    private MyServer myServer;

    private String clientName;

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    public ClientHandler(Socket socket, MyServer myServer) {
        try {
            this.socket = socket;
            this.myServer = myServer;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());

            Thread thread = new Thread(() -> {
                try {
                    authentication();
                    readMessages();
                } catch (IOException e) {
                    System.out.println("The connection is interrupted");
                    e.printStackTrace();
                } finally {
                    closeConnection();
                }
            });
            thread.setDaemon(true);
            thread.start();
        } catch (IOException e) {
            throw new RuntimeException("Failed to create client handler", e);
        }
    }

    private void readMessages() throws IOException {
        while (true) {
            String clientMessage = in.readUTF();
            System.out.println(String.format("Message '%s' from client %s%n", clientMessage, clientName));
            Message m = Message.fromJson(clientMessage);
            switch (m.command) {
                case PUBLIC_MESSAGE:
                    myServer.broadcastMessage(m, this);
                    break;
                case PRIVATE_MESSAGE:
                    myServer.sendPrivateMessage(m);
                    break;
                case END:
                    return;
            }
        }
    }

    private void closeConnection() {
        myServer.broadcastMessage(Message.createPublic(null,clientName + " is offline"));
        myServer.unsubscribe(this);
        try {
            socket.close();
        } catch (IOException e) {
            System.err.println("Failed to close socket!");
            e.printStackTrace();
        }
    }

    private void authentication() throws IOException {
        while (true) {
            Timer timeoutTimer = new Timer(true);
            timeoutTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        synchronized (this) {
                            if (clientName == null) {
                                System.out.println("authentication is terminated caused by timeout expired");
                                sendMessage(Message.createAuthError("authentication is terminated caused by timeout expired"));
                                Thread.sleep(100);
                                socket.close();
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }, TIMEOUT);
            String clientMessage = in.readUTF();
            synchronized (this) {
                Message message = Message.fromJson(clientMessage);
                if (message.command == Command.AUTH_MESSAGE) {
                    AuthMessage authMessage = message.authMessage;
                    String login = authMessage.login;
                    String password = authMessage.password;
                    String nick = myServer.getAuthService().getNickByLoginPass(login, password);
                    if (nick == null) {
                        sendMessage(Message.createAuthError("Incorrect login and/or password"));
                        continue;
                    }

                    if (myServer.isNickBusy(nick)) {
                        sendMessage(Message.createAuthError("This nick is already in use"));
                        continue;
                    }

                    clientName = nick;
                    sendMessage(Message.createAuthOk(clientName));
                    myServer.broadcastMessage(Message.createPublic(null,clientName + " is online"));
                    myServer.subscribe(this);
                    break;
                }
            }
        }
    }

    private void sendMessage(Message message) {
        sendMessage(message.toJson());
    }
    public void sendMessage(String message) {
        try {
            out.writeUTF(message);
        } catch (IOException e) {
            System.out.println("Failed to send message to user " + clientName + " : " + message);
            e.printStackTrace();
        }
    }

    public String getClientName() {
        return clientName;
    }

}
