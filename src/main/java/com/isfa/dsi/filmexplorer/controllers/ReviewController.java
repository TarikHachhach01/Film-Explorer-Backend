package com.isfa.dsi.filmexplorer.controllers;

import com.isfa.dsi.filmexplorer.DTOs.ReviewRequest;
import com.isfa.dsi.filmexplorer.DTOs.ReviewResponse;
import com.isfa.dsi.filmexplorer.services.ReviewService;
import com.isfa.dsi.filmexplorer.user.User;
import com.isfa.dsi.filmexplorer.user.UserRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:4200")
public class ReviewController {

    private final ReviewService reviewService;
    private final UserRepo userRepository;


    @GetMapping("/movie/{movieId}")
    public ResponseEntity<?> getMovieReviews(
            @PathVariable Long movieId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.info("Fetching reviews for movie: {} (page: {}, size: {})", movieId, page, size);

        try {
            Long userId = null;
            try {
                userId = getCurrentUserId();
                log.info("Authenticated user ID: {}", userId);
            } catch (Exception e) {
                log.info("No authenticated user, fetching reviews as anonymous");
            }

            Pageable pageable = PageRequest.of(page, size);

            Page<ReviewResponse> reviewsPage = reviewService.getMovieReviews(movieId, userId, pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("reviews", reviewsPage.getContent());
            response.put("currentPage", reviewsPage.getNumber());
            response.put("totalPages", reviewsPage.getTotalPages());
            response.put("totalResults", reviewsPage.getTotalElements());

            log.info("Returning {} reviews (page {}/{}) for movie {}",
                    reviewsPage.getNumberOfElements(),
                    reviewsPage.getNumber() + 1,
                    reviewsPage.getTotalPages(),
                    movieId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching reviews for movie {}: {}", movieId, e.getMessage(), e);

            // Return empty list instead of error for better UX
            Map<String, Object> emptyResponse = new HashMap<>();
            emptyResponse.put("reviews", new java.util.ArrayList<>());
            emptyResponse.put("currentPage", 0);
            emptyResponse.put("totalPages", 0);
            emptyResponse.put("totalResults", 0);

            return ResponseEntity.ok(emptyResponse);
        }
    }


    @PostMapping
    public ResponseEntity<?> createReview(@RequestBody ReviewRequest reviewRequest) {
        log.info("Creating review for movie {} with rating {}",
                reviewRequest.getMovieId(), reviewRequest.getRating());

        try {
            Long userId = getCurrentUserId();
            log.info("Review created by user ID: {}", userId);

            ReviewResponse review = reviewService.createReview(reviewRequest, userId);

            log.info("Review created successfully with ID: {}", review.getId());

            return ResponseEntity.ok(review);
        } catch (Exception e) {
            log.error("Error creating review: {}", e.getMessage(), e);
            return ResponseEntity.status(400).body("Error creating review: " + e.getMessage());
        }
    }


    @PutMapping("/{id}")
    public ResponseEntity<?> updateReview(
            @PathVariable Long id,
            @RequestBody ReviewRequest reviewRequest) {

        log.info("Updating review: {}", id);

        try {
            Long userId = getCurrentUserId();
            ReviewResponse review = reviewService.updateReview(id, reviewRequest, userId);

            log.info("Review {} updated successfully", id);

            return ResponseEntity.ok(review);
        } catch (Exception e) {
            log.error("Error updating review {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(400).body("Error updating review: " + e.getMessage());
        }
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long id) {
        log.info("Deleting review: {}", id);

        try {
            Long userId = getCurrentUserId();
            reviewService.deleteReview(id, userId);

            log.info("Review {} deleted successfully", id);


            return ResponseEntity.noContent().build();

        } catch (Exception e) {
            log.error("Error deleting review {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(400).build();
        }
    }


    @PostMapping("/{id}/like")
    public ResponseEntity<?> likeReview(@PathVariable Long id) {
        log.info("Liking review: {}", id);

        try {
            Long userId = getCurrentUserId();
            ReviewResponse review = reviewService.likeReview(id, userId);

            log.info("Review {} liked successfully", id);

            return ResponseEntity.ok(review);
        } catch (Exception e) {
            log.error("Error liking review {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(400).body("Error liking review: " + e.getMessage());
        }
    }


    @DeleteMapping("/{id}/like")
    public ResponseEntity<?> unlikeReview(@PathVariable Long id) {
        log.info("Unliking review: {}", id);

        try {
            Long userId = getCurrentUserId();
            ReviewResponse review = reviewService.unlikeReview(id, userId);

            log.info("Review {} unliked successfully", id);

            return ResponseEntity.ok(review);
        } catch (Exception e) {
            log.error("Error unliking review {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(400).body("Error unliking review: " + e.getMessage());
        }
    }


    private Long getCurrentUserId() {
        log.debug("Extracting user ID from authentication");

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String email = userDetails.getUsername(); // Email is stored as username in JWT

            log.debug("User email from authentication: {}", email);

            // Find user by email in database
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> {
                        log.error("User not found for email: {}", email);
                        return new RuntimeException("User not found");
                    });

            log.debug("User found with ID: {}", user.getId());
            return user.getId();
        }

        log.warn("No authentication principal found");
        throw new RuntimeException("User not authenticated");
    }
}