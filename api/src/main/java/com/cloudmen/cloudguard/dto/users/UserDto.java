package com.cloudmen.cloudguard.dto.users;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
@Getter
@Setter
public class UserDto {
    private String email;
    private String firstName;
    private String lastName;
    private String pictureUrl;
    private List<String> roles;
    private LocalDateTime createdAt;
    private String organizationName;

    public UserDto(String email, String firstName, String lastName, String pictureUrl, LocalDateTime createdAt) {
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.pictureUrl = pictureUrl;
        this.createdAt = createdAt;
    }
}
