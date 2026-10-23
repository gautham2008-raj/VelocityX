package com.velocityx;

import com.sun.net.httpserver.HttpServer;
import com.velocityx.controller.AdminController;
import com.velocityx.controller.AuthController;
import com.velocityx.controller.BookingController;
import com.velocityx.controller.FleetController;
import com.velocityx.controller.StaticFileHandler;
import com.velocityx.db.DatabaseManager;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class Main {
    private static final int DEFAULT_PORT = 8080;

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("   VELOCITY X - NEXT-GEN MOBILITY PLATFORM       ");
        System.out.println("   Built-in High-Performance HTTP Server        ");
        System.out.println("=================================================");

        // Step 1: Initialize Database and Schema Migrations
        DatabaseManager.initialize();

        // Step 2: Configure Server Port (Cloud-Ready via PORT environment variable)
        int port = DEFAULT_PORT;
        String envPort = System.getenv("PORT");
        if (envPort != null && !envPort.trim().isEmpty()) {
            try {
                port = Integer.parseInt(envPort.trim());
            } catch (NumberFormatException e) {
                System.err.println("[Warning] Invalid PORT environment variable. Falling back to default: " + DEFAULT_PORT);
                port = DEFAULT_PORT;
            }
        }

        try {
            // Bind to 0.0.0.0 so both localhost and cloud container networks can reach it
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

            // Step 3: Register API Controller Endpoints
            server.createContext("/api/auth", new AuthController());
            server.createContext("/api/fleet", new FleetController());
            server.createContext("/api/bookings", new BookingController());
            server.createContext("/api/admin", new AdminController());

            // Step 4: Register Frontend Static File Route with root folder "web"
            server.createContext("/", new StaticFileHandler("web"));

            // Multithreaded Executor Pool to handle concurrent HTTP requests smoothly
            server.setExecutor(Executors.newFixedThreadPool(16));

            server.start();

            System.out.println("[Server Ready] VELOCITY X live on http://localhost:" + port);
            System.out.println("[Access] Customer Portal: http://localhost:" + port);
            System.out.println("[Access] Admin Control:   http://localhost:" + port + "/admin-login.html");
            System.out.println("Default Admin Credentials: admin@velocityx.io / Admin@VX2026");
            System.out.println("=================================================");

        } catch (IOException e) {
            System.err.println("[FATAL] Could not start HTTP server on port " + port + ": " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}