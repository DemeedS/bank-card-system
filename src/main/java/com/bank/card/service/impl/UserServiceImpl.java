package com.bank.card.service.impl;

import com.bank.card.dto.response.PageResponse;
import com.bank.card.dto.response.UserResponse;
import com.bank.card.entity.User;
import com.bank.card.exception.ResourceNotFoundException;
import com.bank.card.mapper.CardMapper;
import com.bank.card.repository.CardRepository;
import com.bank.card.repository.UserRepository;
import com.bank.card.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final CardRepository cardRepository;
    private final CardMapper cardMapper;

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUserProfile(User currentUser) {
        return buildUserResponse(currentUser);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getAllUsers(Pageable pageable) {
        Page<UserResponse> page = userRepository.findAll(pageable)
                .map(this::buildUserResponse);
        return PageResponse.from(page);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = findUserOrThrow(id);
        return buildUserResponse(user);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        User user = findUserOrThrow(id);
        userRepository.delete(user);
    }

    @Override
    @Transactional
    public UserResponse toggleUserEnabled(Long id, boolean enabled) {
        User user = findUserOrThrow(id);
        user.setEnabled(enabled);
        User saved = userRepository.save(user);
        return buildUserResponse(saved);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private User findUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    private UserResponse buildUserResponse(User user) {
        UserResponse response = cardMapper.toUserResponse(user);
        response.setCardCount(cardRepository.countByOwnerId(user.getId()));
        return response;
    }
}
