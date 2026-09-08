package com.lucasmarques.authapi.controller;

import com.lucasmarques.authapi.dto.UserResponse;
import com.lucasmarques.authapi.entity.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
public class UserController {

    @GetMapping("me")
    public UserResponse getUser(@AuthenticationPrincipal User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getRole(), user.getEmail());
    }
}
