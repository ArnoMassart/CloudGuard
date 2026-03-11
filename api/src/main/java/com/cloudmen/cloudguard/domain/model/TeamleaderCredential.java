package com.cloudmen.cloudguard.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
public class TeamleaderCredential {
    @Id
    private String id = "SINGLETON"; // We gebruiken slechts één set tokens

    @Column(length = 2048)
    private String accessToken;

    @Column(length = 2048)
    private String refreshToken;

    private LocalDateTime updatedAt;
}
