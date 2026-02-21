package com.bank.card.security.service;

import com.bank.card.entity.User;
import com.bank.card.exception.ResourceNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtils {

    /**
     * Extracts the currently authenticated User from the Spring Security context.
     * Since our UserDetails implementation IS the User entity, this cast is safe.
     */
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResourceNotFoundException("No authenticated user found");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof User user) {
            return user;
        }
        throw new ResourceNotFoundException("Unable to resolve current user");
    }
}
