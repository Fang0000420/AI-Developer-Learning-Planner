package com.aidevplanner.backend.lock;

public record ResourceLockHandle(
        String key,
        String token,
        boolean locked
) {

    public static ResourceLockHandle bypass(String key, String token) {
        return new ResourceLockHandle(key, token, false);
    }
}
