package com.cloudmen.cloudguard.dto.apppasswords;

import java.util.List;

/**
 * One user row in app-password listings with Directory identity fields and their ASP list.
 *
 * @param id        Google user id (fallback to primary email when id missing)
 * @param name      display name
 * @param email     primary mailbox used for {@code asps.list}
 * @param role      coarse {@code Admin} vs {@code User} flag from Directory
 * @param tsv       {@code true} when enrolled in 2-step verification ({@code isEnrolledIn2Sv})
 * @param passwords non-empty ASP details mapped from Directory {@link com.google.api.services.admin.directory.model.Asp}
 */
public record UserAppPasswordsDto(String id, String name, String email, String role, boolean tsv, List<AppPasswordDto> passwords) {}
