package com.isfa.dsi.filmexplorer.DTOs;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Data
@Getter
@Setter
public class MovieCard {
    private Long id;
    private String title;
    private Integer releaseYear;
    private BigDecimal rating;
    private Long voteCount;
    private String posterPath;
    private List<String> genres;
    private String director;
    private String[] mainStars;
    private Integer runtime;


    private BigDecimal imdbRating;
    private BigDecimal popularity;
    private String overview;
    private String originalTitle;
    private Boolean isImdbRated;


    public String getPrimaryGenre() {
        return (genres != null && !genres.isEmpty()) ? genres.get(0) : "Unknown";
    }


    public String getShortOverview() {
        if (overview == null || overview.length() <= 150) {
            return overview;
        }
        return overview.substring(0, 147) + "...";
    }


    public String getRatingSource() {
        return Boolean.TRUE.equals(isImdbRated) ? "IMDB" : "TMDb";
    }


    public String getDisplayTitle() {
        if (releaseYear != null) {
            return title + " (" + releaseYear + ")";
        }
        return title;
    }
}