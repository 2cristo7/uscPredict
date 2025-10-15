# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**uscPredict** is a prediction market platform for USC students built with Spring Boot 3.5.6, Java 21, and PostgreSQL. Users can create and participate in prediction events about future outcomes.

## Build System

This project uses **Gradle** with the Gradle Wrapper.

### Essential Commands

```bash
# Build the project
./gradlew build

# Run the application
./gradlew bootRun

# Run tests
./gradlew test

# Clean build artifacts
./gradlew clean

# Build without running tests
./gradlew build -x test
```

**Important**: This project requires **Java 17 or newer** (configured for Java 21). Ensure the correct JDK is active before running Gradle commands.

## Database Configuration

The application connects to a PostgreSQL database hosted on Supabase. Configuration is in `src/main/resources/application.properties`:

- Active profile: `prod`
- JPA/Hibernate: `ddl-auto=update` (auto-updates schema on startup)
- Connection pooling via Supabase pooler

## Architecture

### Layer Structure

The codebase follows a standard Spring Boot layered architecture:

1. **Controller Layer** (`controller/`) - REST API endpoints
2. **Service Layer** (`service/`) - Business logic
3. **Repository Layer** (`repository/`) - Data access via Spring Data JPA
4. **Model Layer** (`model/`) - JPA entities

### Domain Models

#### User (`model/User.java`)
- Primary key: `UUID uuid`
- Fields: `name`, `email`, `pswd_hash`, `role` (enum), `created_at`
- Email is unique and validated
- Uses Lombok `@Setter` selectively (not full `@Data`)

#### Event (`model/Event.java`)
- Primary key: `UUID id`
- Represents prediction markets with YES/NO outcomes
- Fields: `title`, `description`, `startDate`, `endDate`, `state`, `createdBy`, `yesPrice`, `noPrice`
- State enum: `ACTIVE`, `CLOSED`, `RESOLVED` (defined in `EventState.java`)
- Order books: `yesOrderBook` and `noOrderBook` (List<UUID> stored in separate join tables)
- Relationship: `@ManyToOne` with User via `createdBy`

#### Comment (`model/Comment.java`)
- Primary key: `UUID uuid`
- Fields: `content`, `userId`, `postId`, `createdAt`
- Currently uses UUID references rather than JPA relationships

### Repository Pattern

Repositories extend `CrudRepository` and return `Set<T>` instead of `List<T>`:

```java
public interface UserRepository extends CrudRepository<User, String> {
    Set<User> findAll();
    Set<User> findByName(String name);
}
```

### Security Configuration

Located in `config/SecurityConfig.java`:
- **CSRF is disabled** (for easy API testing)
- **All endpoints permit anonymous access** (no authentication required currently)
- Spring Security is included but not actively enforcing authentication

## Development Notes

### Lombok Usage
The project uses Lombok but **not consistently**:
- Some entities use `@Getter @Setter @NoArgsConstructor` (e.g., Event)
- Others use explicit getters with selective `@Setter` (e.g., User)
- Follow the existing pattern in each file when modifying

### Validation
- Uses Jakarta Bean Validation (`@NotBlank`, `@NotNull`, `@Email`, etc.)
- Controllers accept `@RequestBody` with validation annotations

### Exception Handling
Custom exceptions are in `exception/` package:
- `PredictUsernameNotFoundException` - thrown when user lookup fails

### Comments in Spanish
Some comments and error messages are in Spanish (e.g., "El contenido no puede estar vacío"). Maintain consistency with the existing language in each file.

## API Structure

Controllers are mapped under their plural entity names:
- `/users` - UserController
- `/comments` - CommentController (expected)
- Events controller not yet implemented

Health check endpoint: `GET /users/health`

## Testing

- Test classes are in `src/test/java/usc/uscPredict/`
- Uses JUnit 5 (Jupiter) with Spring Boot Test
- Run with: `./gradlew test`

## Current State

Based on git status:
- **Branch**: `feature/events-logic`
- **Recent work**: Event and EventState models added (staged but uncommitted)
- **Latest commit**: v0.1.1 - User POST endpoint fixed

The Event model is defined but lacks corresponding controller, service, and repository implementations.
