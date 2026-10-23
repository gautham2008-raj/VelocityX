package com.velocityx.util;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

public class JsonUtil {

    public static String toJson(Object obj) {
        if (obj == null) return "null";
        if (obj instanceof String) return "\"" + escape((String) obj) + "\"";
        if (obj instanceof Number || obj instanceof Boolean) return obj.toString();
        if (obj instanceof LocalDate || obj instanceof LocalDateTime || obj instanceof Enum) {
            return "\"" + obj.toString() + "\"";
        }
        if (obj instanceof Collection<?>) {
            StringBuilder sb = new StringBuilder("[");
            Iterator<?> it = ((Collection<?>) obj).iterator();
            while (it.hasNext()) {
                sb.append(toJson(it.next()));
                if (it.hasNext()) sb.append(",");
            }
            sb.append("]");
            return sb.toString();
        }
        if (obj instanceof Map<?, ?>) {
            Map<?, ?> map = (Map<?, ?>) obj;
            StringBuilder sb = new StringBuilder("{");
            Iterator<? extends Map.Entry<?, ?>> it = map.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<?, ?> entry = it.next();
                sb.append("\"").append(escape(String.valueOf(entry.getKey()))).append("\":");
                sb.append(toJson(entry.getValue()));
                if (it.hasNext()) sb.append(",");
            }
            sb.append("}");
            return sb.toString();
        }

        // Generic object serialization via reflection
        StringBuilder sb = new StringBuilder("{");
        Field[] fields = obj.getClass().getDeclaredFields();
        boolean first = true;
        for (Field f : fields) {
            f.setAccessible(true);
            try {
                Object val = f.get(obj);
                if (!first) sb.append(",");
                sb.append("\"").append(f.getName()).append("\":").append(toJson(val));
                first = false;
            } catch (IllegalAccessException ignored) {}
        }
        sb.append("}");
        return sb.toString();
    }

    public static Map<String, String> parseSimpleJson(String json) {
        Map<String, String> map = new HashMap<>();
        if (json == null || json.trim().isEmpty()) return map;
        String trimmed = json.trim();
        if (trimmed.startsWith("{")) trimmed = trimmed.substring(1);
        if (trimmed.endsWith("}")) trimmed = trimmed.substring(0, trimmed.length() - 1);

        // Parse key-value pairs respecting quotes
        boolean inQuotes = false;
        StringBuilder currentKey = new StringBuilder();
        StringBuilder currentValue = new StringBuilder();
        boolean parsingKey = true;

        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (c == '\"') {
                inQuotes = !inQuotes;
                continue;
            }
            if (inQuotes) {
                if (parsingKey) currentKey.append(c);
                else currentValue.append(c);
                continue;
            }
            if (c == ':') {
                parsingKey = false;
                continue;
            }
            if (c == ',') {
                map.put(currentKey.toString().trim(), currentValue.toString().trim());
                currentKey.setLength(0);
                currentValue.setLength(0);
                parsingKey = true;
                continue;
            }
            if (parsingKey) currentKey.append(c);
            else currentValue.append(c);
        }
        if (currentKey.length() > 0) {
            map.put(currentKey.toString().trim(), currentValue.toString().trim());
        }
        return map;
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}