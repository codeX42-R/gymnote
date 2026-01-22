package com.example.gymnote.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.gymnote.domain.User;
import com.example.gymnote.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void register(String email, String rawPassword, String displayName) {

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("すでに登録されているメールアドレスです");
        }

        String hashedPassword = passwordEncoder.encode(rawPassword);

        User user = new User(email, hashedPassword, displayName);
        userRepository.save(user);
    }
}
