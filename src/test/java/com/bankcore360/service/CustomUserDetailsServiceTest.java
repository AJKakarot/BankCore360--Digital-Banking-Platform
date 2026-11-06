package com.bankcore360.service;

import com.bankcore360.entity.AppUser;
import com.bankcore360.entity.UserRole;
import com.bankcore360.repository.AppUserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    @Test
    @DisplayName("Should successfully load user by username and map role to Spring Security authority")
    void shouldLoadUserByUsernameSuccessfully() {
        AppUser user = new AppUser("testchecker", "checker@example.com", "$2a$10$encoded", UserRole.CHECKER, true);
        when(appUserRepository.findByUsername("testchecker")).thenReturn(Optional.of(user));

        UserDetails userDetails = userDetailsService.loadUserByUsername("testchecker");

        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("testchecker");
        assertThat(userDetails.getPassword()).isEqualTo("$2a$10$encoded");
        assertThat(userDetails.isEnabled()).isTrue();
        assertThat(userDetails.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_CHECKER");
    }

    @Test
    @DisplayName("Should correctly map all UserRole enum values to ROLE_ authorities")
    void shouldMapRolesToAuthoritiesWithRolePrefix() {
        for (UserRole role : UserRole.values()) {
            AppUser user = new AppUser("user_" + role.name(), "user@example.com", "pass", role, true);
            when(appUserRepository.findByUsername("user_" + role.name())).thenReturn(Optional.of(user));

            UserDetails userDetails = userDetailsService.loadUserByUsername("user_" + role.name());

            assertThat(userDetails.getAuthorities())
                    .extracting(GrantedAuthority::getAuthority)
                    .containsExactly("ROLE_" + role.name());
        }
    }

    @Test
    @DisplayName("Should throw UsernameNotFoundException when user is not present in database")
    void shouldThrowUsernameNotFoundExceptionWhenUserDoesNotExist() {
        when(appUserRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("unknown"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found with username: unknown");
    }

    @Test
    @DisplayName("Should mark UserDetails as disabled when user.enabled is false")
    void shouldReflectDisabledStatusWhenUserNotEnabled() {
        AppUser disabledUser = new AppUser("disabled_user", "disabled@example.com", "pass", UserRole.CUSTOMER, false);
        when(appUserRepository.findByUsername("disabled_user")).thenReturn(Optional.of(disabledUser));

        UserDetails userDetails = userDetailsService.loadUserByUsername("disabled_user");

        assertThat(userDetails.isEnabled()).isFalse();
    }
}

