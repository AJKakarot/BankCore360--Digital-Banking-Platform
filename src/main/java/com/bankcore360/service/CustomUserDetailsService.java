package com.bankcore360.service;

import com.bankcore360.entity.AppUser;
import com.bankcore360.repository.AppUserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final AppUserRepository appUserRepository;

    public CustomUserDetailsService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String cleanIdentifier = username != null ? username.trim() : "";
        AppUser appUser = appUserRepository.findByUsername(cleanIdentifier)
                .or(() -> appUserRepository.findByEmail(cleanIdentifier.toLowerCase()))
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        return User.builder()
                .username(appUser.getUsername())
                .password(appUser.getPassword())
                .disabled(!appUser.isEnabled())
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + appUser.getRole().name())))
                .build();
    }
}

