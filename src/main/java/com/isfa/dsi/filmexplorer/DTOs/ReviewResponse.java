package com.isfa.dsi.filmexplorer.DTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewResponse {
    private Long id;
    private Long userId;
    private Long movieId;
    private Integer rating;           // 1-10
    private String title;
    private String content;
    private Long likesCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String userEmail;         // For display
    private Boolean isAuthor;         // If current user is author
    private Boolean isLiked;          // If current user liked this
}