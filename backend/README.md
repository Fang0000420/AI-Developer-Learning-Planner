# Backend

Spring Boot backend service for AI Developer Learning Planner.

- Planned port: `8080`
- Responsibility: users, goals, plans, daily tasks, progress records, and REST APIs
- Build tool: Maven
- Java version: 17
- Spring Boot version: 3.4.2
- Status: Day 02 task 3 initial database tables created

## Included Dependencies

- Spring Web
- Spring Data JPA
- Validation
- PostgreSQL Driver
- Flyway
- Lombok
- Springdoc OpenAPI
- JUnit 5 / Spring Boot Test

## Build

Use JDK 17 for backend commands.

```bash
mvn test
```

## Server Development Run

Use any PostgreSQL instance that matches `../.env.example`. If Docker Hub access is available, PostgreSQL can be started from the repository root:

```bash
docker compose -f infra/docker-compose.yml up -d postgres
```

Then start the backend:

```bash
cd backend
mvn spring-boot:run
```

Health check:

```bash
curl http://localhost:8080/api/health
```

Expected response:

```json
{"status":"UP","service":"ai-developer-learning-planner-backend"}
```

Datasource, JPA, Flyway, and port settings are configured in `src/main/resources/application.yml` and can be overridden with the environment variables from `../.env.example`.

## Initial Schema

Flyway migration `src/main/resources/db/migration/V1__create_users_and_goals.sql` creates:

- `users`
- `goals`

JPA mappings and repositories live in:

- `com.aidevplanner.backend.user`
- `com.aidevplanner.backend.goal`
