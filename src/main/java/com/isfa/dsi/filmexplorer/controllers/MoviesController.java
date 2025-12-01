package com.isfa.dsi.filmexplorer.controllers;

import com.isfa.dsi.filmexplorer.DTOs.MovieCard;
import com.isfa.dsi.filmexplorer.DTOs.MovieSearchRequest;
import com.isfa.dsi.filmexplorer.DTOs.MovieSearchResponse;
import com.isfa.dsi.filmexplorer.models.Movies;
import com.isfa.dsi.filmexplorer.repos.MoviesRepo;
import com.isfa.dsi.filmexplorer.services.CsvService;
import com.isfa.dsi.filmexplorer.services.MovieService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/movies")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:4200")
public class MoviesController {

    private final MovieService movieService;
    private final MoviesRepo movieRepository;
    private final CsvService csvService;


    @PostMapping("/search")
    public ResponseEntity<MovieSearchResponse> searchMovies(@RequestBody MovieSearchRequest searchRequest) {
        long startTime = System.currentTimeMillis();

        log.info("Search request: query='{}', genres={}, minRating={}, minYear={}, actors={}",
                searchRequest.getQuery(),
                searchRequest.getGenres(),
                searchRequest.getMinRating(),
                searchRequest.getMinYear(),
                searchRequest.getActors());

        // Execute search
        Page<Movies> moviePage = movieService.searchMovies(searchRequest);
        MovieSearchResponse response = movieService.convertToSearchResponse(moviePage);

        // Add metadata
        long searchTime = System.currentTimeMillis() - startTime;
        response.setSearchTimeMs(searchTime);
        response.setSearchQuery(searchRequest.getQuery());
        response.setAppliedFilters(buildFilterSummary(searchRequest));
        response.setSortedBy(searchRequest.getSortBy() != null ? searchRequest.getSortBy() : "popularity");
        response.setHasMoreResults(response.getCurrentPage() < response.getTotalPages() - 1);

        log.info("Search completed: {} results in {}ms", response.getTotalResults(), searchTime);

        return ResponseEntity.ok(response);
    }


    @GetMapping("/{id}")
    public ResponseEntity<MovieCard> getMovieById(@PathVariable Long id) {
        log.info("Fetching movie details for id: {}", id);

        Optional<Movies> movieOptional = movieRepository.findById(id);

        if (movieOptional.isEmpty()) {
            log.warn("Movie not found with id: {}", id);
            return ResponseEntity.notFound().build();
        }

        MovieCard movieCard = movieService.convertToMovieCard(movieOptional.get());
        return ResponseEntity.ok(movieCard);
    }


    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> importMoviesFromCsv(@RequestParam("file") MultipartFile file) {
        log.info("Admin: Importing movies from CSV file: {}", file.getOriginalFilename());


        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is empty");
        }

        if (!file.getOriginalFilename().endsWith(".csv")) {
            return ResponseEntity.badRequest().body("File must be a CSV");
        }

        try {

            CsvService.ImportResult result = csvService.importMoviesFromCsv(file);

            log.info("Import completed: {}", result.getSummary());


            return ResponseEntity.ok(result);

        } catch (IOException e) {
            log.error("Error reading CSV file: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error reading file: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error importing movies: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error importing movies: " + e.getMessage());
        }
    }


    @PostMapping("/export")
    public ResponseEntity<String> exportMoviesToCsv(@RequestBody MovieSearchRequest searchRequest) {
        log.info("Exporting movies to CSV with search criteria");

        try {

            searchRequest.setSize(searchRequest.getSize() > 0 ? searchRequest.getSize() : 10000);

            Page<Movies> moviePage = movieService.searchMovies(searchRequest);
            List<Movies> movies = moviePage.getContent();

            log.info("Exporting {} movies to CSV", movies.size());

            String csvContent = csvService.exportMoviesToCsv(movies);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv"));
            headers.setContentDispositionFormData("attachment", "movies_export.csv");
            headers.setCacheControl("no-cache, no-store, must-revalidate");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(csvContent);

        } catch (Exception e) {
            log.error("Error exporting movies: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error exporting movies: " + e.getMessage());
        }
    }


    @GetMapping("/export/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> exportAllMovies() {
        log.info("Admin: Exporting all movies to CSV");

        try {
            List<Movies> movies = movieRepository.findAll();
            log.info("Exporting {} movies to CSV", movies.size());


            String csvContent = csvService.exportMoviesToCsv(movies);


            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv"));
            headers.setContentDispositionFormData("attachment", "all_movies_export.csv");
            headers.setCacheControl("no-cache, no-store, must-revalidate");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(csvContent);

        } catch (Exception e) {
            log.error("Error exporting all movies: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error exporting movies: " + e.getMessage());
        }
    }


    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Movies> updateMovie(
            @PathVariable Long id,
            @RequestBody Movies movieDetails) {
        log.info("Admin: Updating movie with id: {}", id);

        try {
            Movies updatedMovie = movieService.updateMovie(id, movieDetails);
            log.info("Movie updated successfully: {}", id);
            return ResponseEntity.ok(updatedMovie);
        } catch (Exception e) {
            log.error("Error updating movie: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }


    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteMovie(@PathVariable Long id) {
        log.info("Admin: Deleting movie with id: {}", id);

        try {
            movieService.deleteMovie(id);
            log.info("Movie deleted successfully: {}", id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error deleting movie: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }


    @GetMapping("/admin/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MovieStats> getMovieStats() {
        log.info("Admin: Fetching movie statistics");

        try {
            long totalMovies = movieService.getMovieCount();

            MovieStats stats = MovieStats.builder()
                    .totalMovies(totalMovies)
                    .build();

            log.info("Movie stats fetched: totalMovies={}", totalMovies);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error fetching movie stats: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }



    @lombok.Data
    @lombok.Builder
    public static class MovieStats {
        private long totalMovies;
    }


    private String buildFilterSummary(MovieSearchRequest request) {
        StringBuilder filters = new StringBuilder();

        if (request.getQuery() != null) {
            filters.append("Query: '").append(request.getQuery()).append("', ");
        }
        if (request.getGenres() != null && !request.getGenres().isEmpty()) {
            filters.append("Genres: ").append(String.join(", ", request.getGenres())).append(", ");
        }
        if (request.getMinRating() != null) {
            filters.append("Rating ≥ ").append(request.getMinRating()).append(", ");
        }
        if (request.getMaxRating() != null) {
            filters.append("Rating ≤ ").append(request.getMaxRating()).append(", ");
        }
        if (request.getMinImdbRating() != null) {
            filters.append("IMDB ≥ ").append(request.getMinImdbRating()).append(", ");
        }
        if (request.getMinYear() != null) {
            filters.append("Year ≥ ").append(request.getMinYear()).append(", ");
        }
        if (request.getMaxYear() != null) {
            filters.append("Year ≤ ").append(request.getMaxYear()).append(", ");
        }
        if (request.getDirector() != null) {
            filters.append("Director: ").append(request.getDirector()).append(", ");
        }
        if (request.getActors() != null && !request.getActors().isEmpty()) {
            filters.append("Actors: ").append(String.join(", ", request.getActors())).append(", ");
        }
        if (Boolean.TRUE.equals(request.getHighlyRated())) {
            filters.append("Highly Rated, ");
        }
        if (Boolean.TRUE.equals(request.getPopular())) {
            filters.append("Popular, ");
        }

        String result = filters.toString();
        return result.isEmpty() ? "No filters" : result.substring(0, result.length() - 2);
    }
}