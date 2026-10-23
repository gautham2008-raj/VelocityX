package com.velocityx.security;

import com.velocityx.model.Role;

public class SecurityContext {
    private final long userId;
    private final String email;
    private final String fullName;
    private final Role role;
    private final String token;

    public SecurityContext(long userId, String email, String fullName, Role role, String token) {
        this.userId = userId;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
        this.token = token;
    }

    public long getUserId() { return userId; }
    public String getEmail() { return email; }
    public String getFullName() { return fullName; }
    public Role getRole() { return role; }
    public String getToken() { return token; }
    public boolean isAdmin() { return role == Role.ADMIN; }
}