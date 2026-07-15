package com.example.user.service.abstraction;

import com.example.user.dtos.CreateUserRequest;
import com.example.user.dtos.UpdateUserRequest;
import com.example.user.entity.User;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

public interface UserService {
    User createUser(CreateUserRequest request);
    User getUserById(UUID id);
    Page<User> getAllUsers(int page, int size);

    User updateUser(UUID id, UpdateUserRequest request);
    void deleteUser(UUID id);
    User toggleUserStatus(UUID id);
}
