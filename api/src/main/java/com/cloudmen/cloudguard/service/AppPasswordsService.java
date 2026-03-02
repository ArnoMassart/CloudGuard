package com.cloudmen.cloudguard.service;

import com.cloudmen.cloudguard.dto.AppPasswordDto;
import com.cloudmen.cloudguard.utility.GoogleApiFactory;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.api.services.admin.directory.Directory;
import com.google.api.services.admin.directory.DirectoryScopes;
import com.google.api.services.admin.directory.model.Asp;
import com.google.api.services.admin.directory.model.Asps;
import com.google.api.services.admin.directory.model.User;
import com.google.api.services.admin.directory.model.Users;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class AppPasswordsService {

    private static final Logger log = LoggerFactory.getLogger(AppPasswordsService.class);

    private final GoogleApiFactory apiFactory;
    private final Cache<String, List<AppPasswordDto>> cache = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .maximumSize(100)
            .build();

    private final Set<String> scopes = Set.of(
            DirectoryScopes.ADMIN_DIRECTORY_USER_SECURITY,
            DirectoryScopes.ADMIN_DIRECTORY_USER_READONLY
    );

    public AppPasswordsService(GoogleApiFactory apiFactory) {
        this.apiFactory = apiFactory;
    }

    public List<AppPasswordDto> getAppPasswords(String adminEmail){
        return cache.get(adminEmail, this::fetchAllAppPasswords);
    }

    public void forceRefreshCache(String adminEmail){
        cache.asMap().compute(adminEmail, (email, existing)-> fetchAllAppPasswords(email));
    }

    private List<AppPasswordDto> fetchAllAppPasswords(String adminEmail){
        try{
            log.info("Ophalen LIVE app passwords van Google voor: {}", adminEmail);
            Directory directory = apiFactory.getDirectoryService(scopes, adminEmail);

            List<AppPasswordDto> result = new ArrayList<>();
            String pageToken = null;

            do{
                Directory.Users.List req = directory.users()
                        .list()
                        .setCustomer("my_customer")
                        .setMaxResults(100);

                if(pageToken != null) req.setPageToken(pageToken);
                Users users = req.execute();

                if(users.getUsers() != null){
                    for(User u : users.getUsers()){
                        String userEmail = u.getPrimaryEmail();
                        log.info("Checking app passwords for user: {}", userEmail);
                        Asps asps = directory.asps().list(userEmail).execute();
                        if(asps.getItems() == null || asps.getItems().isEmpty()) {
                            log.info("  No app passwords for {}", userEmail);
                            continue;
                        }
                        for(Asp asp: asps.getItems()) {
                            log.info("  {} -> app password: name={}, codeId={}", userEmail, asp.getName(), asp.getCodeId());
                            result.add(mapToDto(userEmail, asp));
                        }
                    }
                }
                pageToken = users.getNextPageToken();

            }while(pageToken !=null && !pageToken.isEmpty());

            return result;

        }catch(Exception e){
            throw new RuntimeException("Failed to fetch app passwords",e);
        }

    }

    private AppPasswordDto mapToDto(String email, Asp asp){
        AppPasswordDto dto = new AppPasswordDto();
        dto.setUserEmail(email);
        dto.setCodeId(asp.getCodeId());
        dto.setName(asp.getName());
        dto.setCreationTime(asp.getCreationTime() != null ? String.valueOf(asp.getCreationTime()) : null);
        dto.setLastTimeUsed(asp.getLastTimeUsed() != null ? String.valueOf(asp.getLastTimeUsed()) : null);
        return dto;
    }
}
