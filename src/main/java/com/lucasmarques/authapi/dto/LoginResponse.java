package com.lucasmarques.authapi.dto;

import java.time.Instant;

public record LoginResponse(String token, String userName, Instant expiration) {
}
