package com.cloudmen.cloudguard.dto.contact;

public record ContactMessageDto(
        String topic,
        String subject,
        String message
) {
}
