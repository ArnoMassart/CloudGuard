package com.cloudmen.cloudguard.service;

import com.cloudmen.cloudguard.domain.model.User;
import com.cloudmen.cloudguard.domain.model.UserRole;
import com.cloudmen.cloudguard.dto.users.DatabaseUsersResponse;
import com.cloudmen.cloudguard.dto.users.UserDto;
import com.cloudmen.cloudguard.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserDto convertToDto(User user) {
        return new UserDto(
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPictureUrl(),
                user.getCreatedAt()
        );
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

    public boolean getRoleRequested(String email) {
        Optional<User> userOptional = userRepository.findByEmail(email);

        return userOptional.map(User::isRoleRequested).orElse(false);
    }

    @Transactional
    public void updateRequestAccess(String email) {
        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            user.setRoleRequested(true);
            userRepository.save(user);
        }
    }

    public boolean hasValidRole(String email) {
        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isPresent()) {
            User user = userOptional.get();

            if (user.getRoles().isEmpty()) return false;
            return user.getRoles().stream().noneMatch(userRole -> userRole.equals(UserRole.UNASSIGNED));
        }

        return false;
    }

    public DatabaseUsersResponse getAll(String pageToken, int size, String query) {
        return fetchUsersFromDb(false, pageToken, size, query);
    }

    public DatabaseUsersResponse getAllWithRequestedRole(String pageToken, int size, String query) {
        return fetchUsersFromDb(true, pageToken, size, query);
    }

    private DatabaseUsersResponse fetchUsersFromDb(boolean roleRequested, String pageToken, int size, String query) {
        int page = (pageToken == null || pageToken.isEmpty()) ? 0 : Integer.parseInt(pageToken);

        Pageable pageable = PageRequest.of(page, size, Sort.by("firstName").ascending());

        Page<User> userPage = userRepository.findAllByRoleRequestedWithSearch(roleRequested, query, pageable);

        String nextPageToken = userPage.hasNext() ? String.valueOf(page + 1) : null;

        return new DatabaseUsersResponse(userPage.getContent(), nextPageToken);
    }

    public void updateRoles(String email, List<UserRole> roles) {
      Optional<User> userOptional = userRepository.findByEmail(email);

      if (userOptional.isPresent()) {
          User user = userOptional.get();
          user.setRoles(roles);
          userRepository.save(user);
      }
    }

    public void updateRolesAndUpdateRequestedStatus(String email, List<UserRole> roles) {
        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            user.setRoles(roles);
            user.setRoleRequested(false);
            userRepository.save(user);
        }
    }

    public long getAllRequestedCount() {
        return userRepository.countByRoleRequestedTrue();
    }
}
