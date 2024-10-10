package ru.netology;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

public class ClientHandler implements Runnable {
    private final Socket socket;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (
                final var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                final var out = new BufferedOutputStream(socket.getOutputStream());
        ) {
            // read only request line for simplicity
            // must be in form GET /path HTTP/1.1
            final var requestLine = in.readLine();
            final var parts = requestLine.split(" ");

            if (parts.length != 3) {
                // just close socket
                return;
            }

            final var path = parts[1];
            final var pathParts = path.split("\\?");

            // проверяем наличие параметров в строке и парсим, если они есть
            if (pathParts.length > 1) {
                final var params = pathParts[1];
                getQueryParams(params);
            }

            if (!Server.validPaths.contains(pathParts[0])) {
                sendNotFound(out);
                return;
            }

            // special case for classic
            if (pathParts[0].equals("/classic.html")) {
                sendOkSpecial(out);
                return;
            }

            sendOk(out, pathParts[0]);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void getQueryParams(String params) {
        final var parsedParams = URLEncodedUtils.parse(params, StandardCharsets.UTF_8);
        for (NameValuePair param : parsedParams) {
            System.out.println(param.getName() + ": " + param.getValue());
        }
    }

    private void sendNotFound(BufferedOutputStream out) throws IOException {
        out.write((
                "HTTP/1.1 404 Not Found\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.flush();
    }

    private void sendOk(BufferedOutputStream out, String path) throws IOException {
        final var filePath = Path.of(".", "public", path);
        final var mimeType = Files.probeContentType(filePath);

        final var length = Files.size(filePath);
        out.write((
                "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: " + mimeType + "\r\n" +
                        "Content-Length: " + length + "\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        Files.copy(filePath, out);
        out.flush();
    }

    private void sendOkSpecial(BufferedOutputStream out) throws IOException {
        final var filePath = Path.of(".", "public", "classic.html");
        final var mimeType = Files.probeContentType(filePath);

        final var template = Files.readString(filePath);
        final var content = template.replace(
                "{time}",
                LocalDateTime.now().toString()
        ).getBytes();

        out.write((
                "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: " + mimeType + "\r\n" +
                        "Content-Length: " + content.length + "\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.write(content);
        out.flush();
    }
}
