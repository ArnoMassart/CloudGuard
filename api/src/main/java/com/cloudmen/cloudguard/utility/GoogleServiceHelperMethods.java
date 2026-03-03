package com.cloudmen.cloudguard.utility;

import com.google.api.client.util.DateTime;

import java.security.PrivateKey;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class GoogleServiceHelperMethods {
    public static PrivateKey decodePrivateKey(String pem) throws Exception {
        String privateKeyPEM = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] encoded = java.util.Base64.getDecoder().decode(privateKeyPEM);
        java.security.spec.PKCS8EncodedKeySpec keySpec = new java.security.spec.PKCS8EncodedKeySpec(encoded);
        java.security.KeyFactory kf = java.security.KeyFactory.getInstance("RSA");
        return kf.generatePrivate(keySpec);
    }

    public static String translateRoleName(String role) {
        if (role == null) return "User";
        return switch (role) {
            case "_SEED_ADMIN_ROLE" -> "Super Admin";
            case "_READ_ONLY_ADMIN_ROLE" -> "Read Only Admin";
            default -> role;
        };
    }

    public static boolean checkUserSecurityStatus(boolean isActive, DateTime lastLogin, boolean isTwoFactorEnabled) {
        LocalDate loginDate = DateTimeConverter.convertGoogleDateTime(lastLogin);
        LocalDate now = LocalDate.now();

        long daysSinceLogin = ChronoUnit.DAYS.between(loginDate, now);
        long yearsSinceLogin = ChronoUnit.YEARS.between(loginDate, now);

        if (isActive && !isTwoFactorEnabled) return false;
        if (isActive && yearsSinceLogin >= 1) return false;
        return isActive || daysSinceLogin > 7;
    }

    public static int getPage(String pageToken) {
        int page = 1;
        if (pageToken != null && !pageToken.isBlank()) {
            try { page = Integer.parseInt(pageToken); } catch (NumberFormatException ignored) {}
        }

        return page;
    }
}
