package com.example.neo4jdemo.repository;

import com.example.neo4jdemo.entity.PersonEntity;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PersonRepository extends Neo4jRepository<PersonEntity, Long> {
    PersonEntity findPersonEntityByName(String name);
}
