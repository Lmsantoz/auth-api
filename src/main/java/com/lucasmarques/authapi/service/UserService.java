package com.lucasmarques.authapi.service;

import com.lucasmarques.authapi.entity.User;
import com.lucasmarques.authapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class UserService {

   private final UserRepository userRepository;

   public User findByUserName(String userName){
       return userRepository.findByUserName(userName)
               .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}
