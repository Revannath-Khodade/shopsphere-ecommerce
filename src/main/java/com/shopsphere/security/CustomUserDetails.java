package com.shopsphere.security;

import com.shopsphere.entity.Role;
import com.shopsphere.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Spring Security's view of a ShopSphere {@link User}. Wraps the entity
 * rather than duplicating its fields, and translates {@link Role} entities
 * into {@link GrantedAuthority} instances (role names are already stored
 * with the conventional "ROLE_" prefix, so they map straight across).
 */
@Getter
public class CustomUserDetails implements UserDetails {

    private final Long id;
    private final String username;
    private final String email;
    private final String password;
    private final boolean enabled;
    private final boolean accountNonLocked;
    private final Collection<? extends GrantedAuthority> authorities;

    public CustomUserDetails(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.password = user.getPassword();
        this.enabled = Boolean.TRUE.equals(user.getEnabled());
        this.accountNonLocked = Boolean.TRUE.equals(user.getAccountNonLocked());
        this.authorities = mapAuthorities(user.getRoles());
    }

    private static Collection<? extends GrantedAuthority> mapAuthorities(Set<Role> roles) {
        if (roles == null) {
            return Set.of();
        }
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority(role.getName().name()))
                .collect(Collectors.toSet());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // account expiry is not modeled in this domain
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // credential expiry is not modeled in this domain
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
