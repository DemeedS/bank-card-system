package com.bank.card.controller;

import com.bank.card.dto.response.CardResponse;
import com.bank.card.dto.response.PageResponse;
import com.bank.card.entity.CardStatus;
import com.bank.card.entity.Role;
import com.bank.card.entity.User;
import com.bank.card.exception.CardOperationException;
import com.bank.card.exception.GlobalExceptionHandler;
import com.bank.card.exception.ResourceNotFoundException;
import com.bank.card.security.service.SecurityUtils;
import com.bank.card.service.CardService;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CardController Tests")
class CardControllerTest {

    @Mock private CardService cardService;
    @Mock private SecurityUtils securityUtils;

    @InjectMocks
    private CardController cardController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private User testUser;
    private CardResponse sampleCardResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(cardController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .role(Role.USER)
                .enabled(true)
                .build();

        sampleCardResponse = CardResponse.builder()
                .id(10L)
                .maskedCardNumber("**** **** **** 1234")
                .ownerId(1L)
                .ownerUsername("testuser")
                .status(CardStatus.ACTIVE)
                .balance(new BigDecimal("1000.00"))
                .build();
    }

    @Nested
    @DisplayName("GET /api/v1/cards")
    class GetMyCardsTests {

        @Test
        @DisplayName("Should return 200 with paginated card list")
        void shouldReturn200WithCards() throws Exception {
            PageResponse<CardResponse> pageResponse = PageResponse.<CardResponse>builder()
                    .content(List.of(sampleCardResponse))
                    .page(0).size(10).totalElements(1).totalPages(1)
                    .first(true).last(true)
                    .build();

            when(securityUtils.getCurrentUser()).thenReturn(testUser);
            when(cardService.getMyCards(eq(testUser), isNull(), any(Pageable.class)))
                    .thenReturn(pageResponse);

            mockMvc.perform(get("/api/v1/cards"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content[0].id").value(10))
                    .andExpect(jsonPath("$.content[0].maskedCardNumber").value("**** **** **** 1234"))
                    .andExpect(jsonPath("$.content[0].status").value("ACTIVE"))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("Should filter by status when provided")
        void shouldFilterByStatus() throws Exception {
            PageResponse<CardResponse> pageResponse = PageResponse.<CardResponse>builder()
                    .content(List.of(sampleCardResponse))
                    .page(0).size(10).totalElements(1).totalPages(1)
                    .first(true).last(true)
                    .build();

            when(securityUtils.getCurrentUser()).thenReturn(testUser);
            when(cardService.getMyCards(eq(testUser), eq(CardStatus.ACTIVE), any(Pageable.class)))
                    .thenReturn(pageResponse);

            mockMvc.perform(get("/api/v1/cards").param("status", "ACTIVE"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].status").value("ACTIVE"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/cards/{cardId}")
    class GetCardByIdTests {

        @Test
        @DisplayName("Should return 200 with card details")
        void shouldReturn200WithCard() throws Exception {
            when(securityUtils.getCurrentUser()).thenReturn(testUser);
            when(cardService.getMyCard(10L, testUser)).thenReturn(sampleCardResponse);

            mockMvc.perform(get("/api/v1/cards/10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(10))
                    .andExpect(jsonPath("$.maskedCardNumber").value("**** **** **** 1234"));
        }

        @Test
        @DisplayName("Should return 404 when card not found")
        void shouldReturn404WhenCardNotFound() throws Exception {
            when(securityUtils.getCurrentUser()).thenReturn(testUser);
            when(cardService.getMyCard(99L, testUser))
                    .thenThrow(new ResourceNotFoundException("Card not found with id: 99"));

            mockMvc.perform(get("/api/v1/cards/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Card not found with id: 99"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/cards/{cardId}/request-block")
    class RequestBlockTests {

        @Test
        @DisplayName("Should return 200 with blocked card response")
        void shouldReturn200WhenBlocked() throws Exception {
            CardResponse blockedResponse = CardResponse.builder()
                    .id(10L)
                    .maskedCardNumber("**** **** **** 1234")
                    .status(CardStatus.BLOCKED)
                    .build();

            when(securityUtils.getCurrentUser()).thenReturn(testUser);
            when(cardService.requestBlock(10L, testUser)).thenReturn(blockedResponse);

            mockMvc.perform(post("/api/v1/cards/10/request-block"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("BLOCKED"));
        }

        @Test
        @DisplayName("Should return 400 when card is already blocked")
        void shouldReturn400WhenAlreadyBlocked() throws Exception {
            when(securityUtils.getCurrentUser()).thenReturn(testUser);
            when(cardService.requestBlock(10L, testUser))
                    .thenThrow(new CardOperationException("Card id=10 is already blocked"));

            mockMvc.perform(post("/api/v1/cards/10/request-block"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Card id=10 is already blocked"));
        }
    }
}
