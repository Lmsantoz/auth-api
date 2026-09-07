package com.lucasmarques.authapi.dto;

import java.util.UUID;

public record UserRegisteredEvent(UUID id, String username, String email) {
}
