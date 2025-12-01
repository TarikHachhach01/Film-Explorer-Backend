package com.isfa.dsi.filmexplorer.services;

import com.isfa.dsi.filmexplorer.DTOs.WatchlistResponse;
import com.isfa.dsi.filmexplorer.models.Watchlist;
import com.isfa.dsi.filmexplorer.models.Movies;
import com.isfa.dsi.filmexplorer.repos.WatchlistRepo;
import com.isfa.dsi.filmexplorer.repos.MoviesRepo;
import com.isfa.dsi.filmexplorer.user.User;
import com.isfa.dsi.filmexplorer.user.UserRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class WatchlistService {

    private final WatchlistRepo watchlistRepository;
    private final MoviesRepo moviesRepository;
    private final UserRepo userRepository;
    private final FriendshipService friendshipService;

    /**
     * Add a movie to user's watchlist
     */
    public WatchlistResponse addToWatchlist(Long userId, Long movieId, Watchlist.WatchlistStatus status) {
        log.info("Adding movie {} to watchlist for user {}", movieId, userId);

        // Get user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Get movie
        Movies movie = moviesRepository.findById(movieId)
                .orElseThrow(() -> new RuntimeException("Movie not found"));

        // Check if already in watchlist
        if (watchlistRepository.existsByUserIdAndMovieId(userId, movieId)) {
            throw new RuntimeException("Movie already in watchlist");
        }

        // Create watchlist entry
        Watchlist watchlist = Watchlist.builder()
                .user(user)
                .movie(movie)
                .status(status != null ? status : Watchlist.WatchlistStatus.WANT_TO_WATCH)
                .build();

        Watchlist savedWatchlist = watchlistRepository.save(watchlist);
        log.info("Movie added to watchlist successfully");

        return mapToWatchlistResponse(savedWatchlist);
    }

    /**
     * Remove a movie from user's watchlist
     */
    public void removeFromWatchlist(Long watchlistId, Long userId) {
        log.info("Removing watchlist entry {} by user {}", watchlistId, userId);

        Watchlist watchlist = watchlistRepository.findById(watchlistId)
                .orElseThrow(() -> new RuntimeException("Watchlist entry not found"));

        // Check if user owns this watchlist entry
        if (!watchlist.getUser().getId().equals(userId)) {
            throw new RuntimeException("User can only delete their own watchlist entries");
        }

        watchlistRepository.deleteById(watchlistId);
        log.info("Watchlist entry removed successfully");
    }

    /**
     * Update watchlist status
     */
    public WatchlistResponse updateWatchlistStatus(Long watchlistId, Watchlist.WatchlistStatus newStatus, Long userId) {
        log.info("Updating watchlist status for entry {}", watchlistId);

        Watchlist watchlist = watchlistRepository.findById(watchlistId)
                .orElseThrow(() -> new RuntimeException("Watchlist entry not found"));

        // Check if user owns this watchlist entry
        if (!watchlist.getUser().getId().equals(userId)) {
            throw new RuntimeException("User can only update their own watchlist entries");
        }

        watchlist.setStatus(newStatus);
        Watchlist updatedWatchlist = watchlistRepository.save(watchlist);

        log.info("Watchlist status updated successfully");
        return mapToWatchlistResponse(updatedWatchlist);
    }

    /**
     * Get user's entire watchlist
     */
    public Page<WatchlistResponse> getUserWatchlist(Long userId, Pageable pageable) {
        log.info("Fetching watchlist for user {}", userId);

        Page<Watchlist> watchlists = watchlistRepository.findByUserId(userId, pageable);

        List<WatchlistResponse> responses = watchlists.getContent().stream()
                .map(this::mapToWatchlistResponse)
                .collect(Collectors.toList());

        return new PageImpl<>(responses, pageable, watchlists.getTotalElements());
    }

    /**
     * Get movies in specific status for user
     */
    public Page<WatchlistResponse> getWatchlistByStatus(Long userId, Watchlist.WatchlistStatus status, Pageable pageable) {
        log.info("Fetching watchlist for user {} with status {}", userId, status);

        Page<Watchlist> watchlists = watchlistRepository.findByUserIdAndStatus(userId, status, pageable);

        List<WatchlistResponse> responses = watchlists.getContent().stream()
                .map(this::mapToWatchlistResponse)
                .collect(Collectors.toList());

        return new PageImpl<>(responses, pageable, watchlists.getTotalElements());
    }

    /**
     * Check if movie is in user's watchlist
     */
    public boolean isInWatchlist(Long userId, Long movieId) {
        return watchlistRepository.existsByUserIdAndMovieId(userId, movieId);
    }

    /**
     * Get watchlist entry if exists
     */
    public WatchlistResponse getWatchlistEntry(Long userId, Long movieId) {
        log.info("Fetching watchlist entry for user {} and movie {}", userId, movieId);

        Watchlist watchlist = watchlistRepository.findByUserIdAndMovieId(userId, movieId)
                .orElseThrow(() -> new RuntimeException("Movie not in watchlist"));

        return mapToWatchlistResponse(watchlist);
    }

    /**
     * Get count of movies in user's watchlist
     */
    public long getWatchlistCount(Long userId) {
        return watchlistRepository.countByUserId(userId);
    }

    /**
     * Get count of movies in specific status
     */
    public long getWatchlistCountByStatus(Long userId, Watchlist.WatchlistStatus status) {
        return watchlistRepository.countByUserIdAndStatus(userId, status);
    }

    /**
     * Clear entire watchlist for user (dangerous - admin only)
     */
    public void clearWatchlist(Long userId) {
        log.warn("Clearing entire watchlist for user {}", userId);
        watchlistRepository.deleteByUserId(userId);
    }

    /**
     * Remove all watchlist entries for a movie (when movie is deleted)
     */
    public void removeMovieFromAllWatchlists(Long movieId) {
        log.info("Removing movie {} from all watchlists", movieId);
        watchlistRepository.deleteByMovieId(movieId);
    }

    /**
     * Get friend's public watchlist
     */
    public Page<WatchlistResponse> getFriendWatchlist(Long currentUserId, Long friendId, Pageable pageable) {
        log.info("Fetching public watchlist for friend {} by user {}", friendId, currentUserId);

        // Check if they are friends
        if (!friendshipService.areFriends(currentUserId, friendId)) {
            throw new RuntimeException("Not friends with this user");
        }

        // Get friend's public watchlist
        Page<Watchlist> watchlists = watchlistRepository.findPublicWatchlist(friendId, pageable);

        List<WatchlistResponse> responses = watchlists.getContent().stream()
                .map(this::mapToWatchlistResponse)
                .collect(Collectors.toList());

        return new PageImpl<>(responses, pageable, watchlists.getTotalElements());
    }

    /**
     * Get friends watching a specific movie
     */
    public List<String> getFriendsWatchingMovie(Long userId, Long movieId) {
        log.info("Checking friends watching movie {} for user {}", movieId, userId);

        List<Long> friendIds = friendshipService.getFriendIds(userId);

        if (friendIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<Watchlist> friendsWatching = watchlistRepository.findFriendsWatchingMovie(movieId, friendIds);

        return friendsWatching.stream()
                .map(w -> w.getUser().getFirstName() + " " + w.getUser().getLastName())
                .collect(Collectors.toList());
    }

    /**
     * Update watchlist with notes and privacy settings
     */
    public WatchlistResponse updateWatchlistDetails(Long watchlistId, Long userId,
                                                    Boolean isPublic, String notes) {
        log.info("Updating watchlist details for entry {}", watchlistId);

        Watchlist watchlist = watchlistRepository.findById(watchlistId)
                .orElseThrow(() -> new RuntimeException("Watchlist entry not found"));

        // Check if user owns this watchlist entry
        if (!watchlist.getUser().getId().equals(userId)) {
            throw new RuntimeException("User can only update their own watchlist entries");
        }

        // Update fields if provided
        if (isPublic != null) {
            watchlist.setIsPublic(isPublic);
        }
        if (notes != null) {
            watchlist.setNotes(notes);
        }

        Watchlist updated = watchlistRepository.save(watchlist);
        log.info("Watchlist details updated successfully");

        return mapToWatchlistResponse(updated);
    }

    /**
     * Map Watchlist entity to WatchlistResponse DTO
     */
    private WatchlistResponse mapToWatchlistResponse(Watchlist watchlist) {
        return WatchlistResponse.builder()
                .id(watchlist.getId())
                .userId(watchlist.getUser().getId())
                .movieId(watchlist.getMovie().getId())
                .movieTitle(watchlist.getMovie().getTitle())
                .movieReleaseYear(watchlist.getMovie().getReleaseYear() != null
                        ? watchlist.getMovie().getReleaseYear().intValue() : null)
                .moviePosterPath(watchlist.getMovie().getPosterPath())
                .status(watchlist.getStatus().toString())
                .addedAt(watchlist.getAddedAt())
                .updatedAt(watchlist.getUpdatedAt())
                .build();
    }
}