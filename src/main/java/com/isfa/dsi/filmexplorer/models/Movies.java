package com.isfa.dsi.filmexplorer.models;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "movies")
@Data
public class Movies {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "title", columnDefinition = "TEXT")
    private String title;

    @Column(name = "vote_average", precision = 10, scale = 2)
    private BigDecimal voteAverage;

    @Column(name = "vote_count")
    private Long voteCount;

    @Column(name = "status", columnDefinition = "TEXT")
    private String status;

    @Column(name = "release_date")
    private LocalDate releaseDate;

    @Column(name = "revenue")
    private Long revenue;

    @Column(name = "runtime")
    private Integer runtime;

    @Column(name = "adult")
    private Boolean adult;

    @Column(name = "backdrop_path", columnDefinition = "TEXT")
    private String backdropPath;

    @Column(name = "budget")
    private Long budget;

    @Column(name = "homepage", columnDefinition = "TEXT")
    private String homepage;

    @Column(name = "imdb_id", columnDefinition = "TEXT")
    private String imdbId;

    @Column(name = "original_language", columnDefinition = "TEXT")
    private String originalLanguage;

    @Column(name = "original_title", columnDefinition = "TEXT")
    private String originalTitle;

    @Column(name = "overview", columnDefinition = "TEXT")
    private String overview;

    @Column(name = "popularity", precision = 10, scale = 6)
    private BigDecimal popularity;

    @Column(name = "poster_path", columnDefinition = "TEXT")
    private String posterPath;

    @Column(name = "tagline", columnDefinition = "TEXT")
    private String tagline;

    @Column(name = "production_companies", columnDefinition = "TEXT")
    private String productionCompanies;

    @Column(name = "production_countries", columnDefinition = "TEXT")
    private String productionCountries;

    @Column(name = "spoken_languages", columnDefinition = "TEXT")
    private String spokenLanguages;

    @Column(name = "keywords", columnDefinition = "TEXT")
    private String keywords;

    @Column(name = "release_year", precision = 4, scale = 0)
    private BigDecimal releaseYear;

    @Column(name = "director", columnDefinition = "TEXT")
    private String director;

    @Column(name = "averagerating", precision = 3, scale = 1)
    private BigDecimal averageRating;

    @Column(name = "poster_link", columnDefinition = "TEXT")
    private String posterLink;

    @Column(name = "certificate", columnDefinition = "TEXT")
    private String certificate;

    @Column(name = "imdb_rating", precision = 3, scale = 1)
    private BigDecimal imdbRating;

    @Column(name = "meta_score", precision = 3, scale = 0)
    private BigDecimal metaScore;

    @Column(name = "star1", columnDefinition = "TEXT")
    private String star1;

    @Column(name = "star2", columnDefinition = "TEXT")
    private String star2;

    @Column(name = "star3", columnDefinition = "TEXT")
    private String star3;

    @Column(name = "star4", columnDefinition = "TEXT")
    private String star4;

    @Column(name = "writer", columnDefinition = "TEXT")
    private String writer;

    @Column(name = "director_of_photography", columnDefinition = "TEXT")
    private String directorOfPhotography;

    @Column(name = "producers", columnDefinition = "TEXT")
    private String producers;

    @Column(name = "music_composer", columnDefinition = "TEXT")
    private String musicComposer;

    @Column(name = "genres_list", columnDefinition = "TEXT")
    private String genresList;

    @Column(name = "cast_list", columnDefinition = "TEXT")
    private String castList;

    @Column(name = "overview_sentiment", precision = 5, scale = 4)
    private BigDecimal overviewSentiment;

    @Column(name = "all_combined_keywords", columnDefinition = "TEXT")
    private String allCombinedKeywords;
}