package com.isfa.dsi.filmexplorer.services;

import com.isfa.dsi.filmexplorer.models.Movies;
import com.isfa.dsi.filmexplorer.repos.MoviesRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CsvService {

    private final MoviesRepo movieRepository;

    // CSV Headers for export
    private static final String[] CSV_HEADERS = {
            "id", "title", "release_year", "vote_average", "vote_count",
            "runtime", "director", "genres_list", "overview", "poster_path",
            "imdb_rating", "popularity", "star1", "star2", "star3", "star4"
    };


    @Transactional
    public ImportResult importMoviesFromCsv(MultipartFile file) throws IOException {
        log.info("Starting CSV import from file: {}", file.getOriginalFilename());

        List<Movies> moviesToSave = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int lineNumber = 0;
        int successCount = 0;
        int errorCount = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            // Read header line
            String headerLine = reader.readLine();
            lineNumber++;

            if (headerLine == null) {
                throw new IllegalArgumentException("CSV file is empty");
            }

            log.info("CSV Headers: {}", headerLine);

            // Parse header to get column indices (handles any column order)
            Map<String, Integer> columnIndex = parseHeader(headerLine);
            log.info("Column indices: {}", columnIndex);

            // Read data lines
            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;

                // Skip empty lines
                if (line.trim().isEmpty()) {
                    continue;
                }

                try {
                    Movies movie = parseCsvLine(line, lineNumber, columnIndex);
                    moviesToSave.add(movie);
                    successCount++;

                    // Batch save every 100 movies for performance
                    if (moviesToSave.size() >= 100) {
                        movieRepository.saveAll(moviesToSave);
                        log.info("Saved batch of {} movies", moviesToSave.size());
                        moviesToSave.clear();
                    }

                } catch (Exception e) {
                    errorCount++;
                    String error = String.format("Line %d: %s - %s", lineNumber, line, e.getMessage());
                    errors.add(error);
                    log.error("Error parsing line {}: {}", lineNumber, e.getMessage());
                }
            }

            // Save remaining movies
            if (!moviesToSave.isEmpty()) {
                movieRepository.saveAll(moviesToSave);
                log.info("Saved final batch of {} movies", moviesToSave.size());
            }

        } catch (IOException e) {
            log.error("Error reading CSV file: {}", e.getMessage());
            throw e;
        }

        log.info("CSV import completed: {} successful, {} errors", successCount, errorCount);

        return ImportResult.builder()
                .successCount(successCount)
                .errorCount(errorCount)
                .totalLines(lineNumber)
                .errors(errors)
                .build();
    }

    /**
     * Parse CSV header and map column names to indices
     * Handles both standard format and pgAdmin export format
     */
    private Map<String, Integer> parseHeader(String headerLine) {
        Map<String, Integer> columnIndex = new HashMap<>();
        String[] columns = parseCSVFields(headerLine);

        for (int i = 0; i < columns.length; i++) {
            String colName = columns[i].trim().toLowerCase().replaceAll("\"", "");
            columnIndex.put(colName, i);
        }

        return columnIndex;
    }

    /**
     * Parse CSV fields properly handling quoted fields with commas
     */
    private String[] parseCSVFields(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean insideQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                insideQuotes = !insideQuotes;
            } else if (c == ',' && !insideQuotes) {
                fields.add(currentField.toString());
                currentField = new StringBuilder();
            } else {
                currentField.append(c);
            }
        }
        fields.add(currentField.toString());

        return fields.toArray(new String[0]);
    }

    /**
     * Parse a single CSV line using column indices
     * Supports both standard and pgAdmin export formats
     */
    private Movies parseCsvLine(String line, int lineNumber, Map<String, Integer> columnIndex) {
        String[] fields = parseCSVFields(line);

        Movies movie = new Movies();

        try {
            // Try to get ID from column index
            Integer idIdx = columnIndex.get("id");
            if (idIdx != null && idIdx < fields.length) {
                String idStr = getValue(fields, idIdx);
                if (!idStr.isEmpty()) {
                    movie.setId(Long.parseLong(idStr));
                }
            }

            // Title (required - try different column names)
            String titleStr = getValueByColumnNames(fields, columnIndex, "title");
            if (titleStr != null) {
                movie.setTitle(titleStr);
            } else {
                throw new IllegalArgumentException("Title not found in CSV");
            }

            // Release Year
            String releaseYearStr = getValueByColumnNames(fields, columnIndex, "release_year");
            if (releaseYearStr != null) {
                movie.setReleaseYear(new BigDecimal(releaseYearStr));
            }

            // Vote Average
            String voteAvgStr = getValueByColumnNames(fields, columnIndex, "vote_average");
            if (voteAvgStr != null) {
                movie.setVoteAverage(new BigDecimal(voteAvgStr));
            }

            // Vote Count
            String voteCountStr = getValueByColumnNames(fields, columnIndex, "vote_count");
            if (voteCountStr != null) {
                movie.setVoteCount(Long.parseLong(voteCountStr));
            }

            // Runtime
            String runtimeStr = getValueByColumnNames(fields, columnIndex, "runtime");
            if (runtimeStr != null) {
                movie.setRuntime(Integer.parseInt(runtimeStr));
            }

            // Director
            String directorStr = getValueByColumnNames(fields, columnIndex, "director");
            movie.setDirector(directorStr != null ? directorStr : "Unknown");

            // Genres List
            String genresStr = getValueByColumnNames(fields, columnIndex, "genres_list");
            if (genresStr != null) {
                movie.setGenresList(genresStr);
            }

            // Overview
            String overviewStr = getValueByColumnNames(fields, columnIndex, "overview");
            if (overviewStr != null) {
                movie.setOverview(overviewStr);
            }

            // Poster Path (CRITICAL!)
            String posterPathStr = getValueByColumnNames(fields, columnIndex, "poster_path");
            if (posterPathStr != null) {
                movie.setPosterPath(posterPathStr);
                log.debug("Poster path set: {} for movie: {}", posterPathStr, movie.getTitle());
            } else {
                log.debug("No poster_path for movie: {}", movie.getTitle());
            }

            // IMDB Rating
            String imdbRatingStr = getValueByColumnNames(fields, columnIndex, "imdb_rating");
            if (imdbRatingStr != null) {
                try {
                    movie.setImdbRating(new BigDecimal(imdbRatingStr));
                } catch (NumberFormatException e) {
                    log.debug("Invalid imdb_rating: {}", imdbRatingStr);
                }
            }

            // Popularity
            String popularityStr = getValueByColumnNames(fields, columnIndex, "popularity");
            if (popularityStr != null) {
                try {
                    movie.setPopularity(new BigDecimal(popularityStr));
                } catch (NumberFormatException e) {
                    log.debug("Invalid popularity: {}", popularityStr);
                }
            }

            // Stars
            String star1Str = getValueByColumnNames(fields, columnIndex, "star1");
            if (star1Str != null) {
                movie.setStar1(star1Str);
            }

            String star2Str = getValueByColumnNames(fields, columnIndex, "star2");
            if (star2Str != null) {
                movie.setStar2(star2Str);
            }

            String star3Str = getValueByColumnNames(fields, columnIndex, "star3");
            if (star3Str != null) {
                movie.setStar3(star3Str);
            }

            String star4Str = getValueByColumnNames(fields, columnIndex, "star4");
            if (star4Str != null) {
                movie.setStar4(star4Str);
            }

            // Set defaults if not set
            if (movie.getAdult() == null) {
                movie.setAdult(false);
            }
            if (movie.getStatus() == null) {
                movie.setStatus("Released");
            }

        } catch (Exception e) {
            throw new IllegalArgumentException("Error parsing fields: " + e.getMessage(), e);
        }

        return movie;
    }

    /**
     * Get value by trying multiple column name variations
     * This handles both standard and pgAdmin column names
     */
    private String getValueByColumnNames(String[] fields, Map<String, Integer> columnIndex, String... columnNames) {
        for (String colName : columnNames) {
            String normalized = colName.toLowerCase();
            Integer idx = columnIndex.get(normalized);
            if (idx != null && idx < fields.length) {
                String value = getValue(fields, idx);
                if (value != null && !value.isEmpty()) {
                    return value;
                }
            }
        }
        return null;
    }

    /**
     * Safely get and clean value from fields array
     */
    private String getValue(String[] fields, int index) {
        if (index >= fields.length) {
            return "";
        }
        return cleanField(fields[index]);
    }

    /**
     * Export movies to CSV
     */
    public String exportMoviesToCsv(List<Movies> movies) {
        log.info("Exporting {} movies to CSV", movies.size());

        StringBuilder csv = new StringBuilder();

        // Add header
        csv.append(String.join(",", CSV_HEADERS)).append("\n");

        // Add data rows
        for (Movies movie : movies) {
            csv.append(formatCsvRow(movie)).append("\n");
        }

        log.info("CSV export completed: {} movies", movies.size());
        return csv.toString();
    }

    /**
     * Format a movie as a CSV row
     */
    private String formatCsvRow(Movies movie) {
        return String.join(",",
                escape(movie.getId() != null ? movie.getId().toString() : ""),
                escape(movie.getTitle()),
                escape(movie.getReleaseYear() != null ? movie.getReleaseYear().toString() : ""),
                escape(movie.getVoteAverage() != null ? movie.getVoteAverage().toString() : ""),
                escape(movie.getVoteCount() != null ? movie.getVoteCount().toString() : ""),
                escape(movie.getRuntime() != null ? movie.getRuntime().toString() : ""),
                escape(movie.getDirector()),
                escape(movie.getGenresList()),
                escape(movie.getOverview()),
                escape(movie.getPosterPath()),
                escape(movie.getImdbRating() != null ? movie.getImdbRating().toString() : ""),
                escape(movie.getPopularity() != null ? movie.getPopularity().toString() : ""),
                escape(movie.getStar1()),
                escape(movie.getStar2()),
                escape(movie.getStar3()),
                escape(movie.getStar4())
        );
    }

    /**
     * Escape field for CSV (add quotes if contains comma, quote, or newline)
     */
    private String escape(String field) {
        if (field == null) {
            return "";
        }

        // If field contains comma, quote, or newline, wrap in quotes
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            // Escape quotes by doubling them
            field = field.replace("\"", "\"\"");
            return "\"" + field + "\"";
        }

        return field;
    }

    /**
     * Clean field value (remove quotes, trim, handle null)
     */
    private String cleanField(String field) {
        if (field == null || field.trim().isEmpty()) {
            return null;
        }
        // Remove surrounding quotes if present
        field = field.trim();
        if (field.startsWith("\"") && field.endsWith("\"")) {
            field = field.substring(1, field.length() - 1);
        }
        // Unescape doubled quotes
        field = field.replace("\"\"", "\"");
        return field.isEmpty() ? null : field;
    }

    /**
     * Result DTO for import
     */
    @lombok.Data
    @lombok.Builder
    public static class ImportResult {
        private int successCount;
        private int errorCount;
        private int totalLines;
        private List<String> errors;

        public String getSummary() {
            return String.format("Imported %d movies successfully, %d errors out of %d lines",
                    successCount, errorCount, totalLines);
        }
    }
}