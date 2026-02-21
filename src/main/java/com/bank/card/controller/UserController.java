package com.bank.card.controller;

import com.bank.card.dto.response.UserResponse;
import com.bank.card.entity.User;
import com.bank.card.security.service.SecurityUtils;
import com.bank.card.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User profile endpoints")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;
    private final SecurityUtils securityUtils;

    @GetMapping("/me")
    @Operation(summary = "Get current user profile and card count")
    public ResponseEntity<UserResponse> getMyProfile() {
        User currentUser = securityUtils.getCurrentUser();
        return ResponseEntity.ok(userService.getCurrentUserProfile(currentUser));
    }
}
