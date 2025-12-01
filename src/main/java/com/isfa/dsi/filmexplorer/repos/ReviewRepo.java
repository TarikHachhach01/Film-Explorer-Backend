package com.isfa.dsi.filmexplorer.repos;

import com.isfa.dsi.filmexplorer.models.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepo extends JpaRepository<Review, Long> {

    /**
     * Find all reviews for a specific movie
     */
    Page<Review> findByMovieId(Long movieId, Pageable pageable);

    /**
     * Find all reviews by a specific user
     */
    Page<Review> findByUserId(Long userId, Pageable pageable);

    /**
     * Find a review by user and movie (user can only review a movie once)
     */
    Optional<Review> findByUserIdAndMovieId(Long userId, Long movieId);

    /**
     * Check if user has reviewed a movie
     */
    boolean existsByUserIdAndMovieId(Long userId, Long movieId);

    /**
     * Get reviews count for a movie
     */
    long countByMovieId(Long movieId);

    /**
     * Get average rating for a movie
     */
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.movie.id = :movieId")
    Double getAverageRatingForMovie(@Param("movieId") Long movieId);

    /**
     * Find reviews sorted by likes count (most liked first)
     */
    @Query("SELECT r FROM Review r WHERE r.movie.id = :movieId ORDER BY r.likesCount DESC, r.createdAt DESC")
    List<Review> findTopReviewsForMovie(@Param("movieId") Long movieId, Pageable pageable);

    /**
     * Delete all reviews by a user
     */
    void deleteByUserId(Long userId);

    /**
     * Delete all reviews for a movie
     */
    void deleteByMovieId(Long movieId);
}