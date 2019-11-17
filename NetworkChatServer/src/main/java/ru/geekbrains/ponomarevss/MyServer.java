package ru.geekbrains.ponomarevss;

import ru.geekbrains.ponomarevss.auth.AuthService;
import ru.geekbrains.ponomarevss.auth.BaseAuthService;
import ru.geekbrains.ponomarevss.client.ClientHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MyServer {

    private static final int PORT = 8189;

    private final AuthService authService= new BaseAuthService();

    private List<ClientHandler> clients = new ArrayList<>();

    private ServerSocket serverSocket = null;

     MyServer() {
        System.out.println("Server is running");
        try {
            serverSocket = new ServerSocket(PORT);
            authService.start();
            while (true) {
                System.out.println("Awaiting client connection ...");
                Socket socket = serverSocket.accept();
                System.out.println("Client has connected");
                new ClientHandler(socket, this);
            }

        } catch (IOException e) {
            System.err.println("Server running error. Cause: " + e.getMessage());
            e.printStackTrace();
        } finally {
            authService.stop();
        }
    }

    private void shutdownServer() {
        try {
            authService.stop();
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void subscribe(ClientHandler clientHandler) {
        clients.add(clientHandler);
        broadcastClientList();
    }

    public synchronized void unsubscribe(ClientHandler clientHandler) {
        clients.remove(clientHandler);
        broadcastClientList();
    }

    private void broadcastClientList() {
         List<String> nicknames = new ArrayList<>();
         for (ClientHandler client : clients) {
             nicknames.add(client.getClientName());
         }
         Message message = Message.createClientList(nicknames);
         broadcastMessage(message.toJson());
    }

    public AuthService getAuthService() {
        return authService;
    }

    public synchronized boolean isNickBusy(String nick) {
        for (ClientHandler client : clients) {
            if (client.getClientName().equals(nick)) {
                return true;
            }
        }
        return false;
    }

    public void broadcastMessage(Message message, ClientHandler... unfilteredClients) {
         broadcastMessage(message.toJson(), unfilteredClients);
    }

    public synchronized void broadcastMessage(String message, ClientHandler... unfilteredClients) {
        List<ClientHandler> unfiltered = Arrays.asList(unfilteredClients);
         for (ClientHandler client : clients) {
             if (!unfiltered.contains(client)) {
                 client.sendMessage(message);
             }
         }
    }

    public synchronized void sendPrivateMessage(Message message) {
        for (ClientHandler client : clients) {
            if (client.getClientName().equals(message.privateMessage.to)) {
                client.sendMessage(message.toJson());
                break;
            }
        }
    }
}
