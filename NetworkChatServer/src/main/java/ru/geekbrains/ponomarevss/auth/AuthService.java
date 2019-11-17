package ru.geekbrains.ponomarevss.auth;

public interface AuthService {

    void start();
    void stop();

    /**
     *
     * @param login
     * @param pass
     * @return nick or null
     */

    String getNickByLoginPass(String login, String pass);

}
