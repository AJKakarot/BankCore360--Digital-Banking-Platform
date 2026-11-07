package com.bankcore360.config;

import com.bankcore360.entity.AppUser;
import com.bankcore360.entity.UserRole;
import com.bankcore360.repository.AppUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class SecurityDataInitializer implements CommandLineRunner {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${ADMIN_USERNAME:admin}")
    private String adminUsername;

    @Value("${ADMIN_EMAIL:admin@bankcore360.com}")
    private String adminEmail;

    @Value("${ADMIN_PASSWORD:AdminPassword123!}")
    private String adminPassword;

    @Value("${DEV_USER_PASSWORD:Password@123}")
    private String devPassword;

    public SecurityDataInitializer(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // Bootstrap initial administrator account
        seedOrUpdateUser(adminUsername, adminEmail, adminPassword, UserRole.ADMIN);

        // Seed or sync development and test users with Password@123
        seedOrUpdateUser("dev_customer", "dev_customer@bankcore360.com", devPassword, UserRole.CUSTOMER);
        seedOrUpdateUser("dev_checker", "dev_checker@bankcore360.com", devPassword, UserRole.CHECKER);
        seedOrUpdateUser("dev_manager", "dev_manager@bankcore360.com", devPassword, UserRole.MANAGER);
        seedOrUpdateUser("dev_admin", "dev_admin@bankcore360.com", devPassword, UserRole.ADMIN);
    }

    private void seedOrUpdateUser(String username, String email, String rawPassword, UserRole role) {
        if (appUserRepository.findByUsername(username).isPresent()) {
            return;
        }

        AppUser newUser = new AppUser();
        newUser.setUsername(username);
        newUser.setEmail(email);
        newUser.setPassword(passwordEncoder.encode(rawPassword));
        newUser.setRole(role);
        newUser.setEnabled(true);
        appUserRepository.save(newUser);
    }
}
