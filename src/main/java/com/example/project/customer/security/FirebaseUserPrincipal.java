package com.example.project.customer.security;

import com.example.project.customer.entity.Role;
import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Getter
@Builder
public class FirebaseUserPrincipal implements UserDetails, Serializable {

    private static final long serialVersionUID = 1L;

    private final Integer internalUserId;
    private final String firebaseUid;
    private final String email;
    private final String name;
    private final Role role;
    private final Integer sellerId;
    private final boolean active;
    private final Map<String, Object> claims;
    private final Collection<? extends GrantedAuthority> authorities;

    public static FirebaseUserPrincipal create(
            Integer internalUserId,
            String firebaseUid,
            String email,
            String name,
            Role role,
            Integer sellerId,
            boolean active,
            Map<String, Object> claims
    ) {
        Role resolvedRole = role != null ? role : Role.CUSTOMER;
        List<GrantedAuthority> auths = Collections.singletonList(
                new SimpleGrantedAuthority(resolvedRole.getAuthority())
        );

        return FirebaseUserPrincipal.builder()
                .internalUserId(internalUserId)
                .firebaseUid(firebaseUid)
                .email(email)
                .name(name)
                .role(resolvedRole)
                .sellerId(sellerId)
                .active(active)
                .claims(claims)
                .authorities(auths)
                .build();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return email != null ? email : firebaseUid;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return active;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
