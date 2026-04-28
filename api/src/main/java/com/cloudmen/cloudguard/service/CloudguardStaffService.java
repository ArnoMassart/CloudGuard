package com.cloudmen.cloudguard.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CloudguardStaffService {

    private final Set<String> adminEmails;

    public CloudguardStaffService(
            @Value("${cloudguard.staff.admin-emails:}") String emailsCsv)
    {
        this.adminEmails = Arrays.stream(emailsCsv.split(","))
                .map(String::trim)
                .filter(s->!s.isBlank())
                .map(String::toLowerCase)
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean isCloudmenAdmin(String email) {
        return email != null && adminEmails.contains(email.toLowerCase());
    }

    public void requireCloudmenAdmin(String email) {
        if(!isCloudmenAdmin(email))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only CLOUDMEN staff may access this resource.");
    }


}
