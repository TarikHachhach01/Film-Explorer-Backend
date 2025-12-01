package com.isfa.dsi.filmexplorer.repos;

import com.isfa.dsi.filmexplorer.models.Watchlist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WatchlistRepo extends JpaRepository<Watchlist, Long> {

    // ============================================
    // EXISTING METHODS (you already have these)
    // ============================================

    Page<Watchlist> findByUserId(Long userId, Pageable pageable);

    Page<Watchlist> findByUserIdAndStatus(Long userId, Watchlist.WatchlistStatus status, Pageable pageable);

    Optional<Watchlist> findByUserIdAndMovieId(Long userId, Long movieId);

    boolean existsByUserIdAndMovieId(Long userId, Long movieId);

    long countByUserId(Long userId);

    long countByUserIdAndStatus(Long userId, Watchlist.WatchlistStatus status);

    void deleteByUserId(Long userId);

    void deleteByMovieId(Long movieId);

    // ============================================
    // NEW METHODS FOR SOCIAL FEATURES
    // ============================================

    /**
     * Get user's public watchlist items (visible to friends)
     */
    @Query("SELECT w FROM Watchlist w WHERE " +
            "w.user.id = :userId AND w.isPublic = true " +
            "ORDER BY w.addedAt DESC")
    Page<Watchlist> findPublicWatchlist(@Param("userId") Long userId, Pageable pageable);

    /**
     * Get friends watching a specific movie
     */
    @Query("SELECT w FROM Watchlist w WHERE " +
            "w.movie.id = :movieId AND " +
            "w.user.id IN :friendIds AND " +
            "w.isPublic = true")
    List<Watchlist> findFriendsWatchingMovie(@Param("movieId") Long movieId,
                                             @Param("friendIds") List<Long> friendIds);
}