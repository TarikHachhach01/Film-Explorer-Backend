package com.isfa.dsi.filmexplorer.controllers;

import com.isfa.dsi.filmexplorer.services.FriendshipService;
import com.isfa.dsi.filmexplorer.user.User;
import com.isfa.dsi.filmexplorer.user.UserRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
@Slf4j
public class FriendshipController {

    private final FriendshipService friendshipService;
    private final UserRepo userRepo;

    @GetMapping
    public ResponseEntity<List<FriendshipService.FriendInfo>> getMyFriends() {
        Long userId = getCurrentUserId();
        List<FriendshipService.FriendInfo> friends = friendshipService.getFriends(userId);
        return ResponseEntity.ok(friends);
    }

    @PostMapping("/{friendId}")
    public ResponseEntity<Void> addFriend(@PathVariable Long friendId) {
        Long userId = getCurrentUserId();
        friendshipService.addFriend(userId, friendId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{friendId}")
    public ResponseEntity<Void> removeFriend(@PathVariable Long friendId) {
        Long userId = getCurrentUserId();
        friendshipService.removeFriend(userId, friendId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<UserSearchResult>> searchUsers(@RequestParam String q) {
        List<User> users = userRepo.findAll().stream()
                .filter(u -> u.getEmail().toLowerCase().contains(q.toLowerCase()) ||
                        (u.getFirstName() + " " + u.getLastName()).toLowerCase().contains(q.toLowerCase()))
                .limit(20)
                .collect(Collectors.toList());

        List<UserSearchResult> results = users.stream()
                .map(u -> new UserSearchResult(
                        u.getId(),
                        u.getEmail(),
                        u.getFirstName(),
                        u.getLastName()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(results);
    }

    @GetMapping("/check/{userId}")
    public ResponseEntity<Boolean> checkFriendship(@PathVariable Long userId) {
        Long currentUserId = getCurrentUserId();
        boolean areFriends = friendshipService.areFriends(currentUserId, userId);
        return ResponseEntity.ok(areFriends);
    }


    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }

        try {
            // The principal is a UserDetails object (which is your User entity)
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String email = userDetails.getUsername(); // Username is the email

            log.debug("Extracting user ID for email: {}", email);

            // Look up user by email
            User user = userRepo.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

            log.debug("Found user ID: {} for email: {}", user.getId(), email);
            return user.getId();
        } catch (ClassCastException e) {
            log.error("Principal is not UserDetails: {}", authentication.getPrincipal().getClass());
            throw new RuntimeException("Invalid authentication principal type");
        } catch (Exception e) {
            log.error("Could not extract user ID from authentication: {}", e.getMessage());
            throw new RuntimeException("Could not get user ID from authentication");
        }
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class UserSearchResult {
        private Long id;
        private String email;
        private String firstName;
        private String lastName;
    }
}