package com.example.user.service.implementation;

import com.example.user.dtos.CreateUserRequest;
import com.example.user.dtos.UpdateUserRequest;
import com.example.user.entity.User;
import com.example.user.repository.UserRepository;
import com.example.user.service.abstraction.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public User createUser(CreateUserRequest request) {
        User user = User.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .userName(request.userName())
                .email(request.email())
                .role(request.role())
                .dob(request.dob())
                .password(passwordEncoder.encode("DefaultPassword123!")) // Send temporary password
                .isEnabled(true)
                .isUsing2FA(false)
                .build();

        return userRepository.save(user);
    }

    @Override
    public User getUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }



    @Override
    public Page<User> getAllUsers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return userRepository.findAll(pageable);
    }

    @Override
    public User updateUser(UUID id, UpdateUserRequest request) {
        User user = getUserById(id);

        if (request.firstName() != null) user.setFirstName(request.firstName());
        if (request.lastName() != null) user.setLastName(request.lastName());
        if (request.email() != null) {
            if (userRepository.existsByEmail(request.email()) && !user.getEmail().equals(request.email())) {
                throw new RuntimeException("Email already in use");
            }
            user.setEmail(request.email());
        }
        if (request.role() != null) user.setRole(request.role());

        return userRepository.save(user);
    }

    @Override
    public void deleteUser(UUID id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }

    @Override
    public User toggleUserStatus(UUID id) {
        User user = getUserById(id);
        user.setEnabled(!user.isEnabled()); // Flip the boolean
        return userRepository.save(user);
    }
}