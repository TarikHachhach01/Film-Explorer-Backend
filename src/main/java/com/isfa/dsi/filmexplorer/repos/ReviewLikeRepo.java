package com.isfa.dsi.filmexplorer.repos;

import com.isfa.dsi.filmexplorer.models.ReviewLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewLikeRepo extends JpaRepository<ReviewLike, Long> {

    /**
     * Find a like by user and review
     */
    Optional<ReviewLike> findByUserIdAndReviewId(Long userId, Long reviewId);

    /**
     * Check if user has liked a review
     */
    boolean existsByUserIdAndReviewId(Long userId, Long reviewId);

    /**
     * Get count of likes for a review
     */
    long countByReviewId(Long reviewId);

    /**
     * Delete a like
     */
    void deleteByUserIdAndReviewId(Long userId, Long reviewId);

    /**
     * Delete all likes for a review
     */
    void deleteByReviewId(Long reviewId);
}