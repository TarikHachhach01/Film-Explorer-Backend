package com.isfa.dsi.filmexplorer.services;

import com.isfa.dsi.filmexplorer.DTOs.MovieCard;
import com.isfa.dsi.filmexplorer.DTOs.MovieSearchRequest;
import com.isfa.dsi.filmexplorer.DTOs.MovieSearchResponse;
import com.isfa.dsi.filmexplorer.models.Movies;
import com.isfa.dsi.filmexplorer.repos.MoviesRepo;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Year;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MovieService {

    private final MoviesRepo movieRepository;

    // Genre normalization map
    private static final Map<String, String> GENRE_ALIASES = Map.ofEntries(
            Map.entry("sci-fi", "Science Fiction"),
            Map.entry("scifi", "Science Fiction"),
            Map.entry("sf", "Science Fiction"),
            Map.entry("horror", "Horror"),
            Map.entry("comedy", "Comedy"),
            Map.entry("drama", "Drama"),
            Map.entry("action", "Action"),
            Map.entry("adventure", "Adventure"),
            Map.entry("romance", "Romance"),
            Map.entry("thriller", "Thriller"),
            Map.entry("mystery", "Mystery"),
            Map.entry("crime", "Crime"),
            Map.entry("animation", "Animation")
    );

    /**
     * Search movies with EXACT criteria matching
     * Includes data quality improvements
     */
    public Page<Movies> searchMovies(MovieSearchRequest request) {
        log.info("=== SEARCH STARTED ===");
        log.info("Raw request received:");
        logRequestDetails(request);

        // Apply quick filter defaults
        applyQuickFilterDefaults(request);
        log.info("After quick filters applied:");
        logRequestDetails(request);

        // Normalize genre names for better matching
        normalizeGenreNames(request);
        log.info("After genre normalization:");
        logRequestDetails(request);

        // Build specification with data quality considerations
        Specification<Movies> spec = buildRefinedSpecification(request);

        // Build pagination and sorting
        Pageable pageable = buildPageable(request);
        log.info("Pageable: page={}, size={}, sort={}",
                pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());

        // Execute search
        log.info("Executing search with specification...");
        Page<Movies> results = movieRepository.findAll(spec, pageable);

        log.info("=== SEARCH COMPLETED ===");
        log.info("Results: {} total movies, {} pages, {} on current page",
                results.getTotalElements(),
                results.getTotalPages(),
                results.getContent().size());

        return results;
    }

    /**
     * DEBUG: Log all request details
     */
    private void logRequestDetails(MovieSearchRequest request) {
        log.info("--- Request Details ---");
        log.info("Query: '{}'", request.getQuery());
        log.info("Genres: {}", request.getGenres());
        log.info("Rating: min={}, max={}", request.getMinRating(), request.getMaxRating());
        log.info("IMDB Rating: min={}, max={}", request.getMinImdbRating(), request.getMaxImdbRating());
        log.info("Year: min={}, max={}", request.getMinYear(), request.getMaxYear());
        log.info("Runtime: min={}, max={}", request.getMinRuntime(), request.getMaxRuntime());
        log.info("  - minRuntime class: {}", request.getMinRuntime() != null ? request.getMinRuntime().getClass().getName() : "NULL");
        log.info("  - maxRuntime class: {}", request.getMaxRuntime() != null ? request.getMaxRuntime().getClass().getName() : "NULL");
        log.info("Vote Count: min={}", request.getMinVoteCount());
        log.info("Director: '{}'", request.getDirector());
        log.info("Actors: {}", request.getActors());
        log.info("Quick Filters - highlyRated: {}, popular: {}, recentlyReleased: {}, shortRuntime: {}",
                request.getHighlyRated(), request.getPopular(), request.getRecentlyReleased(), request.getShortRuntime());
        log.info("Pagination: page={}, size={}", request.getPage(), request.getSize());
        log.info("Sort: {}. {}", request.getSortBy(), request.getSortDirection());
        log.info("--- End Request Details ---");
    }

    /**
     * Apply defaults for quick filters
     */
    private void applyQuickFilterDefaults(MovieSearchRequest request) {
        if (Boolean.TRUE.equals(request.getHighlyRated()) && request.getMinRating() == null) {
            request.setMinRating(7.0);
            log.debug("Applied highlyRated filter: minRating=7.0");
        }

        if (Boolean.TRUE.equals(request.getPopular()) && request.getMinVoteCount() == null) {
            request.setMinVoteCount(1000);
            log.debug("Applied popular filter: minVoteCount=1000");
        }

        if (Boolean.TRUE.equals(request.getRecentlyReleased()) && request.getMinYear() == null) {
            request.setMinYear(Year.now().getValue() - 5);
            log.debug("Applied recentlyReleased filter: minYear={}", request.getMinYear());
        }

        if (Boolean.TRUE.equals(request.getShortRuntime()) && request.getMaxRuntime() == null) {
            request.setMaxRuntime(90);
            log.debug("Applied shortRuntime filter: maxRuntime=90");
        }
    }

    /**
     * Normalize genre names to database standard format
     */
    private void normalizeGenreNames(MovieSearchRequest request) {
        if (request.getGenres() != null && !request.getGenres().isEmpty()) {
            List<String> normalizedGenres = request.getGenres().stream()
                    .map(genre -> {
                        String lower = genre.toLowerCase().trim();
                        String normalized = GENRE_ALIASES.getOrDefault(lower, genre);
                        if (!lower.equals(normalized.toLowerCase())) {
                            log.debug("Genre alias: '{}' -> '{}'", genre, normalized);
                        }
                        return normalized;
                    })
                    .collect(Collectors.toList());
            request.setGenres(normalizedGenres);
            log.info("Normalized genres: {}", normalizedGenres);
        }
    }

    /**
     * Build JPA Specification with data quality improvements - FIXED VERSION
     */
    private Specification<Movies> buildRefinedSpecification(MovieSearchRequest request) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            log.info("=== Building Specification ===");

            // MANDATORY: Exclude adult content
            predicates.add(cb.or(
                    cb.isNull(root.get("adult")),
                    cb.equal(root.get("adult"), false)
            ));
            log.debug("Added: exclude adult content");

            // FILTER: Only include movies with meaningful data
            predicates.add(cb.isNotNull(root.get("title")));
            log.debug("Added: title is not null");

            // Text search in title (indexed)
            if (StringUtils.hasText(request.getQuery())) {
                String searchTerm = request.getQuery().toLowerCase().trim();
                predicates.add(cb.like(cb.lower(root.get("title")), "%" + searchTerm + "%"));
                log.debug("Added: title contains '{}'", searchTerm);
            }

            // Search in overview/description
            if (StringUtils.hasText(request.getOverview())) {
                String overviewTerm = request.getOverview().toLowerCase().trim();
                predicates.add(cb.like(cb.lower(root.get("overview")), "%" + overviewTerm + "%"));
                log.debug("Added: overview contains '{}'", overviewTerm);
            }

            // Search in original title for foreign films
            if (Boolean.TRUE.equals(request.getSearchForeign()) && StringUtils.hasText(request.getQuery())) {
                String searchTerm = request.getQuery().toLowerCase().trim();
                Predicate titleMatch = cb.like(cb.lower(root.get("title")), "%" + searchTerm + "%");
                Predicate originalTitleMatch = cb.like(cb.lower(root.get("originalTitle")), "%" + searchTerm + "%");

                if (!predicates.isEmpty() && predicates.size() > 2) {
                    try {
                        predicates.remove(predicates.size() - 1);
                    } catch (Exception e) {
                        log.debug("Could not remove title predicate");
                    }
                }
                predicates.add(cb.or(titleMatch, originalTitleMatch));
                log.debug("Added: search foreign (title OR originalTitle)");
            }

            // Genre filter - ALL specified genres must be present (indexed: genres_list)
            if (request.getGenres() != null && !request.getGenres().isEmpty()) {
                for (String genre : request.getGenres()) {
                    predicates.add(cb.like(cb.lower(root.get("genresList")), "%" + genre.toLowerCase() + "%"));
                    log.debug("Added: genre contains '{}'", genre);
                }
            }

            // Rating filters (indexed: vote_average)
            if (request.getMinRating() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("voteAverage"),
                        new BigDecimal(request.getMinRating())));
                log.debug("Added: voteAverage >= {}", request.getMinRating());
            }
            if (request.getMaxRating() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("voteAverage"),
                        new BigDecimal(request.getMaxRating())));
                log.debug("Added: voteAverage <= {}", request.getMaxRating());
            }

            // IMDB rating filters (indexed: imdb_rating)
            if (request.getMinImdbRating() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("imdbRating"),
                        new BigDecimal(request.getMinImdbRating())));
                log.debug("Added: imdbRating >= {}", request.getMinImdbRating());
            }
            if (request.getMaxImdbRating() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("imdbRating"),
                        new BigDecimal(request.getMaxImdbRating())));
                log.debug("Added: imdbRating <= {}", request.getMaxImdbRating());
            }

            // Vote count filter (indexed: vote_count)
            if (request.getMinVoteCount() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("voteCount"), request.getMinVoteCount()));
                log.debug("Added: voteCount >= {}", request.getMinVoteCount());
            }

            // Year filters (indexed: release_year)
            if (request.getMinYear() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("releaseYear"),
                        new BigDecimal(request.getMinYear())));
                log.debug("Added: releaseYear >= {}", request.getMinYear());
            }
            if (request.getMaxYear() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("releaseYear"),
                        new BigDecimal(request.getMaxYear())));
                log.debug("Added: releaseYear <= {}", request.getMaxYear());
            }

            // Runtime filters (indexed: runtime) - FIXED VERSION
            log.info("Runtime filter check:");
            log.info("  - minRuntime: {} (type: {})",
                    request.getMinRuntime(),
                    request.getMinRuntime() != null ? request.getMinRuntime().getClass().getSimpleName() : "null");
            log.info("  - maxRuntime: {} (type: {})",
                    request.getMaxRuntime(),
                    request.getMaxRuntime() != null ? request.getMaxRuntime().getClass().getSimpleName() : "null");

            if (request.getMinRuntime() != null || request.getMaxRuntime() != null) {
                log.info("Runtime filter IS ACTIVE - adding predicates");
                // FIXED: Exclude null AND zero runtimes
                predicates.add(cb.and(
                        cb.isNotNull(root.get("runtime")),
                        cb.greaterThan(root.get("runtime"), 0)
                ));
                log.debug("Added: runtime IS NOT NULL AND runtime > 0");

                // Then apply the specific range filters
                if (request.getMinRuntime() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("runtime"), request.getMinRuntime()));
                    log.debug("Added: runtime >= {}", request.getMinRuntime());
                }
                if (request.getMaxRuntime() != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("runtime"), request.getMaxRuntime()));
                    log.debug("Added: runtime <= {}", request.getMaxRuntime());
                }
            } else {
                log.info("Runtime filter NOT ACTIVE - no runtime predicates added");
            }

            // Director filter (indexed: director)
            if (StringUtils.hasText(request.getDirector())) {
                String directorTerm = request.getDirector().toLowerCase().trim();
                Predicate directorMatch = cb.like(cb.lower(root.get("director")), "%" + directorTerm + "%");
                Predicate notUnknown = cb.notLike(cb.lower(root.get("director")), "%unknown%");
                predicates.add(cb.and(directorMatch, notUnknown));
                log.debug("Added: director contains '{}' AND not unknown", directorTerm);
            }

            // Actor filter
            if (request.getActors() != null && !request.getActors().isEmpty()) {
                List<Predicate> actorPredicates = new ArrayList<>();

                for (String actor : request.getActors()) {
                    String normalizedActor = actor.toLowerCase().trim();

                    if (!normalizedActor.equalsIgnoreCase("Unknown") && !normalizedActor.isEmpty()) {
                        actorPredicates.add(cb.like(cb.lower(root.get("star1")), "%" + normalizedActor + "%"));
                        actorPredicates.add(cb.like(cb.lower(root.get("star2")), "%" + normalizedActor + "%"));
                        actorPredicates.add(cb.like(cb.lower(root.get("star3")), "%" + normalizedActor + "%"));
                        actorPredicates.add(cb.like(cb.lower(root.get("star4")), "%" + normalizedActor + "%"));
                        actorPredicates.add(cb.like(cb.lower(root.get("castList")), "%" + normalizedActor + "%"));
                    }
                }

                if (!actorPredicates.isEmpty()) {
                    predicates.add(cb.or(actorPredicates.toArray(new Predicate[0])));
                    log.debug("Added: actor filters for {} actors", request.getActors().size());
                }
            }

            log.info("Total predicates added: {}", predicates.size());
            log.info("=== Specification Complete ===");

            // Combine ALL predicates with AND logic
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Build pageable with sorting
     */
    private Pageable buildPageable(MovieSearchRequest request) {
        String sortProperty = getSortProperty(request.getSortBy());
        Sort.Direction direction = "asc".equalsIgnoreCase(request.getSortDirection())
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Sort sort = Sort.by(direction, sortProperty);
        return PageRequest.of(request.getPage(), request.getSize(), sort);
    }

    /**
     * Map sort parameter to database column
     */
    private String getSortProperty(String sortBy) {
        if (sortBy == null) return "popularity";

        return switch (sortBy.toLowerCase()) {
            case "rating" -> "voteAverage";
            case "imdb" -> "imdbRating";
            case "votes" -> "voteCount";
            case "year" -> "releaseYear";
            case "title" -> "title";
            case "runtime" -> "runtime";
            case "popularity" -> "popularity";
            default -> "popularity";
        };
    }

    /**
     * Convert Page<Movies> to MovieSearchResponse
     */
    public MovieSearchResponse convertToSearchResponse(Page<Movies> moviePage) {
        MovieSearchResponse response = new MovieSearchResponse();

        List<MovieCard> movieCards = moviePage.getContent().stream()
                .map(this::convertToMovieCard)
                .collect(Collectors.toList());

        response.setMovies(movieCards);
        response.setCurrentPage(moviePage.getNumber());
        response.setTotalPages(moviePage.getTotalPages());
        response.setTotalResults(moviePage.getTotalElements());

        return response;
    }

    /**
     * Convert Movies entity to MovieCard DTO with data validation
     */
    public MovieCard convertToMovieCard(Movies movie) {
        MovieCard card = new MovieCard();

        card.setId(movie.getId());
        card.setTitle(movie.getTitle() != null ? movie.getTitle() : "Unknown Title");

        if (movie.getReleaseYear() != null) {
            card.setReleaseYear(movie.getReleaseYear().intValue());
        }

        BigDecimal displayRating = movie.getVoteAverage();
        boolean usingImdb = false;

        if (movie.getImdbRating() != null &&
                movie.getImdbRating().doubleValue() > 0 &&
                (movie.getVoteCount() == null || movie.getVoteCount() < 1000)) {
            displayRating = movie.getImdbRating();
            usingImdb = true;
        }

        if (displayRating == null || displayRating.doubleValue() == 0) {
            displayRating = BigDecimal.ZERO;
        }

        card.setRating(displayRating);
        card.setImdbRating(movie.getImdbRating());
        card.setPopularity(movie.getPopularity() != null ? movie.getPopularity() : BigDecimal.ZERO);
        card.setOverview(movie.getOverview());
        card.setOriginalTitle(movie.getOriginalTitle());
        card.setIsImdbRated(usingImdb);
        card.setVoteCount(movie.getVoteCount() != null ? movie.getVoteCount() : 0L);
        card.setPosterPath(movie.getPosterPath());

        String directorValue = "Unknown";
        if (StringUtils.hasText(movie.getDirector()) &&
                !movie.getDirector().equalsIgnoreCase("Unknown") &&
                !movie.getDirector().equals("")) {
            directorValue = movie.getDirector();
        }
        card.setDirector(directorValue);

        card.setRuntime(movie.getRuntime() != null && movie.getRuntime() > 0 ? movie.getRuntime() : 0);

        if (movie.getGenresList() != null && !movie.getGenresList().isEmpty()) {
            List<String> genreList = Arrays.stream(movie.getGenresList()
                            .replaceAll("[\\[\\]'\"]", "")
                            .split(","))
                    .map(String::trim)
                    .filter(genre -> !genre.isEmpty() && !genre.equalsIgnoreCase("Unknown"))
                    .collect(Collectors.toList());

            if (genreList.isEmpty()) {
                genreList.add("Unknown");
            }
            card.setGenres(genreList);
        } else {
            card.setGenres(Arrays.asList("Unknown"));
        }

        List<String> stars = new ArrayList<>();
        if (movie.getStar1() != null && !movie.getStar1().equalsIgnoreCase("Unknown") && !movie.getStar1().isEmpty())
            stars.add(movie.getStar1());
        if (movie.getStar2() != null && !movie.getStar2().equalsIgnoreCase("Unknown") && !movie.getStar2().isEmpty())
            stars.add(movie.getStar2());
        if (movie.getStar3() != null && !movie.getStar3().equalsIgnoreCase("Unknown") && !movie.getStar3().isEmpty())
            stars.add(movie.getStar3());
        if (movie.getStar4() != null && !movie.getStar4().equalsIgnoreCase("Unknown") && !movie.getStar4().isEmpty())
            stars.add(movie.getStar4());

        card.setMainStars(stars.toArray(new String[0]));

        return card;
    }
    public Movies updateMovie(Long movieId, Movies movieDetails) {
        log.info("Updating movie with id: {}", movieId);

        Movies movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new RuntimeException("Movie not found with id: " + movieId));

        // Update fields
        if (movieDetails.getTitle() != null) {
            movie.setTitle(movieDetails.getTitle());
        }
        if (movieDetails.getDirector() != null) {
            movie.setDirector(movieDetails.getDirector());
        }
        if (movieDetails.getOverview() != null) {
            movie.setOverview(movieDetails.getOverview());
        }
        if (movieDetails.getReleaseYear() != null) {
            movie.setReleaseYear(movieDetails.getReleaseYear());
        }
        if (movieDetails.getRuntime() != null) {
            movie.setRuntime(movieDetails.getRuntime());
        }
        if (movieDetails.getVoteAverage() != null) {
            movie.setVoteAverage(movieDetails.getVoteAverage());
        }
        if (movieDetails.getGenresList() != null) {
            movie.setGenresList(movieDetails.getGenresList());
        }
        if (movieDetails.getPosterPath() != null) {
            movie.setPosterPath(movieDetails.getPosterPath());
        }

        Movies updatedMovie = movieRepository.save(movie);
        log.info("Movie updated successfully");

        return updatedMovie;
    }

    /**
     * Delete a movie (ADMIN only)
     * This will cascade delete all reviews and watchlist entries
     */
    public void deleteMovie(Long movieId) {
        log.info("Deleting movie with id: {}", movieId);

        if (!movieRepository.existsById(movieId)) {
            throw new RuntimeException("Movie not found with id: " + movieId);
        }

        // Cascade deletes will be handled by database constraints
        movieRepository.deleteById(movieId);

        log.info("Movie deleted successfully");
    }

    /**
     * Get movie count (useful for admin dashboard)
     */
    public long getMovieCount() {
        return movieRepository.count();
    }

    /**
     * Get total movies by genre
     */
    public long getMovieCountByGenre(String genre) {
        return movieRepository.count((root, query, cb) ->
                cb.like(cb.lower(root.get("genresList")), "%" + genre.toLowerCase() + "%"));
    }

    /**
     * Get movies with high ratings (admin analytics)
     */
    public List<Movies> getHighRatedMovies(BigDecimal minRating) {
        return movieRepository.findAll((root, query, cb) ->
                cb.greaterThanOrEqualTo(root.get("voteAverage"), minRating));
    }
}