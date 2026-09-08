package com.lucasmarques.authapi.service;

import com.lucasmarques.authapi.config.RabbitMQConfiguration;
import com.lucasmarques.authapi.dto.LoginRequest;
import com.lucasmarques.authapi.dto.LoginResponse;
import com.lucasmarques.authapi.dto.RegisterRequest;
import com.lucasmarques.authapi.dto.UserRegisteredEvent;
import com.lucasmarques.authapi.entity.User;
import com.lucasmarques.authapi.enums.UserRole;
import com.lucasmarques.authapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class AuthService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final TokenService tokenService;

    private final AuthenticationManager authenticationManager;

    private final RabbitTemplate rabbitTemplate;

    public void register(RegisterRequest registerRequest) {
            User user = new User();
            user.setUserName(registerRequest.userName());
            user.setPassword(passwordEncoder.encode(registerRequest.password()));
            user.setRole(UserRole.CLIENT);
            user.setEmail(registerRequest.email());
            userRepository.save(user);
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfiguration.EXCHANGE_NAME, "", new UserRegisteredEvent(user.getId(), user.getUsername(), user.getEmail()));
        } catch (AmqpException ex) {

        }
    }

    public LoginResponse login(LoginRequest loginRequest) {
        UsernamePasswordAuthenticationToken credentials =  new UsernamePasswordAuthenticationToken(loginRequest.userName(),loginRequest.password());

        var validateUser = authenticationManager.authenticate(credentials);

        User user = (User) validateUser.getPrincipal();

        String token = tokenService.generateToken(user);

        return new LoginResponse(token, user.getUsername(), tokenService.getExpirationTime());
    }
}
