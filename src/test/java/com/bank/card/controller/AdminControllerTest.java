package com.bank.card.controller;

import com.bank.card.dto.request.CardCreateRequest;
import com.bank.card.dto.response.CardResponse;
import com.bank.card.dto.response.PageResponse;
import com.bank.card.dto.response.UserResponse;
import com.bank.card.entity.CardStatus;
import com.bank.card.entity.Role;
import com.bank.card.exception.GlobalExceptionHandler;
import com.bank.card.exception.ResourceNotFoundException;
import com.bank.card.service.CardService;
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
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminController Tests")
class AdminControllerTest {

    @Mock private CardService cardService;
    @Mock private UserService userService;

    @InjectMocks
    private AdminController adminController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private CardResponse sampleCard;
    private UserResponse sampleUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(adminController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        sampleCard = CardResponse.builder()
                .id(10L)
                .maskedCardNumber("**** **** **** 1234")
                .ownerId(1L)
                .ownerUsername("testuser")
                .status(CardStatus.ACTIVE)
                .balance(new BigDecimal("500.00"))
                .build();

        sampleUser = UserResponse.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .role(Role.USER)
                .enabled(true)
                .createdAt(OffsetDateTime.now())
                .cardCount(1)
                .build();
    }

    // ─── Card Endpoints ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/v1/admin/cards")
    class CreateCardTests {

        @Test
        @DisplayName("Should return 201 with created card")
        void shouldCreateCard() throws Exception {
            CardCreateRequest request = new CardCreateRequest();
            request.setCardNumber("1234567890123456");
            request.setOwnerId(1L);
            request.setCardholderName("Test User");
            request.setExpiryDate(LocalDate.now().plusYears(2));
            request.setInitialBalance(new BigDecimal("500.00"));

            when(cardService.createCard(any(CardCreateRequest.class))).thenReturn(sampleCard);

            mockMvc.perform(post("/api/v1/admin/cards")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(10))
                    .andExpect(jsonPath("$.maskedCardNumber").value("**** **** **** 1234"))
                    .andExpect(jsonPath("$.status").value("ACTIVE"));
        }

        @Test
        @DisplayName("Should return 400 when card number is invalid")
        void shouldReturn400ForInvalidCardNumber() throws Exception {
            mockMvc.perform(post("/api/v1/admin/cards")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"cardNumber\":\"123\",\"ownerId\":1,\"cardholderName\":\"Test\",\"expiryDate\":\"2028-01-01\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when required fields are missing")
        void shouldReturn400WhenFieldsMissing() throws Exception {
            mockMvc.perform(post("/api/v1/admin/cards")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/cards")
    class GetAllCardsTests {

        @Test
        @DisplayName("Should return 200 with paginated card list")
        void shouldReturnAllCards() throws Exception {
            PageResponse<CardResponse> page = PageResponse.<CardResponse>builder()
                    .content(List.of(sampleCard))
                    .page(0).size(10).totalElements(1).totalPages(1)
                    .first(true).last(true)
                    .build();

            when(cardService.getAllCards(isNull(), any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/api/v1/admin/cards"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(10))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("Should filter cards by status when provided")
        void shouldFilterByStatus() throws Exception {
            PageResponse<CardResponse> page = PageResponse.<CardResponse>builder()
                    .content(List.of(sampleCard))
                    .page(0).size(10).totalElements(1).totalPages(1)
                    .first(true).last(true)
                    .build();

            when(cardService.getAllCards(eq(CardStatus.ACTIVE), any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/api/v1/admin/cards").param("status", "ACTIVE"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].status").value("ACTIVE"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/cards/{cardId}")
    class GetCardByIdTests {

        @Test
        @DisplayName("Should return 200 with card details")
        void shouldReturnCard() throws Exception {
            when(cardService.getCardById(10L)).thenReturn(sampleCard);

            mockMvc.perform(get("/api/v1/admin/cards/10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(10))
                    .andExpect(jsonPath("$.ownerUsername").value("testuser"));
        }

        @Test
        @DisplayName("Should return 404 when card not found")
        void shouldReturn404WhenNotFound() throws Exception {
            when(cardService.getCardById(99L))
                    .thenThrow(new ResourceNotFoundException("Card not found with id: 99"));

            mockMvc.perform(get("/api/v1/admin/cards/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Card not found with id: 99"));
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/admin/cards/{cardId}/status")
    class SetCardStatusTests {

        @Test
        @DisplayName("Should return 200 with updated card status")
        void shouldSetCardStatus() throws Exception {
            CardResponse blocked = CardResponse.builder()
                    .id(10L).maskedCardNumber("**** **** **** 1234")
                    .status(CardStatus.BLOCKED).balance(new BigDecimal("500.00"))
                    .build();

            when(cardService.setCardStatus(10L, CardStatus.BLOCKED)).thenReturn(blocked);

            mockMvc.perform(patch("/api/v1/admin/cards/10/status")
                            .param("status", "BLOCKED"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("BLOCKED"));
        }

        @Test
        @DisplayName("Should return 404 when card not found")
        void shouldReturn404WhenCardNotFound() throws Exception {
            when(cardService.setCardStatus(99L, CardStatus.ACTIVE))
                    .thenThrow(new ResourceNotFoundException("Card not found with id: 99"));

            mockMvc.perform(patch("/api/v1/admin/cards/99/status")
                            .param("status", "ACTIVE"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/admin/cards/{cardId}")
    class DeleteCardTests {

        @Test
        @DisplayName("Should return 204 on successful delete")
        void shouldDeleteCard() throws Exception {
            doNothing().when(cardService).deleteCard(10L);

            mockMvc.perform(delete("/api/v1/admin/cards/10"))
                    .andExpect(status().isNoContent());

            verify(cardService).deleteCard(10L);
        }

        @Test
        @DisplayName("Should return 404 when card not found")
        void shouldReturn404WhenNotFound() throws Exception {
            doThrow(new ResourceNotFoundException("Card not found with id: 99"))
                    .when(cardService).deleteCard(99L);

            mockMvc.perform(delete("/api/v1/admin/cards/99"))
                    .andExpect(status().isNotFound());
        }
    }

    // ─── User Endpoints ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/v1/admin/users")
    class GetAllUsersTests {

        @Test
        @DisplayName("Should return 200 with paginated user list")
        void shouldReturnAllUsers() throws Exception {
            PageResponse<UserResponse> page = PageResponse.<UserResponse>builder()
                    .content(List.of(sampleUser))
                    .page(0).size(10).totalElements(1).totalPages(1)
                    .first(true).last(true)
                    .build();

            when(userService.getAllUsers(any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/api/v1/admin/users"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(1))
                    .andExpect(jsonPath("$.content[0].username").value("testuser"))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/users/{userId}")
    class GetUserByIdTests {

        @Test
        @DisplayName("Should return 200 with user details")
        void shouldReturnUser() throws Exception {
            when(userService.getUserById(1L)).thenReturn(sampleUser);

            mockMvc.perform(get("/api/v1/admin/users/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.email").value("test@example.com"));
        }

        @Test
        @DisplayName("Should return 404 when user not found")
        void shouldReturn404WhenNotFound() throws Exception {
            when(userService.getUserById(99L))
                    .thenThrow(new ResourceNotFoundException("User not found with id: 99"));

            mockMvc.perform(get("/api/v1/admin/users/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("User not found with id: 99"));
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/admin/users/{userId}/enable")
    class ToggleUserEnabledTests {

        @Test
        @DisplayName("Should return 200 when disabling a user")
        void shouldDisableUser() throws Exception {
            UserResponse disabled = UserResponse.builder()
                    .id(1L).username("testuser").email("test@example.com")
                    .role(Role.USER).enabled(false).createdAt(OffsetDateTime.now()).cardCount(1)
                    .build();

            when(userService.toggleUserEnabled(1L, false)).thenReturn(disabled);

            mockMvc.perform(patch("/api/v1/admin/users/1/enable")
                            .param("enabled", "false"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.enabled").value(false));
        }

        @Test
        @DisplayName("Should return 200 when enabling a user")
        void shouldEnableUser() throws Exception {
            when(userService.toggleUserEnabled(1L, true)).thenReturn(sampleUser);

            mockMvc.perform(patch("/api/v1/admin/users/1/enable")
                            .param("enabled", "true"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.enabled").value(true));
        }

        @Test
        @DisplayName("Should return 404 when user not found")
        void shouldReturn404WhenNotFound() throws Exception {
            when(userService.toggleUserEnabled(99L, true))
                    .thenThrow(new ResourceNotFoundException("User not found with id: 99"));

            mockMvc.perform(patch("/api/v1/admin/users/99/enable")
                            .param("enabled", "true"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/admin/users/{userId}")
    class DeleteUserTests {

        @Test
        @DisplayName("Should return 204 on successful delete")
        void shouldDeleteUser() throws Exception {
            doNothing().when(userService).deleteUser(1L);

            mockMvc.perform(delete("/api/v1/admin/users/1"))
                    .andExpect(status().isNoContent());

            verify(userService).deleteUser(1L);
        }

        @Test
        @DisplayName("Should return 404 when user not found")
        void shouldReturn404WhenNotFound() throws Exception {
            doThrow(new ResourceNotFoundException("User not found with id: 99"))
                    .when(userService).deleteUser(99L);

            mockMvc.perform(delete("/api/v1/admin/users/99"))
                    .andExpect(status().isNotFound());
        }
    }
}
