package com.abrik.bank_cards.bank_cards.util;

public class RoleUtil {
    private RoleUtil() {}

    public static String canonical(String raw) {
        if (raw == null) return null;
        String up = raw.trim().toUpperCase();
        return up.startsWith("ROLE_") ? up.substring("ROLE_".length()) : up;
    }

    public static boolean isAdminCanonical(String canonical) {
        return "ADMIN".equals(canonical);
    }
}
