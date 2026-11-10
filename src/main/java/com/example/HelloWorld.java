package com.example;

import com.sun.net.httpserver.HttpServer;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class HelloWorld {

    public static String getMessage() {
        return "Hello from Hemanth App!\nBuild, Test, Scan and Deploy - Automated through Jenkins CI/CD!";
    }

    public static void main(String[] args) throws Exception {

        HttpServer server = HttpServer.create(
            new InetSocketAddress("0.0.0.0", 8085), 0
        );

        server.createContext("/", exchange -> {

            byte[] response = getMessage().getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders().set(
                "Content-Type", "text/plain; charset=UTF-8"
            );

            exchange.sendResponseHeaders(200, response.length);

            try (OutputStream output = exchange.getResponseBody()) {
                output.write(response);
            }
        });

        server.start();

        System.out.println("Application started on port 8085");
    }
}
