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
public class WatchlistResponse {

    private Long id;
    private Long userId;
    private Long movieId;
    private String movieTitle;
    private Integer movieReleaseYear;
    private String moviePosterPath;
    private String status;  // WANT_TO_WATCH, WATCHING, WATCHED, NOT_INTERESTED
    private LocalDateTime addedAt;
    private LocalDateTime updatedAt;
}