# SpringBoot整合Neo4j，最新版本适用，超通俗详细讲解

## 0.前言

	跟着班导师做项目的时候遇到社交网络的部分，而传统的关系数据库不能很好解决数据之间的关系，因此采用图数据的方式进行存储。Neo4j算是主流的图数据库，多应用于知识图谱、社交网络等。
	
	这两天学习SpringBoot时碰到了很多问题

- [springboot集合neo4j引用了org.neo4j的包，报错Required identifier property not found for class](https://ask.csdn.net/questions/7658608)
- [用SpringBoot集成neo4j，查询报错Could not find mappable nodes or relationships inside Record](https://ask.csdn.net/questions/7659061)
- org.springframework.data.neo4j.core.schema中没有@NodeEntity，@StartNode，@EndNode
- RelationShip无法注解在实体关系类中
- nested exception is org.springframework.beans.BeanInstantiationException: Failed to instantiate [org.*neo4j*.ogm.session.*SessionFactory*] sessionFactory找不到

看了[(128条消息) 【最新】Neo4j官方建议Spring Boot 2.4.及以上版本用Neo4j Java Driver代替The Spring Boot starter_学到一寸是一寸的博客-CSDN博客](https://blog.csdn.net/ccnice99/article/details/123420884)的博客后才知道是因为**SpringBoot版本较高（2.4以上）时集成的Neo4j的API规则方法在变化**。但是他的博客还是没有说明如何使用类似@Node的注解，自己特意去查了Neo4j官方文档和Spring Neo4j的官方文档并进行总结整理发出来。

---------

本文将解决的问题

- SpringBoot高版本（2.4以上）+Neo4j的配置
- 使用@Node等注解操作简化Neo4j
- 一些简单的cypherQuery（很类似MySQL的SQL语句）用法解释
- 利用Repository（类似MyBatisPlus操作的Mapper接口）对图数据进行CRUD
- 不使用Repository而使用Neo4jTemplate直接对图数据进行CRUD

TODO

- id的生成
- 分页查询

---

本文将会结合官网的一个 【导演-电影-演员】关系图来进行实现。以新海诚导演的《你的名字》为例。

![](https://img-blog.csdnimg.cn/13e802a8204a446b8db456e93c9adbf2.png)


```json
[
  {
    "n": {
"identity": 10,
"labels": [
        "Movie"
      ],
"properties": {
"tagline": "影片讲述了男女高中生在梦中相遇，并寻找彼此的故事。",
"title": "你的名字"
      }
    }
  },
  {
    "n": {
"identity": 11,
"labels": [
        "Person"
      ],
"properties": {
"born": 1997,
"name": "上白石萌音"
      }
    }
  },
  {
    "n": {
"identity": 12,
"labels": [
        "Person"
      ],
"properties": {
"born": 1993,
"name": "神木隆之介"
      }
    }
  },
  {
    "n": {
"identity": 13,
"labels": [
        "Person"
      ],
"properties": {
"born": 1973,
"name": "新海诚"
      }
    }
  }
]
```



## 1.初始化Spring Boot项目添加依赖

**注：本文所使用SpringBoot版本为2.7.4，Java8；（Java8+均可）,neo4j的安装不再赘述，请提前安装配置好**

### 1.初始化项目，添加依赖

指定对应的SpringBoot版本（注意本文适配2.4版本及以上）和相应的依赖，当然也可以后续pom依赖中添加坐标（注意pom有两个neo4j相关的依赖）

![在这里插入图片描述](https://img-blog.csdnimg.cn/2e64e34ec03248dd979c24950912febe.png)


```xml
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>2.7.4</version>
        <relativePath/> <!-- lookup parent from repository -->
    </parent>
    <groupId>com.example</groupId>
    <artifactId>Neo4jDemo</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>Neo4jDemo</name>
    <description>Neo4jDemo</description>
    <properties>
        <java.version>8</java.version>
    </properties>

    <dependencies>
        <!-- neo4j 驱动 这个需要自己手动添加一下 -->
        <dependency>
            <groupId>org.neo4j.driver</groupId>
            <artifactId>neo4j-java-driver</artifactId>
        </dependency>
        <!-- neo4j 操作实体注解需要 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-neo4j</artifactId>
        </dependency>

        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
```

### 2.配置Neo4j

```yaml
spring:
  neo4j:
    uri: bolt://<YourNeo4jIpAddress>:7687
    authentication:
      username: <yourUserName>
      password: <yourPassword>
# 指定数据库
  data:
    neo4j:
      database: <yourDatabase>
```

创建`utils`包，并在该包下创建`ExampleCommandLineRunner`来装配Driver和Session

```java
package com.example.neo4jdemo.utils;

import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ExampleCommandLineRunner implements CommandLineRunner {

    private final Driver driver;
    private final ConfigurableApplicationContext applicationContext;
    public final Session session;

    @Bean
    Session session(){
        return session;
    }

    // Autowire the Driver bean by constructor injection
    public ExampleCommandLineRunner(Driver driver, ConfigurableApplicationContext applicationContext) {
        this.driver = driver;
        this.applicationContext = applicationContext;
        this.session = driver.session();

    }

    @Override
    public void run(String... args) throws Exception {
    }
}
```

## 2.创建实体类节点

### 1.节点

节点介绍：

![在这里插入图片描述](https://img-blog.csdnimg.cn/57d40a76d9064a929e99b21b5688881c.png)


创建`entity`包，添加实体类：`PersonEntity`和`MovieEntity`

```java
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

```

```java
package com.example.neo4jdemo.entity;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Movie实体类，一个neo4j的节点 
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
        this.id = null;// 生成node时自动生成
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
}
```



> 1.withId在需要指定节点id而非自动生成时使用。
>
> 2.注意到除了id外的属性均被final修饰，一种构造优化，提高执行效率:(来自[Spring Data Neo4j](https://docs.spring.io/spring-data/neo4j/docs/current/reference/html/#faq.path-mapping))
>
> This gives us a roundabout 25% performance boost over reflection. For the domain class to be eligible for such optimization, it needs to adhere to a set of constraints:
>
> - Types must not reside in the default or under the `java` package.
> - Types and their constructors must be `public`
> - Types that are inner classes must be `static`.
> - The used Java Runtime must allow for declaring classes in the originating `ClassLoader`. Java 9 and newer impose certain limitations.
>
> By default, Spring Data attempts to use generated property accessors and falls back to reflection-based ones if a limitation is detected.
>
> 

### 2.节点间的关系

关系解释：（以ACTED_IN即参演关系为例）

![在这里插入图片描述](https://img-blog.csdnimg.cn/2e113747692c49f2a4dec5b32426d59f.png)


创建`Roles`和完善`MovieEntity`

```java
package com.example.neo4jdemo.entity;

import org.springframework.data.neo4j.core.schema.RelationshipId;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

import java.util.List;

/**
 * 定义一个关系属性
 */
@RelationshipProperties
public class Roles {

    @RelationshipId
    private Long id;

    private final List<String> roles;

    @TargetNode // 相当于@StartNode
    private final PersonEntity person;

    // 参数1是目标关系实体节点 参数2是关系属性
    //    Roles 参数1：Person实体，演员的出生年和姓名；参数2：演员名字列表（考虑到一个演员可能参演多个角色）
    public Roles(PersonEntity person, List<String> roles) {
        this.person = person;
        this.roles = roles;
    }

    public List<String> getRoles() {
        return roles;
    }
}

```



注意这些关系**`@TargetNode`修饰的是关系箭头的尾部, 最终的箭头指向是当前实体**，即`TargetNode（PersonEntity）->当前定义Relationship的实体（MovieEntity）`



```java
package com.example.neo4jdemo.entity;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Movie实体类，一个neo4j的节点 
 */
@Node(labels = "Movie") // 标签名，labels可以缺省
@Data
public class MovieEntity {

    ...(上面 1.节点 有)

    // 定义一个关系（参演）
    @Relationship(type = "ACTED_IN", direction = Relationship.Direction.INCOMING)
    private List<Roles> actorsAndRoles = new ArrayList<>();
    // 定义一个关系（导演）
    @Relationship(type = "DIRECTED", direction = Relationship.Direction.INCOMING)
    private List<PersonEntity> directors = new ArrayList<>();
    // 注意这些关系最终的箭头指向是当前实体，即TargetNode（PersonEntity）->当前定义Relationship的实体（MovieEntity）

}
```

## 3.CRUD

#### Cypher Query基本使用

了解即可，有封装好的api使用，可以先跳过不看，看3.b部分。

>1. **创建**
>
>```cypher
># 查询name等于 $name 的 label为Person的实体类集合
>MATCH (n:Person {name: $name}) RETURN n
>或者
>MATCH (n:Person) WHERE n.name = $name RETURN n 
>```
>
>1. n是一个变量
>2. $name 对应map的key
>3. { } 是 where 筛选的简写
>4. Return 返回符合筛选条件的变量n
>
>
>
>2. **查找**
>
>```cypher
># 查询Person和Movie之间 关系种类为ACTED_IN且关系属性role = $roles 的实体类集合
>MATCH (person:Person) -[ relation:ACTED_IN ]-> (movie:Movie) 
>WHERE relation.roles = $roles
>RETURN person
>等价于
>MATCH (person:Person) -[ relation:ACTED_IN ]-> (:Movie) 
>WHERE relation.roles = $roles
>RETURN person                                         
>```
>
>1. person、relation、movie是变量
>2. :Person 、:Movie 用：后面接Label，`person:Person`有点类似变量声明，如果不使用完全可以省略不写
>3. 用-[]-> 来表示关系的type以及指向
>
>
>
>3. **修改**
>
>```cypher
># 新增属性(Person本来只有name和born属性，新增age属性，一般不用)
>MATCH (n:Person) 
>WHERE n.name = '新海诚'
>SET n.age = 50
>
># 修改属性
>MATCH (n:Person) 
>WHERE n.name = "新津城"
>SET n.name = "新海诚"
>
># 新增/修改多个属性
>MATCH (n:Person) 
>WHERE n.name = '新海诚'
>SET n.age = 50, n.name="新津城"
>```
>
>
>
>4. **删除**
>
>```cypher
># 删除属性（一般不用）
>MATCH (n:Person) 
>WHERE n.name = "新津城"
>REMOVE n.age
># 删除关系（新海诚的导演关系删除）
>MATCH (n:Person) -[k:DIRECTED]-> (m:Movie)
>WHERE n="新海诚"
>DELETE k
># 删除节点
>MATCH (n:Person)
>WHERE n.name = "新海诚"
>DELETE n // 当该实体仍有关系时会报错，必须先删除关系再删除节点
>
># 删除节点（会删除节点实体和他关联的属性）
>MATCH (n:Person)
>WHERE n.name = "新海诚"
>DETACH DELETE n
>```

## 3.a 使用Neo4jTemplate对图数据进行CRUD

#### 1.创建节点和关系

```java
// 创建节点实体

MovieEntity movie = new MovieEntity("你的名字","影片讲述了男女高中生在梦中相遇，并寻找彼此的故事。");// 电影实体节点

// 定义（参演）关系
// new Roles 参数1：Person实体，演员的出生年和姓名；参数2：演员名字列表（考虑到一个演员可能参演多个角色）
// 参数1是目标关系实体节点 参数2是关系属性
Roles roles1 = new Roles(new PersonEntity(1998,"上白石萌音"), Collections.singletonList("宫水三叶"));
Roles roles2 = new Roles(new PersonEntity(1993,"神木隆之介"), Collections.singletonList("立花泷"));
PersonEntity director = new PersonEntity(1973,"新海诚");

// 添加movie的演员实体，加入（参演）关系
movie.getActorsAndRoles().add(roles1);
movie.getActorsAndRoles().add(roles2);
movie.getDirectors().add(director);

// 存入图数据库持久化
neo4jTemplate.save(movie);
```

结果：

![在这里插入图片描述](https://img-blog.csdnimg.cn/0fad0888c59a4bc9926cf8d0f9cf4747.png)


#### 2.查询节点 

```java
        // 查询（不太推荐用Neo4jTemplate进行过滤查询，因为需要手动写cypherQuery,需要开发人员了解一下cypherQuery的写法）
        Optional<PersonEntity> person;
        // 1. 通过id查询
        person = neo4jTemplate.findById(12, PersonEntity.class);
        System.out.println("id为12号的Person节点：\n"+person);

        // 2. 通过属性查询节点，如name 需要手写cypherQuery语句
        Map<String,Object> map = new HashMap<>();
        map.put("name","新海诚");
        // 两种写法都对，看个人喜好 n是一个变量随意取，{}或者where填写query的filter过滤条件
        person = neo4jTemplate.findOne("MATCH (n:Person {name: $name}) RETURN n",map, PersonEntity.class);
//        person = neo4jTemplate.findOne("MATCH (n:Person) WHERE n.name = $name RETURN n",map, PersonEntity.class);
        System.out.println("\n查询名字为新海诚的Person节点:\n"+person);

        // 3. 通过属性关系查询节点
        map = new HashMap<>();
        map.put("roles",Collections.singletonList("宫水三叶"));
        // 方法1.使用toExecutableQuery查询
        QueryFragmentsAndParameters parameters = new QueryFragmentsAndParameters(
                "MATCH (person:Person) -[ relation:ACTED_IN]-> (movie:Movie) \n" +
                "WHERE relation.roles = $roles\n" +
                "RETURN person",map);
        List<PersonEntity> roles = neo4jTemplate.toExecutableQuery(PersonEntity.class, parameters).getResults();
        // 方法2.使用findOne查询
//        Optional<PersonEntity> roles = neo4jTemplate.findOne(
//                "MATCH (person:Person) -[ relation:ACTED_IN]-> (movie:Movie) \n" +
//                "WHERE relation.roles = $roles\n" +
//                "RETURN person",map,PersonEntity.class);
        
        System.out.println("\n查询角色为“宫水三叶”的演员：\n"+roles);
```

结果：

![在这里插入图片描述](https://img-blog.csdnimg.cn/a0040e87dac7493a9010db78310a712d.png)


>

#### 3.更新节点信息

```java
        Long userId = person.get().getId();// 记录当前查询的"新海诚"的节点id
        // 更新①---------更新“新海诚”的name为曾用名“新津诚”（这是他的曾用名）
        map.put("name","新海诚");
        map.put("usedName","新津诚");
        QueryFragmentsAndParameters queryFragmentsAndParameters =
                new QueryFragmentsAndParameters(
                        "MATCH (n:Person{name: $name}) SET n.name = $usedName",
                        map);
        neo4jTemplate.toExecutableQuery(
                PersonEntity.class,
                queryFragmentsAndParameters).getResults();
        Optional<PersonEntity> person1 = neo4jTemplate.findById(userId, PersonEntity.class);
        System.out.println("\n更新“新海诚”的name为曾用名“新津诚”（这是他的曾用名）:\n"+person1);
        // 更新②---------更新“新津诚”的name为“新海诚”
        person.get().setName("新海诚");
        neo4jTemplate.save(person.get());
        Optional<PersonEntity> person2 = neo4jTemplate.findById(userId, PersonEntity.class);
        System.out.println("\n更新“新津诚”的name为“新海诚”:\n"+person2);
```

结果：

![在这里插入图片描述](https://img-blog.csdnimg.cn/b5bd2091bdb9492eb0cdc1c799389803.png)


#### 4.删除节点

```java
        // 删除所有节点和关系（删除节点会响应删除关联关系）[也可以用cypherQuery执行，不再赘述]
        neo4jTemplate.deleteAll(MovieEntity.class);
        neo4jTemplate.deleteAll(PersonEntity.class);
```

![在这里插入图片描述](https://img-blog.csdnimg.cn/481d8a2da4dd4795a1a4599dc6f7007d.png)


上方的api根据实际情况使用，不再赘述。

#### 5.完整代码

```java
/**
     * 没有Repository情况下使用Neo4jTemplate操作数据
     * @param neo4jTemplate
     */
    @Test
    void TestNoRepository(@Autowired Neo4jTemplate neo4jTemplate){
        // 删除所有节点和关系（删除节点会响应删除关联关系），避免后续创建节点重复影响
        neo4jTemplate.deleteAll(MovieEntity.class);
        neo4jTemplate.deleteAll(PersonEntity.class); 
        // 创建节点实体
        MovieEntity movie = new MovieEntity("你的名字","影片讲述了男女高中生在梦中相遇，并寻找彼此的故事。");

        // new Roles 参数1：Person实体，演员的出生年和姓名；参数2：演员名字列表（考虑到一个演员可能参演多个角色）
        // 参数1是目标关系实体节点 参数2是关系属性
        Roles roles1 = new Roles(new PersonEntity(1998,"上白石萌音"), Collections.singletonList("宫水三叶"));
        Roles roles2 = new Roles(new PersonEntity(1993,"神木隆之介"), Collections.singletonList("立花泷"));
        PersonEntity director = new PersonEntity(1973,"新海诚");
        // 添加movie的演员实体，加入（参演）关系
        movie.getActorsAndRoles().add(roles1);
        movie.getActorsAndRoles().add(roles2);
        movie.getDirectors().add(director);

        // 存入图数据库持久化
        neo4jTemplate.save(movie);

        // 查询（不太推荐用Neo4jTemplate进行过滤查询，因为需要手动写cypherQuery,需要开发人员了解一下cypherQuery的写法）
        Optional<PersonEntity> person;
        // 1. 通过id查询
        person = neo4jTemplate.findById(12, PersonEntity.class);
        System.out.println("id为12号的Person节点：\n"+person);

        // 2. 通过属性查询节点，如name 需要手写cypherQuery语句
        Map<String,Object> map = new HashMap<>();
        map.put("name","新海诚");
        // 两种写法都对，看个人喜好 n是一个变量随意取，{}或者where填写query的filter过滤条件
        person = neo4jTemplate.findOne("MATCH (n:Person {name: $name}) RETURN n",map, PersonEntity.class);
//        person = neo4jTemplate.findOne("MATCH (n:Person) WHERE n.name = $name RETURN n",map, PersonEntity.class);
        System.out.println("\n查询名字为新海诚的Person节点:\n"+person);

        // 3. 通过属性关系查询节点
        map = new HashMap<>();
        map.put("roles",Collections.singletonList("宫水三叶"));
        // 方法1.使用toExecutableQuery查询
        QueryFragmentsAndParameters parameters = new QueryFragmentsAndParameters(
                "MATCH (person:Person) -[ relation:ACTED_IN]-> (movie:Movie) \n" +
                "WHERE relation.roles = $roles\n" +
                "RETURN person",map);
        List<PersonEntity> roles = neo4jTemplate.toExecutableQuery(PersonEntity.class, parameters).getResults();
        // 方法2.使用findOne查询
//        Optional<PersonEntity> roles = neo4jTemplate.findOne(
//                "MATCH (person:Person) -[ relation:ACTED_IN]-> (movie:Movie) \n" +
//                "WHERE relation.roles = $roles\n" +
//                "RETURN person",map,PersonEntity.class);
        System.out.println("\n查询角色为“宫水三叶”的演员：\n"+roles);

        Long userId = person.get().getId();// 记录当前查询的"新海诚"的节点id
        // 更新①---------更新“新海诚”的name为曾用名“新津诚”（这是他的曾用名）
        map.put("name","新海诚");
        map.put("usedName","新津诚");
        QueryFragmentsAndParameters queryFragmentsAndParameters =
                new QueryFragmentsAndParameters(
                        "MATCH (n:Person{name: $name}) SET n.name = $usedName",
                        map);
        neo4jTemplate.toExecutableQuery(
                PersonEntity.class,
                queryFragmentsAndParameters).getResults();
        Optional<PersonEntity> person1 = neo4jTemplate.findById(userId, PersonEntity.class);
        System.out.println("\n更新“新海诚”的name为曾用名“新津诚”（这是他的曾用名）:\n"+person1);
        // 更新②---------更新“新津诚”的name为“新海诚”
        person.get().setName("新海诚");
        neo4jTemplate.save(person.get());
        Optional<PersonEntity> person2 = neo4jTemplate.findById(userId, PersonEntity.class);
        System.out.println("\n更新“新津诚”的name为“新海诚”:\n"+person2);

    }
```

## 3.b 使用repository对图数据进行CRUD

### 1.创建Repository

新建`repository`包，创建`PersonRepository`和`MovieRepository`

```java
package com.example.neo4jdemo.repository;

import com.example.neo4jdemo.entity.PersonEntity;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PersonRepository extends Neo4jRepository<PersonEntity, Long> {
}
```

```java
package com.example.neo4jdemo.repository;

import com.example.neo4jdemo.entity.MovieEntity;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovieRepository extends Neo4jRepository<MovieEntity, Long> {
}
```

### 2.创建节点和关系

```java
// 创建节点
MovieEntity movie = new MovieEntity("你的名字","影片讲述了男女高中生在梦中相遇，并寻找彼此的故事。");
Roles roles1 = new Roles(new PersonEntity(1998,"上白石萌音"), Collections.singletonList("宫水三叶"));
Roles roles2 = new Roles(new PersonEntity(1993,"神木隆之介"), Collections.singletonList("立花泷"));
PersonEntity director = new PersonEntity(1973,"新海诚");
// 添加关系
movie.getActorsAndRoles().add(roles1);
movie.getActorsAndRoles().add(roles2);
movie.getDirectors().add(director);
// 存入图数据库持久化
movieRepository.save(movie);
```

### 3.查询

需求：根据Person的名字查询对应节点

在`PersonRepository`中添加方法：

```java
@Repository
public interface PersonRepository extends Neo4jRepository<PersonEntity, Long> {
    PersonEntity findPersonEntityByName(String name);
}
```

在`MovieRepository`中添加方法：

```java
@Repository
public interface MovieRepository extends Neo4jRepository<MovieEntity, Long> {

//    @Query("MATCH (n:Movie) WHERE id(n) = $0 RETURN n") 这种方法是自己写Query语句进行查询
    List<MovieEntity> findMovieEntitiesById(Long id);
    MovieEntity findMovieEntityByTitle(String title);
}
```

查询：

```java
// 查询
        // 查询
        PersonEntity person = personRepository.findPersonEntityByName("上白石萌音");
        System.out.println("查询名字为“上白石萌音”的PersonEntity："+person);
        MovieEntity movieQueried = movieRepository.findMovieEntityByTitle("你的名字");
        System.out.println("查询名字为“你的名字”的MovieEntity："+movieQueried);
```

结果：

![在这里插入图片描述](https://img-blog.csdnimg.cn/cd5b960021364a95a49f3fd74ecf93c9.png)


```bash
查询名字为“上白石萌音”的PersonEntity：
PersonEntity(id=15, name=上白石萌音, born=1998)

查询名字为“你的名字”的MovieEntity：
MovieEntity(id=14, title=你的名字, description=影片讲述了男女高中生在梦中相遇，并寻找彼此的故事。, actorsAndRoles=[com.example.neo4jdemo.entity.Roles@d902300, com.example.neo4jdemo.entity.Roles@2db33feb], directors=[PersonEntity(id=17, name=新海诚, born=1973)])
```

### 4.更新

```java
        // 更新(更新主要是三步：1.获取实体id；2.修改实体属性；3.更新实体）
        // 注意：repository的save方法【对应的实体若id一致】则为修改，否则为新建。
        Long personId = person.getId();
        person.setBorn(1997);
        personRepository.save(person);
        person = personRepository.findPersonEntityByName("上白石萌音");
        System.out.println(personId == person.getId()?"\n更新“上白石萌音”出生日期为1997信息成功！：\n"+person:"更新信息失败！");
```

![在这里插入图片描述](https://img-blog.csdnimg.cn/42ef26fb571d47a8a5d07f7ee3354aeb.png)


### 5.删除

```java
        // 删除所有节点和关系
        movieRepository.deleteAll();
        personRepository.deleteAll();
```

### 6.完整代码

```java
/**
 * 使用repository操作图数据
 */
@Test
void testByRepository(@Autowired MovieRepository movieRepository, @Autowired PersonRepository personRepository){
    // 删除所有节点和关系（删除节点会响应删除关联关系），避免后续创建节点重复影响
    movieRepository.deleteAll();
    personRepository.deleteAll();

    // 创建节点
    MovieEntity movie = new MovieEntity("你的名字","影片讲述了男女高中生在梦中相遇，并寻找彼此的故事。");
    Roles roles1 = new Roles(new PersonEntity(1998,"上白石萌音"), Collections.singletonList("宫水三叶"));
    Roles roles2 = new Roles(new PersonEntity(1993,"神木隆之介"), Collections.singletonList("立花泷"));
    PersonEntity director = new PersonEntity(1973,"新海诚");
    // 添加关系
    movie.getActorsAndRoles().add(roles1);
    movie.getActorsAndRoles().add(roles2);
    movie.getDirectors().add(director);
    // 存入图数据库持久化
    movieRepository.save(movie);

    // 查询
    PersonEntity person = personRepository.findPersonEntityByName("上白石萌音");
    System.out.println("\n查询名字为“上白石萌音”的PersonEntity：\n"+person);
    MovieEntity movieQueried = movieRepository.findMovieEntityByTitle("你的名字");
    System.out.println("\n查询名字为“你的名字”的MovieEntity：\n"+movieQueried);

    // 更新(更新主要是三步：1.获取实体id；2.修改实体属性；3.更新实体）
    // 注意：repository的save方法【对应的实体若id一致】则为修改，否则为新建。
    Long personId = person.getId();
    person.setBorn(1997);
    personRepository.save(person);
    person = personRepository.findPersonEntityByName("上白石萌音");
    System.out.println(personId == person.getId()?"\n更新“上白石萌音”出生日期为1997信息成功！：\n"+person:"更新信息失败！");
}
```

## 4.参考与源码

> 案例来自官方文档：
>
> ```
> https://docs.spring.io/spring-data/neo4j/docs/current/reference/html/#conversions
> https://neo4j.com/docs/aura/auradb/connecting-applications/spring-boot/
> ```
>
> 源码戳这里：
>
> https://github.com/WuYiheng-Og/neo4j_springboot
