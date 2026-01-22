package com.example.gymnote.security;

import com.example.gymnote.domain.User;
import com.example.gymnote.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class GymnoteUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public GymnoteUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("ユーザーが見つかりません"));

        return new GymnoteUserDetails(user);
    }
}
