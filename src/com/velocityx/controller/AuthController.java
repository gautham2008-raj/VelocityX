package com.velocityx.controller;

import com.sun.net.httpserver.HttpExchange;
import com.velocityx.model.Role;
import com.velocityx.model.User;
import com.velocityx.repository.UserRepository;
import com.velocityx.security.CookieUtil;
import com.velocityx.security.PasswordUtil;
import com.velocityx.security.SecurityContext;
import com.velocityx.security.SessionManager;
import com.velocityx.util.JsonUtil;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class AuthController extends BaseApiHandler {
    private final UserRepository userRepo = new UserRepository();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod().toUpperCase();

        try {
            if ("POST".equals(method) && path.equals("/api/auth/register")) {
                handleRegister(exchange);
            } else if ("POST".equals(method) && path.equals("/api/auth/login")) {
                handleLogin(exchange, false);
            } else if ("POST".equals(method) && path.equals("/api/auth/admin-login")) {
                handleLogin(exchange, true);
            } else if ("GET".equals(method) && path.equals("/api/auth/me")) {
                handleCheckSession(exchange);
            } else if ("POST".equals(method) && path.equals("/api/auth/logout")) {
                handleLogout(exchange);
            } else {
                sendError(exchange, 404, "Endpoint not found");
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendError(exchange, 500, "Internal Server Error: " + e.getMessage());
        }
    }

    private void handleRegister(HttpExchange exchange) throws IOException, SQLException {
        String body = readRequestBody(exchange);
        Map<String, String> data = JsonUtil.parseSimpleJson(body);

        String fullName = data.get("fullName");
        String email = data.get("email");
        String password = data.get("password");
        String phone = data.get("phone");
        String requestedRole = data.getOrDefault("role", "CUSTOMER");
        String adminSecret = data.get("adminSecret");

        if (fullName == null || email == null || password == null || password.length() < 6) {
            sendError(exchange, 400, "Invalid fields. Password must be at least 6 characters.");
            return;
        }

        Role assignedRole = Role.CUSTOMER;
        if ("ADMIN".equalsIgnoreCase(requestedRole)) {
            // Require secret passcode for Administrator tier
            if (!"VX-ROOT-2026".equals(adminSecret)) {
                sendError(exchange, 403, "Invalid Administrator Authorization Secret Key.");
                return;
            }
            assignedRole = Role.ADMIN;
        }

        if (userRepo.findByEmail(email).isPresent()) {
            sendError(exchange, 409, "An account with this email already exists.");
            return;
        }

        String salt = PasswordUtil.generateSalt();
        String hash = PasswordUtil.hashPassword(password, salt);

        User newUser = new User();
        newUser.setFullName(fullName);
        newUser.setEmail(email);
        newUser.setPasswordHash(hash);
        newUser.setSalt(salt);
        newUser.setRole(assignedRole);
        newUser.setPhone(phone != null ? phone : "");

        userRepo.createUser(newUser);

        String token = SessionManager.createSession(newUser);
        CookieUtil.setSessionCookie(exchange, token, 86400);

        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("message", "Registration successful.");
        resp.put("role", newUser.getRole().name());
        resp.put("name", newUser.getFullName());
        sendJsonResponse(exchange, 201, resp);
    }

    private void handleLogin(HttpExchange exchange, boolean requireAdmin) throws IOException, SQLException {
        String body = readRequestBody(exchange);
        Map<String, String> data = JsonUtil.parseSimpleJson(body);

        String email = data.get("email");
        String password = data.get("password");

        if (email == null || password == null) {
            sendError(exchange, 400, "Email and password are required.");
            return;
        }

        Optional<User> optUser = userRepo.findByEmail(email);
        if (optUser.isEmpty()) {
            sendError(exchange, 401, "Invalid credentials.");
            return;
        }

        User user = optUser.get();
        if (!PasswordUtil.verifyPassword(password, user.getPasswordHash(), user.getSalt())) {
            sendError(exchange, 401, "Invalid credentials.");
            return;
        }

        if (requireAdmin && user.getRole() != Role.ADMIN) {
            sendError(exchange, 403, "Access denied: Root Operations Administrator clearance required.");
            return;
        }

        String token = SessionManager.createSession(user);
        CookieUtil.setSessionCookie(exchange, token, 86400);

        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("message", "Authentication confirmed.");
        resp.put("role", user.getRole().name());
        resp.put("name", user.getFullName());
        sendJsonResponse(exchange, 200, resp);
    }

    private void handleCheckSession(HttpExchange exchange) throws IOException {
        SecurityContext ctx = getSecurityContext(exchange);
        Map<String, Object> resp = new HashMap<>();
        if (ctx != null) {
            resp.put("authenticated", true);
            resp.put("userId", ctx.getUserId());
            resp.put("name", ctx.getFullName());
            resp.put("email", ctx.getEmail());
            resp.put("role", ctx.getRole().name());
        } else {
            resp.put("authenticated", false);
        }
        sendJsonResponse(exchange, 200, resp);
    }

    private void handleLogout(HttpExchange exchange) throws IOException {
        String token = CookieUtil.getSessionTokenFromHeaders(exchange);
        if (token != null) {
            SessionManager.destroySession(token);
        }
        CookieUtil.clearSessionCookie(exchange);

        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("message", "Session cleared.");
        sendJsonResponse(exchange, 200, resp);
    }
}