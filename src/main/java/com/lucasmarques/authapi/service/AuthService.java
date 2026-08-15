package com.lucasmarques.authapi.service;

import com.lucasmarques.authapi.dto.RegisterRequest;
import com.lucasmarques.authapi.entity.User;
import com.lucasmarques.authapi.enums.UserRole;
import com.lucasmarques.authapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class AuthService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    public void register(RegisterRequest registerRequest) {
        User user = new User();
        user.setUserName(registerRequest.name());
        user.setPassword(passwordEncoder.encode(registerRequest.password()));
        user.setRole(UserRole.CLIENT);
        userRepository.save(user);
    }
}
