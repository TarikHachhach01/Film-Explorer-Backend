package com.isfa.dsi.filmexplorer.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class TokenBlacklistService {

    private final Map<String, Long> blacklist = new ConcurrentHashMap<>();

    public void blacklistToken(String token, Long expirationTimeMs) {
        log.info(" Blacklisting token, expires at: {}", expirationTimeMs);
        blacklist.put(token, expirationTimeMs);
    }

    public boolean isBlacklisted(String token) {
        if (!blacklist.containsKey(token)) {
            return false;
        }

        Long expirationTime = blacklist.get(token);
        long now = System.currentTimeMillis();

        if (now > expirationTime) {
            log.debug("🗑️ Token naturally expired, removing from blacklist");
            blacklist.remove(token);
            return false;
        }

        return true;
    }

    public int getBlacklistSize() {
        return blacklist.size();
    }

    @Scheduled(fixedRate = 3600000)
    public void cleanupExpiredTokens() {
        long now = System.currentTimeMillis();
        int before = blacklist.size();

        blacklist.entrySet().removeIf(entry -> entry.getValue() < now);

        int after = blacklist.size();
        int removed = before - after;

        if (removed > 0) {
            log.info("🧹 Cleaned up {} expired tokens. Blacklist size: {}", removed, after);
        }
    }
}