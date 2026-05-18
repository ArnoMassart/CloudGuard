package com.cloudmen.cloudguard.dto.apppasswords;

import java.util.List;

/**
 * One page of {@link UserAppPasswordsDto} from {@code GET /google/app-passwords}.
 *
 * @param users          slice after filter + pagination (only users with ASPs)
 * @param nextPageToken  1-based next page index as string, or {@code null}
 */
public record AppPasswordPageResponse(List<UserAppPasswordsDto> users, String nextPageToken) {}
