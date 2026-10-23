package com.velocityx.security;

import com.sun.net.httpserver.HttpExchange;
import java.util.List;

public class CookieUtil {

    public static String getSessionTokenFromHeaders(HttpExchange exchange) {
        List<String> cookies = exchange.getRequestHeaders().get("Cookie");
        if (cookies == null || cookies.isEmpty()) return null;

        for (String cookieHeader : cookies) {
            String[] pairs = cookieHeader.split(";");
            for (String pair : pairs) {
                String[] kv = pair.trim().split("=", 2);
                if (kv.length == 2 && SessionManager.SESSION_COOKIE_NAME.equals(kv[0])) {
                    return kv[1];
                }
            }
        }
        return null;
    }

    public static void setSessionCookie(HttpExchange exchange, String token, int maxAgeSeconds) {
        String cookieVal = String.format("%s=%s; Path=/; Max-Age=%d; HttpOnly; SameSite=Lax",
                SessionManager.SESSION_COOKIE_NAME, token, maxAgeSeconds);
        exchange.getResponseHeaders().add("Set-Cookie", cookieVal);
    }

    public static void clearSessionCookie(HttpExchange exchange) {
        String cookieVal = String.format("%s=deleted; Path=/; Max-Age=0; HttpOnly; SameSite=Lax",
                SessionManager.SESSION_COOKIE_NAME);
        exchange.getResponseHeaders().add("Set-Cookie", cookieVal);
    }
}