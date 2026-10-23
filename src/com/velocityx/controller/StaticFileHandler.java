package com.velocityx.controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class StaticFileHandler implements HttpHandler {
    private final String baseDir;
    private static final Map<String, String> MIME_TYPES = new HashMap<>();

    static {
        MIME_TYPES.put(".html", "text/html; charset=UTF-8");
        MIME_TYPES.put(".css", "text/css; charset=UTF-8");
        MIME_TYPES.put(".js", "application/javascript; charset=UTF-8");
        MIME_TYPES.put(".json", "application/json; charset=UTF-8");
        MIME_TYPES.put(".png", "image/png");
        MIME_TYPES.put(".jpg", "image/jpeg");
        MIME_TYPES.put(".jpeg", "image/jpeg");
        MIME_TYPES.put(".svg", "image/svg+xml");
        MIME_TYPES.put(".gltf", "model/gltf+json");
        MIME_TYPES.put(".glb", "model/gltf-binary");
        MIME_TYPES.put(".bin", "application/octet-stream");
        MIME_TYPES.put(".ico", "image/x-icon");
    }

    public StaticFileHandler(String baseDir) {
        this.baseDir = baseDir;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if (path.equals("/") || path.isEmpty()) {
            path = "/index.html";
        }

        File file = new File(baseDir, path).getCanonicalFile();
        File base = new File(baseDir).getCanonicalFile();

        // Prevent directory traversal attacks
        if (!file.getPath().startsWith(base.getPath()) || !file.exists() || file.isDirectory()) {
            String notFound = "<h1>404 Not Found - VELOCITY X</h1>";
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(404, notFound.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(notFound.getBytes());
            }
            return;
        }

        String extension = "";
        int i = file.getName().lastIndexOf('.');
        if (i > 0) extension = file.getName().substring(i).toLowerCase();

        String contentType = MIME_TYPES.getOrDefault(extension, "application/octet-stream");
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(200, file.length());

        try (FileInputStream fis = new FileInputStream(file);
             OutputStream os = exchange.getResponseBody()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = fis.read(buffer)) != -1) {
                os.write(buffer, 0, read);
            }
        }
    }
}