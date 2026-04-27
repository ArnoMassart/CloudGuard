package com.cloudmen.cloudguard.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a user in the system.
 * <p>
 * This class defines the persistent representation of a user, including its ID,
 * organization ID, email, first name, last name, picture URL, creation timestamp,
 * preferences, and assigned roles.
 * <p>
 * It is mapped to the {@code tbl_users} table in the database.
 */
@Getter
@Setter
@Entity(name = "tbl_users")
public class User {

    /**
     * The unique identifier of the user. <p>
     *
     * This is the primary key for the {@code tbl_users} table and is automatically generated
     * using the identity column strategy.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The identifier of the organization the user belongs to. <p>
     *
     * It is mapped to the {@code organization_id} column in the database.
     */
    @Column(name = "organization_id")
    private Long organizationId;

    /**
     * The email address of the user. <p>
     *
     * This field is required, cannot be null, and must be unique across all users.
     */
    @Column(unique = true, nullable = false)
    private String email;

    /**
     * The first name of the user. <p>
     *
     * This field can be null.
     */
    private String firstName;

    /**
     * The last name of the user. <p>
     *
     * This field can be null.
     */
    private String lastName;

    /**
     * The URL pointing to the user's profile picture. <p>
     *
     * This field can be null.
     */
    private String pictureUrl;

    /**
     * The timestamp indicating when the user's account was created. <p>
     *
     * This field can be null.
     */
    private LocalDateTime createdAt;

    /**
     * The preferred language code of the user. <p>
     *
     * This field defaults to {@code "nl"} (Dutch).
     */
    private String language = "nl";

    /**
     * A flag indicating whether the user has actively requested a role assignment. <p>
     *
     * This field defaults to {@code false}.
     */
    private boolean roleRequested = false;

    /**
     * A flag indicating whether the user has actively requested to be assigned to an organization. <p>
     *
     * This field defaults to {@code false}.
     */
    private boolean organizationRequested = false;

    /**
     * A list of roles assigned to the user. <p>
     *
     * This field is stored as an {@link ElementCollection} fetched eagerly from the database.
     * The roles are mapped as string enumerations in a separate {@code tbl_user_roles} table,
     * which is joined to this entity via the {@code user_id} column. <p>
     *
     * By default, a new user is initialized with a single {@link UserRole#UNASSIGNED} role.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "tbl_user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 100)
    private List<UserRole> roles = new ArrayList<>(List.of(UserRole.UNASSIGNED));
}
