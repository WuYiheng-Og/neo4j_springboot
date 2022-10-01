package com.example.neo4jdemo.repository;

import com.example.neo4jdemo.entity.MovieEntity;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovieRepository extends Neo4jRepository<MovieEntity, Long> {

//    @Query("MATCH (n:Movie) WHERE id(n) = $0 RETURN n")
    List<MovieEntity> findMovieEntitiesById(Long id);
    MovieEntity findMovieEntityByTitle(String title);
}
