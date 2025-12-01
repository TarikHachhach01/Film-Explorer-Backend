package com.isfa.dsi.filmexplorer.user;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.isfa.dsi.filmexplorer.user.Permission.*;

public enum Role {
    USER(
            Set.of(
                    READ_MOVIES,
                    WRITE_REVIEWS,
                    READ_REVIEWS
            )
    ),
    ADMIN(
            Set.of(
                    READ_MOVIES,
                    WRITE_REVIEWS,
                    READ_REVIEWS,
                    DELETE_REVIEWS,
                    MANAGE_USERS,
                    WRITEE_MOVIES
            )
    );

    private final Set<Permission> permissions;

    Role(Set<Permission> permissions) {
        this.permissions = permissions;
    }

    public List<SimpleGrantedAuthority> getAuthorities() {
        var authorities = new java.util.ArrayList<SimpleGrantedAuthority>();


        authorities.add(new SimpleGrantedAuthority("ROLE_" + this.name()));


        authorities.addAll(permissions.stream()
                .map(permission -> new SimpleGrantedAuthority(permission.getPermission()))
                .toList());

        return authorities;
    }
}