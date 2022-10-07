package com.example.neo4jdemo.entity;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Movie实体类，一个neo4j的节点
 * 注意到除了id外的属性均
 */
@Node(labels = "Movie") // 标签名，labels可以缺省
@Data
public class MovieEntity {

    @Id
    @GeneratedValue // Id自增
    private Long id;

    private final String title;

    @Property("tagline") // 映射到neo4j的属性名
    private final String description;

    public MovieEntity(String title, String description) {
        this.id = null;
        this.title = title;
        this.description = description;
    }

    // 用户指定特定的Id
    public MovieEntity withId(Long id) {
        if (this.id!= null && this.id.equals(id)) {
            return this;
        } else {
            MovieEntity newObject = new MovieEntity(this.title, this.description);
            newObject.id = id;
            return newObject;
        }
    }

    // 定义一个关系（参演）注意directiond代表箭头方向，INCOMING箭头指向自己，OUTGOING箭头指向TargetNode
    @Relationship(type = "ACTED_IN", direction = Relationship.Direction.INCOMING)
    private List<Roles> actorsAndRoles = new ArrayList<>();
    // 定义一个关系（导演）
    @Relationship(type = "DIRECTED", direction = Relationship.Direction.INCOMING)
    private List<PersonEntity> directors = new ArrayList<>();
    // 注意这些关系最终的箭头指向（INCOMING）是当前实体，即TargetNode（PersonEntity）->当前定义Relationship的实体（MovieEntity）
}
