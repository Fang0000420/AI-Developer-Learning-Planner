# Backend

Spring Boot backend service for AI Developer Learning Planner.

- Planned port: `8080`
- Responsibility: users, goals, plans, daily tasks, progress records, and REST APIs
- Build tool: Maven
- Java version: 17
- Spring Boot version: 3.4.2
- Status: Day 02 task 1 Spring Boot project skeleton created

## Included Dependencies

- Spring Web
- Spring Data JPA
- Validation
- PostgreSQL Driver
- Flyway
- Lombok
- Springdoc OpenAPI
- JUnit 5 / Spring Boot Test

## Local Build

Use JDK 17 for backend commands.

```bash
mvn test
```

`mvn test` is the task 1 verification command for compiling the project and running the lightweight test skeleton.
`mvn spring-boot:run` will be used after Day 02 task 2 adds datasource, JPA, Flyway, port, and health configuration.
