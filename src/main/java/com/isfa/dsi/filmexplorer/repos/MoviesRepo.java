package com.isfa.dsi.filmexplorer.repos;

import com.isfa.dsi.filmexplorer.models.Movies;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.Optional;

@CrossOrigin("http://localhost:4200/")
@Repository
public interface MoviesRepo  extends JpaRepository<Movies, Long> , JpaSpecificationExecutor<Movies> {



}
