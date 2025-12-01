package com.isfa.dsi.filmexplorer.repos;

import com.isfa.dsi.filmexplorer.models.Friendship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendshipRepo extends JpaRepository<Friendship, Long> {

    /**
     * Find all accepted friends for a user (bidirectional)
     */
    @Query("SELECT f FROM Friendship f WHERE " +
            "(f.user.id = :userId OR f.friend.id = :userId) AND " +
            "f.status = 'ACCEPTED'")
    List<Friendship> findFriends(@Param("userId") Long userId);

    /**
     * Find friendship between two users (bidirectional)
     */
    @Query("SELECT f FROM Friendship f WHERE " +
            "(f.user.id = :userId AND f.friend.id = :friendId) OR " +
            "(f.user.id = :friendId AND f.friend.id = :userId)")
    Optional<Friendship> findFriendship(@Param("userId") Long userId,
                                        @Param("friendId") Long friendId);

    /**
     * Check if two users are friends (accepted status)
     */
    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM Friendship f WHERE " +
            "((f.user.id = :userId AND f.friend.id = :friendId) OR " +
            "(f.user.id = :friendId AND f.friend.id = :userId)) AND " +
            "f.status = 'ACCEPTED'")
    boolean areFriends(@Param("userId") Long userId, @Param("friendId") Long friendId);

    /**
     * Count total friends for a user
     */
    @Query("SELECT COUNT(f) FROM Friendship f WHERE " +
            "(f.user.id = :userId OR f.friend.id = :userId) AND " +
            "f.status = 'ACCEPTED'")
    long countFriends(@Param("userId") Long userId);
}