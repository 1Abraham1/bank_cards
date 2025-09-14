package com.abrik.bank_cards.bank_cards.util;

import org.springframework.stereotype.Component;

@Component
public class RoleUtil {
    public String canonical(String raw) {
        if (raw == null) return null;
        String up = raw.trim().toUpperCase();
        return up.startsWith("ROLE_") ? up.substring("ROLE_".length()) : up;
    }

    public boolean isAdminCanonical(String canonical) {
        return "ADMIN".equals(canonical);
    }
}
