package com.cloudmen.cloudguard.dto.apppasswords;

import java.util.List;

/**
 * Value cached per viewer in {@link com.cloudmen.cloudguard.service.AppPasswordsService}: users who have ASPs (list may be
 * empty) and the total number of Directory users scanned for scoring denominators.
 *
 * @param users           only users with one or more ASPs
 * @param totalUserCount  all customer users visited during the sync (including those without ASPs)
 */
public record AppPasswordCacheEntry(List<UserAppPasswordsDto> users, int totalUserCount) {}
