package ru.netology;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    public static final List<String> validPaths = List.of("/index.html", "/spring.svg", "/spring.png", "/resources.html", "/styles.css", "/app.js", "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js");
    private final ExecutorService threadsPool;
    private final int port;

    public Server(int port, int threadsCount) {
        this.port = port;
        threadsPool = Executors.newFixedThreadPool(threadsCount);
    }

    public void start(){
        try (final var serverSocket = new ServerSocket(port)) {
            System.out.println("Server launched");
            while (!serverSocket.isClosed()) {
                Socket socket = serverSocket.accept();
                final var clientHandler = connectionHandler(socket);
                threadsPool.execute(clientHandler);
                System.out.println("Handle new connection");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            threadsPool.shutdown();
        }
    }

    private ClientHandler connectionHandler(Socket socket) {
        return new ClientHandler(socket);
    }
}
