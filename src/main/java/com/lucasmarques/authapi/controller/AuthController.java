package com.lucasmarques.authapi.controller;

import com.lucasmarques.authapi.dto.LoginRequest;
import com.lucasmarques.authapi.dto.LoginResponse;
import com.lucasmarques.authapi.dto.RegisterRequest;
import com.lucasmarques.authapi.service.AuthService;
import com.lucasmarques.authapi.service.JpaUserDetailsService;
import com.lucasmarques.authapi.service.TokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RequestMapping("/auth")
@RestController
public class AuthController {

    private TokenService tokenService;

    private JpaUserDetailsService authenticationService;

    private final AuthService authService;

    private AuthenticationManager authenticationManager;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public void registerUser(@RequestBody @Valid RegisterRequest registerRequest) {
        authService.register(registerRequest);
    }

    @PostMapping("login")
    @ResponseStatus(HttpStatus.OK)
    public LoginResponse login(@RequestBody @Valid LoginRequest loginRequest) {
        return authService.login(loginRequest);
    }


}
