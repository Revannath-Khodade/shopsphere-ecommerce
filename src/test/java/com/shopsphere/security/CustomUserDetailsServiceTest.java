package com.shopsphere.security;

import com.shopsphere.entity.Role;
import com.shopsphere.entity.User;
import com.shopsphere.entity.enums.RoleName;
import com.shopsphere.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    private User user;

    @BeforeEach
    void setUp() {
        Set<Role> roles = new HashSet<>();
        roles.add(Role.builder().id(1L).name(RoleName.ROLE_CUSTOMER).build());

        user = User.builder()
                .id(10L)
                .username("amit_verma")
                .email("amit@example.com")
                .password("hashed-password")
                .enabled(true)
                .accountNonLocked(true)
                .roles(roles)
                .build();
    }

    @Test
    void loadUserByUsername_shouldReturnCustomUserDetails_withMappedAuthorities() {
        when(userRepository.findByUsernameOrEmail("amit_verma", "amit_verma")).thenReturn(Optional.of(user));

        UserDetails result = userDetailsService.loadUserByUsername("amit_verma");

        assertThat(result).isInstanceOf(CustomUserDetails.class);
        assertThat(result.getUsername()).isEqualTo("amit_verma");
        assertThat(result.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_CUSTOMER");
    }

    @Test
    void loadUserByUsername_shouldWorkWithEmailIdentifier() {
        when(userRepository.findByUsernameOrEmail("amit@example.com", "amit@example.com")).thenReturn(Optional.of(user));

        UserDetails result = userDetailsService.loadUserByUsername("amit@example.com");

        assertThat(result.getUsername()).isEqualTo("amit_verma");
    }

    @Test
    void loadUserByUsername_shouldThrowUsernameNotFoundException_whenNoMatch() {
        when(userRepository.findByUsernameOrEmail(anyString(), anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("unknown_user"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void loadUserById_shouldReturnCustomUserDetails_whenUserExists() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));

        UserDetails result = userDetailsService.loadUserById(10L);

        assertThat(result.getUsername()).isEqualTo("amit_verma");
    }

    @Test
    void loadUserById_shouldThrowUsernameNotFoundException_whenUserDoesNotExist() {
        when(userRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserById(404L))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
