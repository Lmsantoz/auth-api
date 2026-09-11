package com.lucasmarques.authapi.service;

import com.lucasmarques.authapi.config.RabbitMQConfiguration;
import com.lucasmarques.authapi.dto.LoginRequest;
import com.lucasmarques.authapi.dto.LoginResponse;
import com.lucasmarques.authapi.dto.RegisterRequest;
import com.lucasmarques.authapi.dto.UserRegisteredEvent;
import com.lucasmarques.authapi.entity.User;
import com.lucasmarques.authapi.enums.UserRole;
import com.lucasmarques.authapi.repository.UserRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {
    @InjectMocks
    AuthService authService;

    @Mock
    UserRepository userRepository;

    @Mock
    TokenService tokenService;

    @Mock
    AuthenticationManager authenticationManager;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    RabbitTemplate rabbitTemplate;

    @Mock
    Authentication authentication;

    @Test
    public void verifyHashBeforeSaving() {
        RegisterRequest registerRequest = new RegisterRequest("Lucas", "23sse32", "lucas@gmail.com");
        when(passwordEncoder.encode(registerRequest.password())).thenReturn("hash-fake");
        authService.register(registerRequest);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User user = captor.getValue();

        Assertions.assertEquals("hash-fake", user.getPassword());
    }

    @Test
    public void verifyRoleUserBeforeSaving() {
        RegisterRequest registerRequest = new RegisterRequest("Pedro", "1isn1478", "pedro@gmail.com");
        authService.register(registerRequest);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User user = captor.getValue();

        Assertions.assertEquals(UserRole.CLIENT, user.getRole());
    }

    @Test
    public void rabbitCorrectlyExchange() {
        RegisterRequest registerRequest = new RegisterRequest("Ana", "winm3283-24n", "ana@gmail.com");
        authService.register(registerRequest);
        verify(rabbitTemplate).convertAndSend(eq(RabbitMQConfiguration.EXCHANGE_NAME), eq(""), any(UserRegisteredEvent.class));
    }

    @Test
    public void verifySuccessfullyLogin() {
        LoginRequest loginRequest = new LoginRequest("Ana Julia", "dn2180e9");
        User user = new User();
        user.setUserName(loginRequest.userName());

        when(authentication.getPrincipal()).thenReturn(user);

        when(authenticationManager.authenticate(ArgumentMatchers.any(Authentication.class))).thenReturn(authentication);

        when(tokenService.generateToken(user)).thenReturn("fake token");

        LoginResponse loginResponse = authService.login(loginRequest);

        Assertions.assertEquals("fake token", loginResponse.token());
    }
}
