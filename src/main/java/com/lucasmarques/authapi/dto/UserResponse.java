package com.lucasmarques.authapi.dto;

import com.lucasmarques.authapi.enums.UserRole;

import java.util.UUID;

public record UserResponse (UUID id, String username, UserRole role, String email) {
}
