package com.isfa.dsi.filmexplorer.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import com.isfa.dsi.filmexplorer.user.User;

import java.time.LocalDateTime;

@Entity
@Table(name = "watchlist", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "movie_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Watchlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id", nullable = false)
    private Movies movie;

    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private WatchlistStatus status = WatchlistStatus.WANT_TO_WATCH;

    // ============================================
    // NEW SOCIAL FEATURES
    // ============================================

    @Column(name = "is_public")
    private Boolean isPublic = false;  // Can friends see this item?

    @Column(columnDefinition = "TEXT")
    private String notes;  // Personal notes about the movie

    @Column(name = "position")
    private Integer position = 0;  // For custom ordering (drag & drop)

    // ============================================
    // EXISTING FIELDS
    // ============================================

    @CreatedDate
    @Column(nullable = false, updatable = false, name = "added_at")
    private LocalDateTime addedAt;

    @LastModifiedDate
    @Column(insertable = false, name = "updated_at")
    private LocalDateTime updatedAt;

    public enum WatchlistStatus {
        WANT_TO_WATCH,  // User wants to watch
        WATCHING,       // Currently watching
        WATCHED,        // Finished watching
        NOT_INTERESTED  // User not interested
    }
}