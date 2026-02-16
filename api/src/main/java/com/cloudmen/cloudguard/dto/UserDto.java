package com.cloudmen.cloudguard.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class UserDto {
    private String email;
    private String firstName;
    private String lastName;
    private LocalDateTime createdAt;

    public UserDto(String email, String firstName, String lastName, LocalDateTime createdAt) {
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.createdAt = createdAt;
    }
}
