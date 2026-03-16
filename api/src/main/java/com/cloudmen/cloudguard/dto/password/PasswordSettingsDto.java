package com.cloudmen.cloudguard.dto.password;

import java.util.List;

public record PasswordSettingsDto(
        List<OuPasswordPolicyDto> passwordPoliciesByOu,
        TwoStepVerificationDto twoStepVerification,
        List<PasswordChangeRequirementDto> usersWithForcedChange,
        PasswordSettingsSummaryDto summary
) {
}
