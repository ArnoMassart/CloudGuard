package com.cloudmen.cloudguard.dto.users;

import com.cloudmen.cloudguard.domain.model.UserRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

public record UserDto(
        String email,
        String firstName,
        String lastName,
        String pictureUrl,
        List<UserRole> roles,
        LocalDateTime createdAt,
        Boolean roleRequested
        ) {

}
