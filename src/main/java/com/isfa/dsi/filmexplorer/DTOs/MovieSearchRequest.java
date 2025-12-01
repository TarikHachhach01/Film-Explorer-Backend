package com.isfa.dsi.filmexplorer.DTOs;

import lombok.Data;

import java.util.List;

@Data
public class MovieSearchRequest {
    // Basic search
    private String query;

    // Quick filters (checkboxes)
    private Boolean highlyRated;    // >7.0
    private Boolean recentlyReleased; // Last 5 years
    private Boolean popular;        // >1k votes
    private Boolean shortRuntime;   // <90 min

    // Detailed filters
    private List<String> genres;
    private Integer minYear;
    private Integer maxYear;
    private Double minRating;
    private Double maxRating;
    private Integer minVoteCount;
    private String director;
    private List<String> actors;
    private Integer minRuntime;
    private Integer maxRuntime;

    // NEW: Essential additions based on your database
    private Double minImdbRating;    // imdb_rating field - users trust IMDB
    private Double maxImdbRating;    // imdb_rating field
    private String overview;         // search overview field for content-based search
    private Boolean searchForeign;   // also search original_title field

    // pagination params
    private int page = 0;
    private int size = 20;
    private String sortBy = "popularity";
    private String sortDirection = "desc";
}