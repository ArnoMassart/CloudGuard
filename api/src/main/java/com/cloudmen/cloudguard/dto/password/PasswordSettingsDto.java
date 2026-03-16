package com.cloudmen.cloudguard.dto.password;

import java.util.List;

public record PasswordSettingsDto(
        PasswordPolicyDto policy,
        TwoStepVerificationDto twoStepVerification,
        List<PasswordChangeRequirementDto> usersWithForcedChange,
        PasswordSettingsSummaryDto summary
) {
}
