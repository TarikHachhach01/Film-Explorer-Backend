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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

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

            // Read data lines
            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;

                // Skip empty lines
                if (line.trim().isEmpty()) {
                    continue;
                }

                try {
                    Movies movie = parseCsvLine(line, lineNumber);
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


    private Movies parseCsvLine(String line, int lineNumber) {
        // Split by comma, but handle commas in quoted fields
        String[] fields = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);

        if (fields.length < 6) {
            throw new IllegalArgumentException("Insufficient fields (minimum 6 required)");
        }

        Movies movie = new Movies();

        try {
            // Required fields
            movie.setTitle(cleanField(fields[0]));
            movie.setReleaseYear(parseBigDecimal(fields[1]));
            movie.setVoteAverage(parseBigDecimal(fields[2]));
            movie.setVoteCount(parseLong(fields[3]));
            movie.setRuntime(parseInteger(fields[4]));
            movie.setDirector(cleanField(fields[5]));

            // Optional fields
            if (fields.length > 6) movie.setGenresList(cleanField(fields[6]));
            if (fields.length > 7) movie.setOverview(cleanField(fields[7]));
            if (fields.length > 8) movie.setPosterPath(cleanField(fields[8]));
            if (fields.length > 9) movie.setImdbRating(parseBigDecimal(fields[9]));
            if (fields.length > 10) movie.setStar1(cleanField(fields[10]));
            if (fields.length > 11) movie.setStar2(cleanField(fields[11]));
            if (fields.length > 12) movie.setStar3(cleanField(fields[12]));
            if (fields.length > 13) movie.setStar4(cleanField(fields[13]));

            // Set defaults
            movie.setAdult(false);
            movie.setPopularity(BigDecimal.ZERO);
            movie.setStatus("Released");

        } catch (Exception e) {
            throw new IllegalArgumentException("Error parsing fields: " + e.getMessage());
        }

        return movie;
    }


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


    private BigDecimal parseBigDecimal(String value) {
        String cleaned = cleanField(value);
        if (cleaned == null || cleaned.isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }


    private Long parseLong(String value) {
        String cleaned = cleanField(value);
        if (cleaned == null || cleaned.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }


    private Integer parseInteger(String value) {
        String cleaned = cleanField(value);
        if (cleaned == null || cleaned.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }


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