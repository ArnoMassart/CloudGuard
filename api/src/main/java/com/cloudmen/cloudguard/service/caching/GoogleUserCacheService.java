package com.cloudmen.cloudguard.service.caching;

import com.cloudmen.cloudguard.dto.users.UserCacheEntry;
import com.cloudmen.cloudguard.dto.users.UserOrgDetail;
import com.cloudmen.cloudguard.dto.users.UserPageResponse;
import com.cloudmen.cloudguard.utility.GoogleApiFactory;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GoogleUserCacheService {
    private final Map<String, UserCacheEntry> userCacheEntryMap = new ConcurrentHashMap<>();

    private static final long CACHE_DURATION_MS = 3600000L;

    private final GoogleApiFactory googleApiFactory;

    public GoogleUserCacheService(GoogleApiFactory googleApiFactory) {
        this.googleApiFactory = googleApiFactory;
    }

//    public UserPageResponse getCachedUsersPaged(String adminEmail, int page, int size, String query) {
//
//    }
}
