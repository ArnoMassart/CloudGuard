package com.cloudmen.cloudguard.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity(name = "tbl_users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    private String firstName;
    private String lastName;
    private String pictureUrl;
    private LocalDateTime createdAt;

    private String language = "nl";
    private boolean roleRequested = false;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "tbl_user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private List<UserRole> roles = new ArrayList<>(List.of(UserRole.UNASSIGNED));
}
