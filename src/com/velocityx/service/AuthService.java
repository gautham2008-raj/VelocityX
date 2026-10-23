package com.velocityx.service;

import com.velocityx.model.Role;
import com.velocityx.model.User;
import com.velocityx.repository.UserRepository;
import com.velocityx.security.PasswordUtil;
import com.velocityx.security.SecurityContext;
import com.velocityx.security.SessionManager;

import java.sql.SQLException;
import java.util.Optional;

public class AuthService {
    private final UserRepository userRepo = new UserRepository();

    public record AuthResult(User user, String sessionToken) {}

    public AuthResult register(String fullName, String email, String rawPassword, String phone) throws SQLException {
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new IllegalArgumentException("Full name is required.");
        }
        if (email == null || !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new IllegalArgumentException("Valid email address is required.");
        }
        if (rawPassword == null || rawPassword.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters long.");
        }

        if (userRepo.findByEmail(email).isPresent()) {
            throw new IllegalStateException("An account with this email already exists.");
        }

        String salt = PasswordUtil.generateSalt();
        String hash = PasswordUtil.hashPassword(rawPassword, salt);

        User newUser = new User();
        newUser.setFullName(fullName.trim());
        newUser.setEmail(email.trim().toLowerCase());
        newUser.setPasswordHash(hash);
        newUser.setSalt(salt);
        newUser.setRole(Role.CUSTOMER);
        newUser.setPhone(phone != null ? phone.trim() : "");

        User created = userRepo.createUser(newUser);
        String sessionToken = SessionManager.createSession(created);

        return new AuthResult(created, sessionToken);
    }

    public AuthResult login(String email, String rawPassword, boolean requireAdminRole) throws SQLException {
        if (email == null || rawPassword == null) {
            throw new IllegalArgumentException("Email and password cannot be empty.");
        }

        Optional<User> optUser = userRepo.findByEmail(email);
        if (optUser.isEmpty()) {
            throw new SecurityException("Invalid email or password.");
        }

        User user = optUser.get();
        boolean verified = PasswordUtil.verifyPassword(rawPassword, user.getPasswordHash(), user.getSalt());
        if (!verified) {
            throw new SecurityException("Invalid email or password.");
        }

        if (requireAdminRole && user.getRole() != Role.ADMIN) {
            throw new SecurityException("Access denied: Elevated administrator clearance required.");
        }

        String sessionToken = SessionManager.createSession(user);
        return new AuthResult(user, sessionToken);
    }

    public SecurityContext validateSessionToken(String token) {
        return SessionManager.validateSession(token);
    }

    public void logout(String token) {
        if (token != null && !token.isBlank()) {
            SessionManager.destroySession(token);
        }
    }
}