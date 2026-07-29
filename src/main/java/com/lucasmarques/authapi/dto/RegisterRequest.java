package com.lucasmarques.authapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "O Username é obrigatório")
        String userName,

        @NotNull
        @Size(min = 8, message = "Senha deve conter no mínimo 8 caracteres")
        String password
) {}
