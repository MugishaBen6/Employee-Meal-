package com.emeal.security;

import com.emeal.entity.User;
import com.emeal.entity.UserStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class UserPrincipal implements UserDetails {

    private Long id;
    private String username;
    private String email;
    private String password;
    private UserStatus status;
    private Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal() {
    }

    public UserPrincipal(Long id, String username, String email, String password, UserStatus status, Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
        this.status = status;
        this.authorities = authorities;
    }

    public static UserPrincipal build(User user) {
        List<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
        );

        return new UserPrincipal(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getPassword(),
                user.getStatus(),
                authorities
        );
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public UserStatus getStatus() { return status; }

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
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return status == UserStatus.ACTIVE;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status == UserStatus.ACTIVE;
    }
}
