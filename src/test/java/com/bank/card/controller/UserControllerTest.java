package com.bank.card.controller;

import com.bank.card.dto.request.UserUpdateRequest;
import com.bank.card.dto.response.UserResponse;
import com.bank.card.entity.Role;
import com.bank.card.entity.User;
import com.bank.card.exception.ConflictException;
import com.bank.card.exception.GlobalExceptionHandler;
import com.bank.card.security.service.SecurityUtils;
import com.bank.card.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserController Tests")
class UserControllerTest {

    @Mock private UserService userService;
    @Mock private SecurityUtils securityUtils;

    @InjectMocks
    private UserController userController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private User testUser;
    private UserResponse baseUserResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("old@example.com")
                .role(Role.USER)
                .enabled(true)
                .build();

        baseUserResponse = UserResponse.builder()
                .id(1L)
                .username("testuser")
                .email("old@example.com")
                .role(Role.USER)
                .enabled(true)
                .createdAt(OffsetDateTime.now())
                .cardCount(2)
                .build();
    }

    @Nested
    @DisplayName("PATCH /api/v1/users/me")
    class UpdateMyProfileTests {

        @Test
        @DisplayName("Should return 200 with updated email")
        void shouldUpdateEmailSuccessfully() throws Exception {
            UserUpdateRequest request = new UserUpdateRequest();
            ReflectionTestUtils.setField(request, "email", "new@example.com");

            UserResponse updated = UserResponse.builder()
                    .id(1L).username("testuser").email("new@example.com")
                    .role(Role.USER).enabled(true).createdAt(OffsetDateTime.now()).cardCount(2)
                    .build();

            when(securityUtils.getCurrentUser()).thenReturn(testUser);
            when(userService.updateCurrentUser(eq(testUser), any(UserUpdateRequest.class))).thenReturn(updated);

            mockMvc.perform(patch("/api/v1/users/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("new@example.com"))
                    .andExpect(jsonPath("$.username").value("testuser"));
        }

        @Test
        @DisplayName("Should return 200 when updating password")
        void shouldUpdatePasswordSuccessfully() throws Exception {
            UserUpdateRequest request = new UserUpdateRequest();
            ReflectionTestUtils.setField(request, "password", "newpassword123");

            when(securityUtils.getCurrentUser()).thenReturn(testUser);
            when(userService.updateCurrentUser(eq(testUser), any(UserUpdateRequest.class))).thenReturn(baseUserResponse);

            mockMvc.perform(patch("/api/v1/users/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1));
        }

        @Test
        @DisplayName("Should return 409 when email is already taken")
        void shouldReturn409WhenEmailTaken() throws Exception {
            UserUpdateRequest request = new UserUpdateRequest();
            ReflectionTestUtils.setField(request, "email", "taken@example.com");

            when(securityUtils.getCurrentUser()).thenReturn(testUser);
            when(userService.updateCurrentUser(eq(testUser), any(UserUpdateRequest.class)))
                    .thenThrow(new ConflictException("Email already in use: taken@example.com"));

            mockMvc.perform(patch("/api/v1/users/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("Email already in use: taken@example.com"));
        }

        @Test
        @DisplayName("Should return 400 when email format is invalid")
        void shouldReturn400ForInvalidEmail() throws Exception {
            mockMvc.perform(patch("/api/v1/users/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\": \"not-an-email\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when password is too short")
        void shouldReturn400ForShortPassword() throws Exception {
            mockMvc.perform(patch("/api/v1/users/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"password\": \"short\"}"))
                    .andExpect(status().isBadRequest());
        }
    }
}
