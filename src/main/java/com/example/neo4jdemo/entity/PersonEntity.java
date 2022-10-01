package com.example.neo4jdemo.entity;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;


@Node("Person")
@Data
public class PersonEntity {
    @Id
    @GeneratedValue
    private Long id;
    private String name;
    private Integer born;
    public PersonEntity(Integer born, String name) {
        this.name = name;
        this.born = born;
    }
}
