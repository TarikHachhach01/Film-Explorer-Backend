package com.isfa.dsi.filmexplorer.services;

import com.isfa.dsi.filmexplorer.models.Friendship;
import com.isfa.dsi.filmexplorer.repos.FriendshipRepo;
import com.isfa.dsi.filmexplorer.user.User;
import com.isfa.dsi.filmexplorer.user.UserRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class FriendshipService {

    private final FriendshipRepo friendshipRepo;
    private final UserRepo userRepo;

    /**
     * Add a friend (instant acceptance - no approval needed)
     */
    public void addFriend(Long userId, Long friendId) {
        if (userId.equals(friendId)) {
            throw new RuntimeException("Cannot add yourself as friend");
        }

        if (friendshipRepo.areFriends(userId, friendId)) {
            throw new RuntimeException("Already friends");
        }

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        User friend = userRepo.findById(friendId)
                .orElseThrow(() -> new RuntimeException("Friend not found"));

        // Create friendship with ACCEPTED status (instant friends)
        Friendship friendship = Friendship.builder()
                .user(user)
                .friend(friend)
                .status(Friendship.FriendshipStatus.ACCEPTED)  // Use enum, not String!
                .acceptedAt(LocalDateTime.now())
                .build();

        friendshipRepo.save(friendship);
        log.info("Friendship created between {} and {}", userId, friendId);
    }

    /**
     * Remove a friend
     */
    public void removeFriend(Long userId, Long friendId) {
        Friendship friendship = friendshipRepo.findFriendship(userId, friendId)
                .orElseThrow(() -> new RuntimeException("Friendship not found"));

        friendshipRepo.delete(friendship);
        log.info("Friendship removed between {} and {}", userId, friendId);
    }

    /**
     * Get all friends for a user
     */
    public List<FriendInfo> getFriends(Long userId) {
        List<Friendship> friendships = friendshipRepo.findFriends(userId);

        return friendships.stream()
                .map(f -> {
                    // Get the OTHER user (not the current user)
                    User friend = f.getUser().getId().equals(userId) ? f.getFriend() : f.getUser();
                    return new FriendInfo(
                            friend.getId(),
                            friend.getEmail(),
                            friend.getFirstName(),
                            friend.getLastName()
                    );
                })
                .collect(Collectors.toList());
    }

    /**
     * Get friend IDs only (for queries)
     */
    public List<Long> getFriendIds(Long userId) {
        return getFriends(userId).stream()
                .map(FriendInfo::getId)
                .collect(Collectors.toList());
    }

    /**
     * Check if two users are friends
     */
    public boolean areFriends(Long userId, Long friendId) {
        return friendshipRepo.areFriends(userId, friendId);
    }

    /**
     * DTO for friend information
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class FriendInfo {
        private Long id;
        private String email;
        private String firstName;
        private String lastName;
    }
}