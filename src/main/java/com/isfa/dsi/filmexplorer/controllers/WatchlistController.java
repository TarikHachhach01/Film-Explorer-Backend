package com.isfa.dsi.filmexplorer.controllers;

import com.isfa.dsi.filmexplorer.DTOs.WatchlistResponse;
import com.isfa.dsi.filmexplorer.models.Watchlist;
import com.isfa.dsi.filmexplorer.services.WatchlistService;
import com.isfa.dsi.filmexplorer.user.User;
import com.isfa.dsi.filmexplorer.user.UserRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/watchlist")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:4200")
public class WatchlistController {

    private final WatchlistService watchlistService;
    private final UserRepo userRepository;


    @PostMapping
    public ResponseEntity<WatchlistResponse> addToWatchlist(
            @RequestParam Long movieId,
            @RequestParam(required = false, defaultValue = "WANT_TO_WATCH") String status) {
        log.info("Adding movie {} to watchlist", movieId);

        Long userId = getCurrentUserId();
        Watchlist.WatchlistStatus watchStatus = Watchlist.WatchlistStatus.valueOf(status);
        WatchlistResponse response = watchlistService.addToWatchlist(userId, movieId, watchStatus);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @DeleteMapping("/{watchlistId}")
    public ResponseEntity<Void> removeFromWatchlist(@PathVariable Long watchlistId) {
        log.info("Removing watchlist entry {}", watchlistId);

        Long userId = getCurrentUserId();
        watchlistService.removeFromWatchlist(watchlistId, userId);

        return ResponseEntity.noContent().build();
    }


    @PutMapping("/{watchlistId}")
    public ResponseEntity<WatchlistResponse> updateWatchlistStatus(
            @PathVariable Long watchlistId,
            @RequestParam String status) {
        log.info("Updating watchlist status for entry {}", watchlistId);

        Long userId = getCurrentUserId();
        Watchlist.WatchlistStatus newStatus = Watchlist.WatchlistStatus.valueOf(status);
        WatchlistResponse response = watchlistService.updateWatchlistStatus(watchlistId, newStatus, userId);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<WatchlistResponse>> getWatchlist(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Fetching user's watchlist");

        Long userId = getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size);
        Page<WatchlistResponse> watchlist = watchlistService.getUserWatchlist(userId, pageable);

        return ResponseEntity.ok(watchlist);
    }


    @GetMapping("/status/{status}")
    public ResponseEntity<Page<WatchlistResponse>> getWatchlistByStatus(
            @PathVariable String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Fetching watchlist for status: {}", status);

        Long userId = getCurrentUserId();
        Watchlist.WatchlistStatus watchStatus = Watchlist.WatchlistStatus.valueOf(status);
        Pageable pageable = PageRequest.of(page, size);
        Page<WatchlistResponse> watchlist = watchlistService.getWatchlistByStatus(userId, watchStatus, pageable);

        return ResponseEntity.ok(watchlist);
    }


    @GetMapping("/check/{movieId}")
    public ResponseEntity<Boolean> isInWatchlist(@PathVariable Long movieId) {
        log.info("Checking if movie {} is in user's watchlist", movieId);

        Long userId = getCurrentUserId();
        boolean inWatchlist = watchlistService.isInWatchlist(userId, movieId);

        return ResponseEntity.ok(inWatchlist);
    }


    @GetMapping("/movie/{movieId}")
    public ResponseEntity<WatchlistResponse> getWatchlistEntry(@PathVariable Long movieId) {
        log.info("Fetching watchlist entry for movie {}", movieId);

        Long userId = getCurrentUserId();
        WatchlistResponse response = watchlistService.getWatchlistEntry(userId, movieId);

        return ResponseEntity.ok(response);
    }


    @GetMapping("/stats")
    public ResponseEntity<WatchlistStats> getWatchlistStats() {
        log.info("Fetching watchlist statistics");

        Long userId = getCurrentUserId();

        long totalCount = watchlistService.getWatchlistCount(userId);
        long wantToWatch = watchlistService.getWatchlistCountByStatus(userId, Watchlist.WatchlistStatus.WANT_TO_WATCH);
        long watching = watchlistService.getWatchlistCountByStatus(userId, Watchlist.WatchlistStatus.WATCHING);
        long watched = watchlistService.getWatchlistCountByStatus(userId, Watchlist.WatchlistStatus.WATCHED);
        long notInterested = watchlistService.getWatchlistCountByStatus(userId, Watchlist.WatchlistStatus.NOT_INTERESTED);

        WatchlistStats stats = WatchlistStats.builder()
                .totalCount(totalCount)
                .wantToWatchCount(wantToWatch)
                .watchingCount(watching)
                .watchedCount(watched)
                .notInterestedCount(notInterested)
                .build();

        return ResponseEntity.ok(stats);
    }


    @GetMapping("/friend/{friendId}")
    public ResponseEntity<Page<WatchlistResponse>> getFriendWatchlist(
            @PathVariable Long friendId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Fetching friend's watchlist: {}", friendId);

        Long userId = getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size);
        Page<WatchlistResponse> watchlist = watchlistService.getFriendWatchlist(userId, friendId, pageable);

        return ResponseEntity.ok(watchlist);
    }


    @GetMapping("/friends-watching/{movieId}")
    public ResponseEntity<List<String>> getFriendsWatchingMovie(@PathVariable Long movieId) {
        log.info("Checking friends watching movie: {}", movieId);

        Long userId = getCurrentUserId();
        List<String> friendNames = watchlistService.getFriendsWatchingMovie(userId, movieId);

        return ResponseEntity.ok(friendNames);
    }


    @PatchMapping("/{watchlistId}/details")
    public ResponseEntity<WatchlistResponse> updateWatchlistDetails(
            @PathVariable Long watchlistId,
            @RequestParam(required = false) Boolean isPublic,
            @RequestParam(required = false) String notes) {
        log.info("Updating watchlist details: {}", watchlistId);

        Long userId = getCurrentUserId();
        WatchlistResponse response = watchlistService.updateWatchlistDetails(
                watchlistId, userId, isPublic, notes);

        return ResponseEntity.ok(response);
    }


    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }

        try {

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String email = userDetails.getUsername();

            log.debug("Extracting user ID for email: {}", email);


            User user = userRepository.findByEmail(email)
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
    @lombok.Builder
    public static class WatchlistStats {
        private long totalCount;
        private long wantToWatchCount;
        private long watchingCount;
        private long watchedCount;
        private long notInterestedCount;
    }
}