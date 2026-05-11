package com.cloudmen.cloudguard.integration.repository;

import com.cloudmen.cloudguard.domain.model.User;
import com.cloudmen.cloudguard.domain.model.UserRole;
import com.cloudmen.cloudguard.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Real-MySQL integration tests for {@link UserRepository}.
 *
 * <p>These tests cover the queries that drive Account beheer screens and are
 * the most sensitive to JPQL / SQL drift:
 * <ul>
 *   <li>{@code findByEmail} / {@code findByEmailIn} — used everywhere the app
 *       resolves a user from an OAuth principal.</li>
 *   <li>{@code findAllByAccessRequested} — Account beheer ➜ requests tab.</li>
 *   <li>{@code findAllAccepted} — Account beheer ➜ all users (with org filter,
 *       active filter and case-insensitive search).</li>
 *   <li>{@code findAllDenied} — Account beheer ➜ denied list.</li>
 *   <li>{@code findByOrganizationIdAndRoleOrderByIdAsc} — used to find SUPER_ADMIN
 *       recipients for weekly critical-notification reminders.</li>
 *   <li>{@code findDistinctOrganizationIds} — drives nightly per-tenant jobs.</li>
 *   <li>The {@code countBy…} derived queries used by the dashboard badges.</li>
 * </ul>
 */
class UserRepositoryIT extends AbstractRepositoryIT {

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void cleanSlate() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("findByEmail returns the user when the email matches and is empty otherwise")
    void findByEmail_resolvesUserByEmail() {
        userRepository.save(user("alice@example.com", "Alice", "Doe", 1L, true));

        Optional<User> found = userRepository.findByEmail("alice@example.com");
        Optional<User> missing = userRepository.findByEmail("ghost@example.com");

        assertThat(found).isPresent().get().extracting(User::getFirstName).isEqualTo("Alice");
        assertThat(missing).isEmpty();
    }

    @Test
    @DisplayName("findByEmailIn returns every user whose email is in the supplied collection")
    void findByEmailIn_returnsMatchingUsers() {
        userRepository.save(user("a@example.com", "A", "A", 1L, true));
        userRepository.save(user("b@example.com", "B", "B", 1L, true));
        userRepository.save(user("c@example.com", "C", "C", 1L, true));

        List<User> found = userRepository.findByEmailIn(List.of("a@example.com", "c@example.com", "x@example.com"));

        assertThat(found)
                .extracting(User::getEmail)
                .containsExactlyInAnyOrder("a@example.com", "c@example.com");
    }

    @Test
    @DisplayName("findAllByAccessRequested returns only users with access_requested = true")
    void findAllByAccessRequested_returnsOnlyRequestedUsers() {
        userRepository.save(accessRequested("pending1@example.com"));
        userRepository.save(accessRequested("pending2@example.com"));
        userRepository.save(accepted("approved@example.com", 1L, true));

        Page<User> page = userRepository.findAllByAccessRequested(null, PageRequest.of(0, 20));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent())
                .extracting(User::getEmail)
                .containsExactlyInAnyOrder("pending1@example.com", "pending2@example.com");
    }

    @Test
    @DisplayName("findAllByAccessRequested applies a case-insensitive partial filter on names and email")
    void findAllByAccessRequested_appliesSearchQuery() {
        userRepository.save(accessRequestedWithNames("alice.smith@example.com", "Alice", "Smith"));
        userRepository.save(accessRequestedWithNames("bob.jones@example.com", "Bob", "Jones"));
        userRepository.save(accessRequestedWithNames("c@another.io", "Carol", "Smithson"));

        Page<User> byFirstName = userRepository.findAllByAccessRequested("ALI", PageRequest.of(0, 20));
        Page<User> byLastName = userRepository.findAllByAccessRequested("smith", PageRequest.of(0, 20));
        Page<User> byEmail = userRepository.findAllByAccessRequested("another.io", PageRequest.of(0, 20));
        Page<User> empty = userRepository.findAllByAccessRequested("", PageRequest.of(0, 20));

        assertThat(byFirstName.getContent()).extracting(User::getEmail).containsExactly("alice.smith@example.com");
        assertThat(byLastName.getContent())
                .extracting(User::getEmail)
                .containsExactlyInAnyOrder("alice.smith@example.com", "c@another.io");
        assertThat(byEmail.getContent()).extracting(User::getEmail).containsExactly("c@another.io");
        assertThat(empty.getTotalElements()).isEqualTo(3);
    }

    @Test
    @DisplayName("findAllAccepted returns only users whose access has been accepted")
    void findAllAccepted_returnsOnlyAcceptedUsers() {
        userRepository.save(accepted("approved@example.com", 1L, true));
        userRepository.save(accessRequested("pending@example.com"));
        userRepository.save(denied("rejected@example.com"));

        Page<User> page = userRepository.findAllAccepted(null, null, null, PageRequest.of(0, 20));

        assertThat(page.getContent())
                .extracting(User::getEmail)
                .containsExactly("approved@example.com");
    }

    @Test
    @DisplayName("findAllAccepted filters by organizationId when one is supplied")
    void findAllAccepted_filtersByOrganization() {
        userRepository.save(accepted("orgA-1@example.com", 1L, true));
        userRepository.save(accepted("orgA-2@example.com", 1L, true));
        userRepository.save(accepted("orgB-1@example.com", 2L, true));

        Page<User> orgA = userRepository.findAllAccepted(1L, null, null, PageRequest.of(0, 20));
        Page<User> orgB = userRepository.findAllAccepted(2L, null, null, PageRequest.of(0, 20));
        Page<User> all = userRepository.findAllAccepted(null, null, null, PageRequest.of(0, 20));

        assertThat(orgA.getContent()).extracting(User::getEmail)
                .containsExactlyInAnyOrder("orgA-1@example.com", "orgA-2@example.com");
        assertThat(orgB.getContent()).extracting(User::getEmail).containsExactly("orgB-1@example.com");
        assertThat(all.getTotalElements()).isEqualTo(3);
    }

    @Test
    @DisplayName("findAllAccepted filters by isActive when the flag is non-null")
    void findAllAccepted_filtersByIsActiveFlag() {
        userRepository.save(accepted("active@example.com", 1L, true));
        userRepository.save(accepted("inactive@example.com", 1L, false));

        Page<User> activeOnly = userRepository.findAllAccepted(null, true, null, PageRequest.of(0, 20));
        Page<User> inactiveOnly = userRepository.findAllAccepted(null, false, null, PageRequest.of(0, 20));
        Page<User> bothWhenNull = userRepository.findAllAccepted(null, null, null, PageRequest.of(0, 20));

        assertThat(activeOnly.getContent()).extracting(User::getEmail).containsExactly("active@example.com");
        assertThat(inactiveOnly.getContent()).extracting(User::getEmail).containsExactly("inactive@example.com");
        assertThat(bothWhenNull.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("findAllAccepted applies case-insensitive partial search across firstName, lastName and email")
    void findAllAccepted_appliesSearchAcrossNamesAndEmail() {
        userRepository.save(acceptedWithNames("dirk.peeters@example.com", "Dirk", "Peeters", 1L));
        userRepository.save(acceptedWithNames("ella.peters@example.com", "Ella", "Peters", 1L));
        userRepository.save(acceptedWithNames("frank.x@example.com", "Frank", "X", 1L));

        Page<User> byFirstName = userRepository.findAllAccepted(null, null, "ella", PageRequest.of(0, 20));
        Page<User> byLastNameStem = userRepository.findAllAccepted(null, null, "PETER", PageRequest.of(0, 20));
        Page<User> byEmail = userRepository.findAllAccepted(null, null, "frank.x", PageRequest.of(0, 20));

        assertThat(byFirstName.getContent()).extracting(User::getEmail).containsExactly("ella.peters@example.com");
        assertThat(byLastNameStem.getContent())
                .extracting(User::getEmail)
                .containsExactlyInAnyOrder("dirk.peeters@example.com", "ella.peters@example.com");
        assertThat(byEmail.getContent()).extracting(User::getEmail).containsExactly("frank.x@example.com");
    }

    @Test
    @DisplayName("findAllAccepted respects Pageable size, page index and Sort")
    void findAllAccepted_paginatesAndSorts() {
        for (int i = 1; i <= 5; i++) {
            userRepository.save(accepted("user-" + i + "@example.com", 1L, true));
        }

        Page<User> firstPage = userRepository.findAllAccepted(
                null, null, null, PageRequest.of(0, 2, Sort.by(Sort.Direction.ASC, "email")));
        Page<User> secondPage = userRepository.findAllAccepted(
                null, null, null, PageRequest.of(1, 2, Sort.by(Sort.Direction.ASC, "email")));

        assertThat(firstPage.getTotalElements()).isEqualTo(5);
        assertThat(firstPage.getTotalPages()).isEqualTo(3);
        assertThat(firstPage.getContent())
                .extracting(User::getEmail)
                .containsExactly("user-1@example.com", "user-2@example.com");
        assertThat(secondPage.getContent())
                .extracting(User::getEmail)
                .containsExactly("user-3@example.com", "user-4@example.com");
    }

    @Test
    @DisplayName("findAllDenied returns only access_denied = true rows and supports search")
    void findAllDenied_filtersAndSearches() {
        userRepository.save(denied("blocked-1@example.com"));
        userRepository.save(denied("blocked-2@example.com"));
        userRepository.save(accepted("ok@example.com", 1L, true));

        Page<User> all = userRepository.findAllDenied(null, PageRequest.of(0, 20));
        Page<User> filtered = userRepository.findAllDenied("blocked-2", PageRequest.of(0, 20));

        assertThat(all.getTotalElements()).isEqualTo(2);
        assertThat(filtered.getContent()).extracting(User::getEmail).containsExactly("blocked-2@example.com");
    }

    @Test
    @DisplayName("countByRoleRequestedTrueOrOrganizationRequestedTrue counts both kinds of pending requests")
    void countByRoleOrOrganizationRequested_countsCorrectly() {
        User a = accepted("a@example.com", 1L, true);
        a.setRoleRequested(true);
        userRepository.save(a);

        User b = accepted("b@example.com", 1L, true);
        b.setOrganizationRequested(true);
        userRepository.save(b);

        User c = accepted("c@example.com", 1L, true);
        c.setRoleRequested(true);
        c.setOrganizationRequested(true);
        userRepository.save(c);

        userRepository.save(accepted("d@example.com", 1L, true));

        long count = userRepository.countByRoleRequestedTrueOrOrganizationRequestedTrue();

        assertThat(count).isEqualTo(3);
    }

    @Test
    @DisplayName("findByOrganizationIdAndRoleOrderByIdAsc returns org members carrying the requested role, ordered by id")
    void findByOrganizationIdAndRole_returnsMatchingMembersInIdOrder() {
        User admin1 = withRoles(accepted("admin1@example.com", 1L, true), UserRole.SUPER_ADMIN);
        User admin2 = withRoles(accepted("admin2@example.com", 1L, true), UserRole.SUPER_ADMIN);
        User viewer = withRoles(accepted("viewer@example.com", 1L, true), UserRole.USERS_GROUPS_VIEWER);
        User otherOrgAdmin = withRoles(accepted("other@example.com", 2L, true), UserRole.SUPER_ADMIN);

        userRepository.saveAll(List.of(admin1, admin2, viewer, otherOrgAdmin));

        List<User> result = userRepository.findByOrganizationIdAndRoleOrderByIdAsc(1L, UserRole.SUPER_ADMIN);

        assertThat(result)
                .extracting(User::getEmail)
                .containsExactly("admin1@example.com", "admin2@example.com");
    }

    @Test
    @DisplayName("findDistinctOrganizationIds returns each org id only once and skips nulls")
    void findDistinctOrganizationIds_deduplicates() {
        userRepository.save(accepted("u1@example.com", 1L, true));
        userRepository.save(accepted("u2@example.com", 1L, true));
        userRepository.save(accepted("u3@example.com", 2L, true));
        userRepository.save(accepted("u4@example.com", null, true));

        List<Long> ids = userRepository.findDistinctOrganizationIds();

        assertThat(ids).containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    @DisplayName("countByAccessRequestedTrue and countByAccessDeniedTrue feed dashboard badges correctly")
    void countByAccessRequestedAndDenied_returnExpectedCounts() {
        userRepository.save(accessRequested("p1@example.com"));
        userRepository.save(accessRequested("p2@example.com"));
        userRepository.save(denied("d1@example.com"));
        userRepository.save(accepted("ok@example.com", 1L, true));

        assertThat(userRepository.countByAccessRequestedTrue()).isEqualTo(2);
        assertThat(userRepository.countByAccessDeniedTrue()).isEqualTo(1);
    }

    @Test
    @DisplayName("existsByOrganizationId returns true only when at least one user references that organization")
    void existsByOrganizationId_reflectsCurrentMembership() {
        userRepository.save(accepted("only-orgA@example.com", 1L, true));

        assertThat(userRepository.existsByOrganizationId(1L)).isTrue();
        assertThat(userRepository.existsByOrganizationId(999L)).isFalse();
    }

    // ---------------------------------------------------------------------
    // Builders
    // ---------------------------------------------------------------------

    private static User user(String email, String firstName, String lastName, Long orgId, boolean active) {
        User u = new User();
        u.setEmail(email);
        u.setFirstName(firstName);
        u.setLastName(lastName);
        u.setOrganizationId(orgId);
        u.setActive(active);
        u.setRoles(new ArrayList<>(List.of(UserRole.UNASSIGNED)));
        return u;
    }

    private static User accepted(String email, Long orgId, boolean active) {
        User u = user(email, "First", "Last", orgId, active);
        u.setAccessAccepted(true);
        return u;
    }

    private static User acceptedWithNames(String email, String firstName, String lastName, Long orgId) {
        User u = user(email, firstName, lastName, orgId, true);
        u.setAccessAccepted(true);
        return u;
    }

    private static User accessRequested(String email) {
        User u = user(email, "Req", "Uester", null, true);
        u.setAccessRequested(true);
        return u;
    }

    private static User accessRequestedWithNames(String email, String firstName, String lastName) {
        User u = user(email, firstName, lastName, null, true);
        u.setAccessRequested(true);
        return u;
    }

    private static User denied(String email) {
        User u = user(email, "Den", "Ied", null, true);
        u.setAccessDenied(true);
        return u;
    }

    private static User withRoles(User u, UserRole... roles) {
        u.setRoles(new ArrayList<>(List.of(roles)));
        return u;
    }
}
