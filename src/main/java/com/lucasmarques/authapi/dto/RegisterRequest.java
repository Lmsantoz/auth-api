package com.lucasmarques.authapi.dto;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;


public record RegisterRequest(
        @NotBlank(message = "The username is required.")
        String userName,

        @NotNull
        @Size(min = 8, message = "Password must contain at least 8 characters.")
        String password,

        @NotBlank(message = "The email is required")
        @Email(message = "The provide a valid email")
        String email
) {}
