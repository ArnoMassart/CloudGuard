package com.cloudmen.cloudguard.service;

import com.cloudmen.cloudguard.domain.model.Organization;
import com.cloudmen.cloudguard.domain.model.User;
import com.cloudmen.cloudguard.dto.users.UserDto;
import com.cloudmen.cloudguard.repository.OrganizationRepository;
import com.cloudmen.cloudguard.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;

    public UserService(UserRepository userRepository, OrganizationRepository organizationRepository) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
    }

    public UserDto convertToDto(User user) {
        UserDto dto = new UserDto(
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPictureUrl(),
                user.getCreatedAt()
        );
        Long orgId = user.getOrganizationId();
        if (orgId != null) {
            organizationRepository
                    .findById(orgId)
                    .map(Organization::getName)
                    .ifPresent(dto::setOrganizationName);
        }
        return dto;
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public User save(User user) {
        return userRepository.save(user);
    }

    public List<String> getAllEmails() {
        return userRepository.findAll().stream().map(User::getEmail).toList();
    }

    public String getLanguage(String email) {
        Optional<User> user = userRepository.findByEmail(email);

        if (user.isPresent()) {
            String lang = user.get().getLanguage();

            if (lang == null) return "nl";

            return lang;
        }

        return "nl";
    }

    @Transactional
    public void updateLanguage(String email, String newLanguage) {
        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            user.setLanguage(newLanguage);
            userRepository.save(user);
        }
    }
}
