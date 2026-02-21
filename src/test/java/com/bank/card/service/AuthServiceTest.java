package com.bank.card.service;

import com.bank.card.dto.request.AuthRequest;
import com.bank.card.dto.response.AuthResponse;
import com.bank.card.entity.Role;
import com.bank.card.entity.User;
import com.bank.card.exception.ConflictException;
import com.bank.card.repository.UserRepository;
import com.bank.card.security.jwt.JwtService;
import com.bank.card.service.impl.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Tests")
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "jwtExpiration", 86400000L);
    }

    @Nested
    @DisplayName("Register")
    class RegisterTests {

        private AuthRequest.Register validRequest;

        @BeforeEach
        void setUp() {
            validRequest = new AuthRequest.Register();
            ReflectionTestUtils.setField(validRequest, "username", "testuser");
            ReflectionTestUtils.setField(validRequest, "email", "test@example.com");
            ReflectionTestUtils.setField(validRequest, "password", "password123");
        }

        @Test
        @DisplayName("Should register new user and return JWT token")
        void shouldRegisterSuccessfully() {
            // Arrange
            when(userRepository.existsByUsername("testuser")).thenReturn(false);
            when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("$2a$hashed");
            when(userRepository.save(any(User.class))).thenAnswer(i -> {
                User u = i.getArgument(0);
                ReflectionTestUtils.setField(u, "id", 1L);
                return u;
            });
            when(jwtService.generateToken(any(User.class))).thenReturn("jwt.token.here");

            // Act
            AuthResponse response = authService.register(validRequest);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getToken()).isEqualTo("jwt.token.here");
            assertThat(response.getUsername()).isEqualTo("testuser");
            assertThat(response.getRole()).isEqualTo(Role.USER.name());
            assertThat(response.getExpiresIn()).isEqualTo(86400000L);

            verify(userRepository).save(argThat(user ->
                    user.getUsername().equals("testuser") &&
                    user.getEmail().equals("test@example.com") &&
                    user.getRole() == Role.USER &&
                    user.isEnabled()
            ));
        }

        @Test
        @DisplayName("Should throw ConflictException when username already taken")
        void shouldThrowWhenUsernameTaken() {
            when(userRepository.existsByUsername("testuser")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(validRequest))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("testuser");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw ConflictException when email already registered")
        void shouldThrowWhenEmailTaken() {
            when(userRepository.existsByUsername("testuser")).thenReturn(false);
            when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(validRequest))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("test@example.com");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should encode password before saving")
        void shouldEncodePassword() {
            when(userRepository.existsByUsername(any())).thenReturn(false);
            when(userRepository.existsByEmail(any())).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("encoded_pass");
            when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(jwtService.generateToken(any())).thenReturn("token");

            authService.register(validRequest);

            verify(userRepository).save(argThat(user ->
                    user.getPassword().equals("encoded_pass")
            ));
        }
    }

    @Nested
    @DisplayName("Login")
    class LoginTests {

        private AuthRequest.Login loginRequest;
        private User existingUser;

        @BeforeEach
        void setUp() {
            loginRequest = new AuthRequest.Login();
            ReflectionTestUtils.setField(loginRequest, "username", "testuser");
            ReflectionTestUtils.setField(loginRequest, "password", "password123");

            existingUser = User.builder()
                    .username("testuser")
                    .email("test@example.com")
                    .password("$2a$encoded")
                    .role(Role.USER)
                    .enabled(true)
                    .build();
        }

        @Test
        @DisplayName("Should return JWT token on successful login")
        void shouldLoginSuccessfully() {
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(existingUser));
            when(jwtService.generateToken(existingUser)).thenReturn("valid.jwt.token");

            AuthResponse response = authService.login(loginRequest);

            assertThat(response.getToken()).isEqualTo("valid.jwt.token");
            assertThat(response.getUsername()).isEqualTo("testuser");
            assertThat(response.getRole()).isEqualTo("USER");

            verify(authenticationManager).authenticate(
                    any(UsernamePasswordAuthenticationToken.class)
            );
        }

        @Test
        @DisplayName("Should throw BadCredentialsException on wrong password")
        void shouldThrowOnWrongPassword() {
            doThrow(new BadCredentialsException("Bad credentials"))
                    .when(authenticationManager).authenticate(any());

            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(BadCredentialsException.class);
        }
    }
}
