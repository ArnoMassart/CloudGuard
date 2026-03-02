package com.cloudmen.cloudguard.service;

import com.cloudmen.cloudguard.dto.AppPasswordDto;
import com.cloudmen.cloudguard.utility.GoogleApiFactory;
import com.google.api.services.admin.directory.Directory;
import com.google.api.services.admin.directory.DirectoryScopes;
import com.google.api.services.admin.directory.model.Asp;
import com.google.api.services.admin.directory.model.Asps;
import com.google.api.services.admin.directory.model.User;
import com.google.api.services.admin.directory.model.Users;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class AppPasswordsService {

    private final GoogleApiFactory apiFactory;

    Set<String> scopes = Set.of(
            DirectoryScopes.ADMIN_DIRECTORY_USER_SECURITY,
            DirectoryScopes.ADMIN_DIRECTORY_USER_READONLY
    );

    public AppPasswordsService(GoogleApiFactory apiFactory) {
        this.apiFactory = apiFactory;
    }

    public List<AppPasswordDto> getAllAppPasswords(String adminEmail){
        try{
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
                        Asps asps = directory.asps().list(userEmail).execute();
                        if(asps.getItems() == null) continue;

                        for(Asp asp: asps.getItems()) {
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
