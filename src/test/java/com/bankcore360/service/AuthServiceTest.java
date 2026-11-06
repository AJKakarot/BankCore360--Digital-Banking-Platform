package com.bankcore360.service;

import com.bankcore360.dto.CreateUserRequest;
import com.bankcore360.dto.LoginRequest;
import com.bankcore360.dto.LoginResponse;
import com.bankcore360.dto.RegisterRequest;
import com.bankcore360.dto.UserResponse;
import com.bankcore360.entity.AppUser;
import com.bankcore360.entity.UserRole;
import com.bankcore360.exception.DuplicateUserException;
import com.bankcore360.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest("newuser", "newuser@bankcore360.com", "Password123!");
    }

    @Test
    @DisplayName("Should successfully register customer with BCrypt hashed password and CUSTOMER role")
    void shouldRegisterCustomerSuccessfullyWithBcryptPassword() {
        when(appUserRepository.existsByUsername("newuser")).thenReturn(false);
        when(appUserRepository.existsByEmail("newuser@bankcore360.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("$2a$10$hashedBCryptPasswordString");
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(invocation -> {
            AppUser u = invocation.getArgument(0);
            u.setId(101L);
            u.setCreatedAt(LocalDateTime.now());
            return u;
        });

        UserResponse response = authService.register(registerRequest);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(101L);
        assertThat(response.getUsername()).isEqualTo("newuser");
        assertThat(response.getEmail()).isEqualTo("newuser@bankcore360.com");
        assertThat(response.getRole()).isEqualTo(UserRole.CUSTOMER);
        assertThat(response.isEnabled()).isTrue();

        ArgumentCaptor<AppUser> userCaptor = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository).save(userCaptor.capture());
        AppUser savedUser = userCaptor.getValue();
        assertThat(savedUser.getPassword()).isEqualTo("$2a$10$hashedBCryptPasswordString");
        assertThat(savedUser.getRole()).isEqualTo(UserRole.CUSTOMER);
    }

    @Test
    @DisplayName("Should throw DuplicateUserException when username is already taken")
    void shouldRejectRegistrationWhenUsernameAlreadyExists() {
        when(appUserRepository.existsByUsername("newuser")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(DuplicateUserException.class)
                .hasMessageContaining("Username 'newuser' is already taken");

        verify(appUserRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw DuplicateUserException when email is already registered")
    void shouldRejectRegistrationWhenEmailAlreadyExists() {
        when(appUserRepository.existsByUsername("newuser")).thenReturn(false);
        when(appUserRepository.existsByEmail("newuser@bankcore360.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(DuplicateUserException.class)
                .hasMessageContaining("Email 'newuser@bankcore360.com' is already registered");

        verify(appUserRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should authenticate and return LoginResponse on valid credentials")
    void shouldAuthenticateAndReturnLoginResponseOnValidCredentials() {
        LoginRequest loginRequest = new LoginRequest("validuser", "Password123!");
        Authentication mockAuth = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mockAuth);

        AppUser existingUser = new AppUser("validuser", "valid@bankcore360.com", "$2a$10$hashed", UserRole.CUSTOMER);
        existingUser.setId(5L);
        when(appUserRepository.findByUsername("validuser")).thenReturn(Optional.of(existingUser));

        LoginResponse response = authService.login(loginRequest);

        assertThat(response).isNotNull();
        assertThat(response.getMessage()).isEqualTo("Login successful");
        assertThat(response.getUsername()).isEqualTo("validuser");
        assertThat(response.getEmail()).isEqualTo("valid@bankcore360.com");
        assertThat(response.getRole()).isEqualTo(UserRole.CUSTOMER);
        assertThat(response.getId()).isEqualTo(5L);
    }

    @Test
    @DisplayName("Should propagate BadCredentialsException when password is invalid")
    void shouldPropagateBadCredentialsExceptionOnInvalidPassword() {
        LoginRequest loginRequest = new LoginRequest("validuser", "WrongPassword!");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Bad credentials");

        verify(appUserRepository, never()).findByUsername(any());
    }

    @Test
    @DisplayName("Should allow admin to provision privileged user with assigned role")
    void shouldAllowAdminToCreatePrivilegedUser() {
        CreateUserRequest adminRequest = new CreateUserRequest("new_checker", "checker@bankcore360.com", "SecretPass123!", UserRole.CHECKER);
        when(appUserRepository.existsByUsername("new_checker")).thenReturn(false);
        when(appUserRepository.existsByEmail("checker@bankcore360.com")).thenReturn(false);
        when(passwordEncoder.encode("SecretPass123!")).thenReturn("$2a$10$hashedSecret");
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(invocation -> {
            AppUser u = invocation.getArgument(0);
            u.setId(201L);
            return u;
        });

        UserResponse response = authService.createPrivilegedUser(adminRequest);

        assertThat(response.getId()).isEqualTo(201L);
        assertThat(response.getUsername()).isEqualTo("new_checker");
        assertThat(response.getRole()).isEqualTo(UserRole.CHECKER);

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(UserRole.CHECKER);
    }

    @Test
    @DisplayName("Should successfully update role for an existing user")
    void shouldUpdateUserRoleSuccessfully() {
        AppUser existingUser = new AppUser();
        existingUser.setId(55L);
        existingUser.setUsername("test_user");
        existingUser.setEmail("test@bankcore360.com");
        existingUser.setRole(UserRole.CUSTOMER);

        when(appUserRepository.findById(55L)).thenReturn(Optional.of(existingUser));
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = authService.updateUserRole(55L, UserRole.MANAGER);

        assertThat(response.getId()).isEqualTo(55L);
        assertThat(response.getRole()).isEqualTo(UserRole.MANAGER);
        assertThat(existingUser.getRole()).isEqualTo(UserRole.MANAGER);
        verify(appUserRepository).save(existingUser);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when updating role for non-existent user")
    void shouldThrowWhenUpdatingRoleForMissingUser() {
        when(appUserRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.updateUserRole(999L, UserRole.CHECKER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found with id: 999");
    }
}

