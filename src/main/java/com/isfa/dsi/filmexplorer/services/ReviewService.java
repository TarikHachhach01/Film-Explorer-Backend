package com.isfa.dsi.filmexplorer.services;

import com.isfa.dsi.filmexplorer.DTOs.ReviewRequest;
import com.isfa.dsi.filmexplorer.DTOs.ReviewResponse;
import com.isfa.dsi.filmexplorer.models.Review;
import com.isfa.dsi.filmexplorer.models.Movies;
import com.isfa.dsi.filmexplorer.repos.ReviewRepo;
import com.isfa.dsi.filmexplorer.repos.MoviesRepo;
import com.isfa.dsi.filmexplorer.user.User;
import com.isfa.dsi.filmexplorer.user.UserRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final ReviewRepo reviewRepository;
    private final MoviesRepo moviesRepository;
    private final UserRepo userRepository;

    /**
     * Get all reviews for a specific movie
     */
    public Page<ReviewResponse> getMovieReviews(Long movieId, Long userId, Pageable pageable) {
        log.info("Fetching reviews for movie: {} by user: {}", movieId, userId);

        Page<Review> reviews = reviewRepository.findByMovieId(movieId, pageable);

        return reviews.map(review -> convertToResponse(review, userId));
    }

    /**
     * Create a new review
     * ✅ FIXED: Add created_at and updated_at timestamps
     */
    @Transactional
    public ReviewResponse createReview(ReviewRequest request, Long userId) {
        log.info("Creating review for movie: {} by user: {}", request.getMovieId(), userId);

        // Verify movie exists
        Movies movie = moviesRepository.findById(request.getMovieId())
                .orElseThrow(() -> new RuntimeException("Movie not found"));

        // Verify user exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Create review entity
        Review review = new Review();
        review.setMovie(movie);
        review.setUser(user);
        review.setRating(request.getRating());
        review.setTitle(request.getTitle());
        review.setContent(request.getContent());
        review.setLikesCount(0L);

        // ✅ FIXED: Set timestamps
        review.setCreatedAt(LocalDateTime.now());
        review.setUpdatedAt(LocalDateTime.now());

        // Save review
        Review savedReview = reviewRepository.save(review);
        log.info("Review created successfully with ID: {}", savedReview.getId());

        return convertToResponse(savedReview, userId);
    }

    /**
     * Update a review
     * ✅ FIXED: Update updated_at timestamp
     */
    @Transactional
    public ReviewResponse updateReview(Long reviewId, ReviewRequest request, Long userId) {
        log.info("Updating review: {} by user: {}", reviewId, userId);

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));

        // Verify user is author
        if (!review.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized: You can only edit your own reviews");
        }

        // Update fields
        review.setRating(request.getRating());
        review.setTitle(request.getTitle());
        review.setContent(request.getContent());

        // ✅ FIXED: Update timestamp
        review.setUpdatedAt(LocalDateTime.now());

        Review updatedReview = reviewRepository.save(review);
        log.info("Review updated successfully: {}", reviewId);

        return convertToResponse(updatedReview, userId);
    }

    /**
     * Delete a review
     */
    @Transactional
    public void deleteReview(Long reviewId, Long userId) {
        log.info("Deleting review: {} by user: {}", reviewId, userId);

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));

        // Verify user is author or admin
        if (!review.getUser().getId().equals(userId)) {
            log.warn("Unauthorized delete attempt for review: {}", reviewId);
            throw new RuntimeException("Unauthorized: You can only delete your own reviews");
        }

        reviewRepository.delete(review);
        log.info("Review deleted successfully: {}", reviewId);
    }

    /**
     * Like a review
     */
    @Transactional
    public ReviewResponse likeReview(Long reviewId, Long userId) {
        log.info("Liking review: {} by user: {}", reviewId, userId);

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));

        // Increment likes count
        review.setLikesCount((review.getLikesCount() != null ? review.getLikesCount() : 0L) + 1);

        Review updatedReview = reviewRepository.save(review);
        log.info("Review liked successfully: {}", reviewId);

        return convertToResponse(updatedReview, userId);
    }

    /**
     * Unlike a review
     */
    @Transactional
    public ReviewResponse unlikeReview(Long reviewId, Long userId) {
        log.info("Unliking review: {} by user: {}", reviewId, userId);

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));

        // Decrement likes count
        Long currentLikes = review.getLikesCount() != null ? review.getLikesCount() : 0L;
        review.setLikesCount(Math.max(0L, currentLikes - 1));

        Review updatedReview = reviewRepository.save(review);
        log.info("Review unliked successfully: {}", reviewId);

        return convertToResponse(updatedReview, userId);
    }

    /**
     * Convert Review entity to ReviewResponse DTO
     */
    private ReviewResponse convertToResponse(Review review, Long currentUserId) {
        ReviewResponse response = new ReviewResponse();
        response.setId(review.getId());
        response.setUserId(review.getUser().getId());
        response.setMovieId(review.getMovie().getId());
        response.setRating(review.getRating());
        response.setTitle(review.getTitle());
        response.setContent(review.getContent());
        response.setLikesCount(review.getLikesCount() != null ? review.getLikesCount() : 0L);
        response.setCreatedAt(review.getCreatedAt());
        response.setUpdatedAt(review.getUpdatedAt());
        response.setUserEmail(review.getUser().getEmail());
        response.setIsAuthor(review.getUser().getId().equals(currentUserId));
        // response.setIsLiked could be set if you track likes separately

        return response;
    }
}