package com.zhongyan.uav.common.security;

import java.util.Collections;
import java.util.Set;

public class CurrentUser {
    private final String userId;
    private final String username;
    private final Set<String> roles;

    public CurrentUser(String userId, String username, Set<String> roles) {
        this.userId = userId;
        this.username = username;
        this.roles = roles == null ? Collections.emptySet() : Collections.unmodifiableSet(roles);
    }

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public Set<String> getRoles() {
        return roles;
    }
}

