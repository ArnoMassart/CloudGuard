package com.cloudmen.cloudguard.dto.report;

public record DomainTip(
        String title,
        String validDesc,
        String actionDesc,
        String attentionDesc,
        String tip
) {}
