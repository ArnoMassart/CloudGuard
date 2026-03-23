package com.cloudmen.cloudguard.service;

import com.cloudmen.cloudguard.domain.model.User;
import com.cloudmen.cloudguard.dto.users.UserDto;
import com.cloudmen.cloudguard.repository.UserRepository;
import org.springframework.stereotype.Service;

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
}
