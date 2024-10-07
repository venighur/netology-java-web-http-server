package ru.netology;

public class Main {
    public static void main(String[] args) {
        final var server = new Server(9999, 64);
        server.start();
    }
}