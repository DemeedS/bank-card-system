package com.bank.card.service;

import com.bank.card.dto.request.UserUpdateRequest;
import com.bank.card.dto.response.PageResponse;
import com.bank.card.dto.response.UserResponse;
import com.bank.card.entity.Role;
import com.bank.card.entity.User;
import com.bank.card.exception.ConflictException;
import com.bank.card.exception.ResourceNotFoundException;
import com.bank.card.mapper.CardMapper;
import com.bank.card.repository.CardRepository;
import com.bank.card.repository.UserRepository;
import com.bank.card.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl Tests")
class UserServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private CardRepository cardRepository;
    @Mock private CardMapper cardMapper;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;
    private UserResponse baseResponse;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("encoded-password")
                .role(Role.USER)
                .enabled(true)
                .createdAt(OffsetDateTime.now())
                .build();

        baseResponse = UserResponse.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .role(Role.USER)
                .enabled(true)
                .createdAt(testUser.getCreatedAt())
                .cardCount(0)
                .build();
    }

    @Nested
    @DisplayName("getCurrentUserProfile")
    class GetCurrentUserProfileTests {

        @Test
        @DisplayName("Should return UserResponse with cardCount populated")
        void shouldReturnProfileWithCardCount() {
            when(cardMapper.toUserResponse(testUser)).thenReturn(baseResponse);
            when(cardRepository.countByOwnerId(1L)).thenReturn(3L);

            UserResponse result = userService.getCurrentUserProfile(testUser);

            assertThat(result).isNotNull();
            assertThat(result.getCardCount()).isEqualTo(3L);
            assertThat(result.getUsername()).isEqualTo("testuser");
        }
    }

    @Nested
    @DisplayName("updateCurrentUser")
    class UpdateCurrentUserTests {

        private UserUpdateRequest request;

        @BeforeEach
        void setUp() {
            request = new UserUpdateRequest();
        }

        @Test
        @DisplayName("Should update email when new email is not taken")
        void shouldUpdateEmailSuccessfully() {
            ReflectionTestUtils.setField(request, "email", "new@example.com");
            when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
            when(userRepository.save(testUser)).thenReturn(testUser);
            when(cardMapper.toUserResponse(testUser)).thenReturn(baseResponse);
            when(cardRepository.countByOwnerId(1L)).thenReturn(0L);

            userService.updateCurrentUser(testUser, request);

            assertThat(testUser.getEmail()).isEqualTo("new@example.com");
            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("Should throw ConflictException when new email is already taken")
        void shouldThrowConflictWhenEmailTaken() {
            ReflectionTestUtils.setField(request, "email", "taken@example.com");
            when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

            assertThatThrownBy(() -> userService.updateCurrentUser(testUser, request))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("taken@example.com");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should not check uniqueness when email is unchanged")
        void shouldSkipCheckWhenEmailUnchanged() {
            ReflectionTestUtils.setField(request, "email", "test@example.com");
            when(userRepository.save(testUser)).thenReturn(testUser);
            when(cardMapper.toUserResponse(testUser)).thenReturn(baseResponse);
            when(cardRepository.countByOwnerId(1L)).thenReturn(0L);

            userService.updateCurrentUser(testUser, request);

            verify(userRepository, never()).existsByEmail(any());
            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("Should encode and update password when provided")
        void shouldUpdatePasswordSuccessfully() {
            ReflectionTestUtils.setField(request, "password", "newpassword123");
            when(passwordEncoder.encode("newpassword123")).thenReturn("new-encoded");
            when(userRepository.save(testUser)).thenReturn(testUser);
            when(cardMapper.toUserResponse(testUser)).thenReturn(baseResponse);
            when(cardRepository.countByOwnerId(1L)).thenReturn(0L);

            userService.updateCurrentUser(testUser, request);

            assertThat(testUser.getPassword()).isEqualTo("new-encoded");
            verify(passwordEncoder).encode("newpassword123");
        }

        @Test
        @DisplayName("Should not update password when field is null")
        void shouldSkipPasswordWhenNull() {
            when(userRepository.save(testUser)).thenReturn(testUser);
            when(cardMapper.toUserResponse(testUser)).thenReturn(baseResponse);
            when(cardRepository.countByOwnerId(1L)).thenReturn(0L);

            userService.updateCurrentUser(testUser, request);

            verify(passwordEncoder, never()).encode(any());
            assertThat(testUser.getPassword()).isEqualTo("encoded-password");
        }

        @Test
        @DisplayName("Should not update password when field is blank")
        void shouldSkipPasswordWhenBlank() {
            ReflectionTestUtils.setField(request, "password", "   ");
            when(userRepository.save(testUser)).thenReturn(testUser);
            when(cardMapper.toUserResponse(testUser)).thenReturn(baseResponse);
            when(cardRepository.countByOwnerId(1L)).thenReturn(0L);

            userService.updateCurrentUser(testUser, request);

            verify(passwordEncoder, never()).encode(any());
        }
    }

    @Nested
    @DisplayName("getAllUsers")
    class GetAllUsersTests {

        @Test
        @DisplayName("Should return paginated UserResponse list")
        void shouldReturnPaginatedUsers() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<User> page = new PageImpl<>(List.of(testUser), pageable, 1);

            when(userRepository.findAll(pageable)).thenReturn(page);
            when(cardMapper.toUserResponse(testUser)).thenReturn(baseResponse);
            when(cardRepository.countByOwnerId(1L)).thenReturn(2L);

            PageResponse<UserResponse> result = userService.getAllUsers(pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getPage()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("getUserById")
    class GetUserByIdTests {

        @Test
        @DisplayName("Should return UserResponse when user exists")
        void shouldReturnUserWhenFound() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(cardMapper.toUserResponse(testUser)).thenReturn(baseResponse);
            when(cardRepository.countByOwnerId(1L)).thenReturn(0L);

            UserResponse result = userService.getUserById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when user not found")
        void shouldThrowWhenUserNotFound() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUserById(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("99");
        }
    }

    @Nested
    @DisplayName("deleteUser")
    class DeleteUserTests {

        @Test
        @DisplayName("Should delete user when found")
        void shouldDeleteUserWhenFound() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            userService.deleteUser(1L);

            verify(userRepository).delete(testUser);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when user not found")
        void shouldThrowWhenUserNotFound() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.deleteUser(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("99");

            verify(userRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("toggleUserEnabled")
    class ToggleUserEnabledTests {

        @Test
        @DisplayName("Should disable a user account")
        void shouldDisableUser() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.save(testUser)).thenReturn(testUser);
            when(cardMapper.toUserResponse(testUser)).thenReturn(baseResponse);
            when(cardRepository.countByOwnerId(1L)).thenReturn(0L);

            userService.toggleUserEnabled(1L, false);

            assertThat(testUser.isEnabled()).isFalse();
            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("Should enable a disabled user account")
        void shouldEnableUser() {
            testUser.setEnabled(false);
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.save(testUser)).thenReturn(testUser);
            when(cardMapper.toUserResponse(testUser)).thenReturn(baseResponse);
            when(cardRepository.countByOwnerId(1L)).thenReturn(0L);

            userService.toggleUserEnabled(1L, true);

            assertThat(testUser.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when user not found")
        void shouldThrowWhenUserNotFound() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.toggleUserEnabled(99L, true))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("99");
        }
    }
}
