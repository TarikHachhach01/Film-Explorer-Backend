package com.isfa.dsi.filmexplorer.DTOs;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class MovieSearchResponse {
    private List<MovieCard> movies;
    private int currentPage;
    private int totalPages;
    private long totalResults;
    private Map<String, Long> facetCounts;

    private String searchQuery;          // What the user searched for
    private String appliedFilters;       // Summary of applied filters
    private long searchTimeMs;           // How long the search took
    private String sortedBy;             // How results are sorted
    private Boolean hasMoreResults;      // If there are more pages

    // Helper method to check if search was successful
    public boolean hasResults() {
        return movies != null && !movies.isEmpty();
    }

    // Helper method to get result summary
    public String getResultSummary() {
        if (totalResults == 0) {
            return "No movies found";
        } else if (totalResults == 1) {
            return "1 movie found";
        } else {
            return totalResults + " movies found";
        }
    }
}