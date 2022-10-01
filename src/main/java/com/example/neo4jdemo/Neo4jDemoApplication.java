package com.example.neo4jdemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;

@SpringBootApplication
@EnableNeo4jRepositories("com.example.neo4jdemo.repository")
public class Neo4jDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(Neo4jDemoApplication.class, args);
    }

}
