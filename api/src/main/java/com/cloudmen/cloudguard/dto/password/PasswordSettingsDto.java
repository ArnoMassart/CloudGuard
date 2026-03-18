package com.cloudmen.cloudguard.dto.password;

import com.cloudmen.cloudguard.dto.adminsecuritykeys.AdminWithSecurityKeyDto;

import java.util.List;

public record PasswordSettingsDto(
        List<OuPasswordPolicyDto> passwordPoliciesByOu,
        TwoStepVerificationDto twoStepVerification,
        List<PasswordChangeRequirementDto> usersWithForcedChange,
        PasswordSettingsSummaryDto summary,
        List<AdminWithSecurityKeyDto> adminsWithoutSecurityKeys,
        String adminsSecurityKeysErrorMessage,
        int securityScore,
        SecurityScoreBreakdownDto securityScoreBreakdown
) {
}
