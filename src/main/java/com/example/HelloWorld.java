package com.example;

import com.sun.net.httpserver.HttpServer;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class HelloWorld {

    public static String getMessage() {
        return "Hello from Hemanth Application!";
    }

    public static void main(String[] args) throws Exception {

        HttpServer server = HttpServer.create(
            new InetSocketAddress("0.0.0.0", 8080), 0
        );

        server.createContext("/", exchange -> {

            byte[] responseBytes =
                getMessage().getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders().set(
                "Content-Type", "text/plain; charset=UTF-8"
            );

            exchange.sendResponseHeaders(200, responseBytes.length);

            try (OutputStream output = exchange.getResponseBody()) {
                output.write(responseBytes);
            }
        });

        server.start();

        System.out.println("Hemanth Application started on port 8080");
    }
}
