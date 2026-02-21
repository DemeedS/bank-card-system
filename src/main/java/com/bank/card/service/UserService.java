package com.bank.card.service;

import com.bank.card.dto.response.PageResponse;
import com.bank.card.dto.response.UserResponse;
import com.bank.card.entity.User;
import org.springframework.data.domain.Pageable;

public interface UserService {

    UserResponse getCurrentUserProfile(User currentUser);
    PageResponse<UserResponse> getAllUsers(Pageable pageable);
    UserResponse getUserById(Long id);
    void deleteUser(Long id);
    UserResponse toggleUserEnabled(Long id, boolean enabled);
}
