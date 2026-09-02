package com.lucasmarques.authapi.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.lucasmarques.authapi.entity.User;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class TokenService {

    @Value("${api.security.token.secret}")
    private String secret;

    private Algorithm algorithm;

    @PostConstruct
    public void init() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        this.algorithm = Algorithm.HMAC256(keyBytes);
    }


    public String generateToken (User user) {
        try {
            String token = JWT.create()
                    .withIssuer("authapi")
                    .withSubject(String.valueOf(user.getId()))
                    .withExpiresAt(getExpirationTime())
                    .sign(algorithm);
            return token;
        } catch(JWTCreationException exception) {
                throw new RuntimeException("Error while generation token", exception);
        }
    }

    public String validateToken(String token) {
        try {
            return JWT.require(algorithm)
                    .withIssuer("authapi")
                    .build()
                    .verify(token)
                    .getSubject();
        } catch (JWTVerificationException exception) {
            throw new RuntimeException("Error while validating token", exception);
        }
    }

    public Instant getExpirationTime () {
        return LocalDateTime.now().plusMinutes(2).toInstant(ZoneOffset.of("-03:00"));
    }
}
