package com.cloudmen.cloudguard.dto.users;

import com.cloudmen.cloudguard.domain.model.UserRole;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;

public record UserDto(
        String email,
        String firstName,
        String lastName,
        String pictureUrl,
        List<UserRole> roles,
        LocalDateTime createdAt,
        Boolean roleRequested,
        Boolean organizationRequested,
        Long organizationId,
        String organizationName,
        @JsonProperty("isCloudmenStaff") Boolean isCloudmenStaff
        ) {

}
