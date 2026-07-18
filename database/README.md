# Database schema

The versioned source of truth for the PostgreSQL schema is:

`backend/src/main/resources/db/migration/V1__initial_schema.sql`

Flyway applies this migration when the Spring Boot application starts. Docker Compose mounts the same migration into a newly created PostgreSQL container, so local Docker environments start with the identical schema.