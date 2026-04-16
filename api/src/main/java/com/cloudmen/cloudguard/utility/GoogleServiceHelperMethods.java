package com.cloudmen.cloudguard.utility;

import com.cloudmen.cloudguard.domain.model.UserRole;
import com.cloudmen.cloudguard.dto.users.UserSecurityEvaluation;
import com.cloudmen.cloudguard.exception.GoogleWorkspaceSyncException;
import com.cloudmen.cloudguard.service.OrganizationService;
import com.cloudmen.cloudguard.service.UserService;
import com.google.api.client.util.DateTime;

import java.security.PrivateKey;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

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

    public static final String VIOLATION_NO_2FA = "NO_2FA";
    public static final String VIOLATION_ACTIVITY_STALE = "ACTIVITY_STALE";
    public static final String VIOLATION_ACTIVITY_INACTIVE_RECENT = "ACTIVITY_INACTIVE_RECENT";

    /**
     * Null-safe evaluation aligned with {@link #checkUserSecurityStatus(boolean, DateTime, boolean)}.
     * Violation codes map to users-groups preferences: NO_2FA → 2fa; activity codes → activity.
     */
    public static UserSecurityEvaluation evaluateUserSecurity(boolean isActive, DateTime lastLogin, boolean isTwoFactorEnabled) {
        LocalDate now = LocalDate.now();
        Long daysSinceLogin = null;
        if (lastLogin != null) {
            LocalDate loginDate = DateTimeConverter.convertGoogleDateTimeToLocalDate(lastLogin);
            daysSinceLogin = ChronoUnit.DAYS.between(loginDate, now);
        }

        List<String> violations = new ArrayList<>();
        if (isActive && !isTwoFactorEnabled) {
            violations.add(VIOLATION_NO_2FA);
        }
        if (isActive && (daysSinceLogin == null || daysSinceLogin >= 90)) {
            violations.add(VIOLATION_ACTIVITY_STALE);
        }
        if (!isActive && daysSinceLogin != null && daysSinceLogin <= 7) {
            violations.add(VIOLATION_ACTIVITY_INACTIVE_RECENT);
        }

        boolean conform;
        if (isActive && !isTwoFactorEnabled) {
            conform = false;
        } else if (isActive && (daysSinceLogin == null || daysSinceLogin >= 90)) {
            conform = false;
        } else {
            conform = isActive || (daysSinceLogin != null && daysSinceLogin > 7);
        }
        return new UserSecurityEvaluation(conform, List.copyOf(violations));
    }

    public static boolean checkUserSecurityStatus(boolean isActive, DateTime lastLogin, boolean isTwoFactorEnabled) {
        return evaluateUserSecurity(isActive, lastLogin, isTwoFactorEnabled).conform();
    }

    public static int getPage(String pageToken) {
        int page = 1;
        if (pageToken != null && !pageToken.isBlank()) {
            try { page = Integer.parseInt(pageToken); } catch (NumberFormatException ignored) {}
        }

        return page;
    }

    public static <T> List<T> filterByNameOrEmail(List<T> items, String query, Function<T, String> nameExtractor,
                                                  Function<T, String> emailExtractor) {
        if (query == null || query.trim().isEmpty()) {
            return items;
        }

        String lowerQuery = query.toLowerCase().trim();
        return items.stream()
                .filter(item -> {
                    String name = nameExtractor.apply(item);
                    String email = emailExtractor.apply(item);
                    return (name != null && name.toLowerCase().contains(lowerQuery)) ||
                            (email != null && email.toLowerCase().contains(lowerQuery));
                })
                .toList();
    }


    public static String severity(double score) {
        return severity(score, false, "");
    }

    public static String severity(double score, boolean reverse) {
        return severity(score, reverse, "");
    }

    public static String severity(double score, String color) {
        return severity(score, false, color);
    }

    public static String severity(double score, boolean reverse, String color) {
        if (!color.isEmpty()) {
            return color;
        }

        if (reverse) {
            if (score >= 75) return "error";
            if (score >= 50) return "warning";
            return "success";
        } else {
            if (score >= 75) return "success";
            if (score >= 50) return "warning";
            return "error";
        }
    }

    public static int calculateWeightedScore(int total, int count, double weight, int emptyFallback) {
        if (total == 0) {
            return emptyFallback;
        }

        return (int) Math.round(count * weight / total);
    }

    public static int calculateDeductionScore(int total, int violationCount) {
        if (total == 0) {
            return 100;
        }
        return Math.max(0, 100 - (violationCount * 100 / total));
    }

    public static String getAdminEmailForUser(String email, UserService userService, OrganizationService organizationService) {
        return userService.findByEmailOptional(email)
                .flatMap(user -> organizationService.findById(user.getOrganizationId()))
                .map(org -> {
                    if (org.getAdminEmail() == null || org.getAdminEmail().isBlank()) {
                        throw new GoogleWorkspaceSyncException("No Admin email configured for organization: " + org.getName());
                    }
                    return org.getAdminEmail();
                })
                .orElseThrow(() -> new GoogleWorkspaceSyncException("User or Organization not found for: " + email));
    }

    public static boolean hasAccessToModule(List<UserRole> roles, UserRole requiredRole) {
        if (roles==null) return false;
        return roles.contains(UserRole.SUPER_ADMIN) | roles.contains(requiredRole);
    }
}
