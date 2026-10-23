package com.velocityx.security;

import com.velocityx.db.DatabaseManager;
import com.velocityx.model.Role;
import com.velocityx.model.User;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Base64;

public class SessionManager {
    public static final String SESSION_COOKIE_NAME = "VX_AUTH_SESSION";
    private static final int SESSION_DURATION_HOURS = 24;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public static String createSession(User user) throws SQLException {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(SESSION_DURATION_HOURS);

        String sql = "INSERT INTO user_sessions (session_token, user_id, role, expires_at) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, token);
            ps.setLong(2, user.getId());
            ps.setString(3, user.getRole().name());
            ps.setTimestamp(4, Timestamp.valueOf(expiresAt));
            ps.executeUpdate();
        }
        return token;
    }

    public static SecurityContext validateSession(String token) {
        if (token == null || token.isBlank()) return null;

        String sql = """
            SELECT s.session_token, s.expires_at, u.id, u.email, u.full_name, u.role
            FROM user_sessions s
            JOIN users u ON s.user_id = u.id
            WHERE s.session_token = ?
        """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, token);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Timestamp expiresAt = rs.getTimestamp("expires_at");
                    if (expiresAt.toLocalDateTime().isAfter(LocalDateTime.now())) {
                        return new SecurityContext(
                            rs.getLong("id"),
                            rs.getString("email"),
                            rs.getString("full_name"),
                            Role.valueOf(rs.getString("role")),
                            token
                        );
                    } else {
                        // Clean up expired session
                        destroySession(token);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[SessionManager] Validation error: " + e.getMessage());
        }
        return null;
    }

    public static void destroySession(String token) {
        if (token == null) return;
        String sql = "DELETE FROM user_sessions WHERE session_token = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, token);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[SessionManager] Invalidation error: " + e.getMessage());
        }
    }
}