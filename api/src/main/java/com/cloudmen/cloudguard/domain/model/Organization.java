package com.cloudmen.cloudguard.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Tenant / customer organization. Preferences and other org-scoped data reference this id so users with
 * different email domains can belong to the same organization.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "tbl_organizations")
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 255)
    private String name;

    @Column(name="customer_id")
    private String customerId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
