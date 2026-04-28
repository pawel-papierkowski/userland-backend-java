# AGENTS.md

This is a modern **Java 25**, **Spring Boot 4.0.5** application. The backend serves a **REST API** and handles data persistence via ***PostgreSQL***.

## 🛠️ Build & Run Commands
- Compile the project: `mvn clean compile`
- Run all tests: `mvn clean test`
- Run a single test class: `mvn test -Dtest=ClassNameTest`
- Generate JaCoCo coverage report: `mvn clean test jacoco:report`
- Start the application locally: `mvn spring-boot:run`

*Note: You do not need to start a local database manually. The project uses Testcontainers and Docker Compose support for local development and testing.*

## 📐 Code Style & Conventions
- **Java Version:** Use modern Java 25 features (Records, Switch Expressions, Pattern Matching) wherever possible.
- **Lombok:** Always use Lombok to reduce boilerplate.
    - Use `@Data` or `@Value` for DTOs.
    - Use `@RequiredArgsConstructor` for dependency injection in `@Service` and `@RestController` classes (do not use `@Autowired` on fields).
    - Never write manual getters or setters.
    - Never write manual `equals()`/`hashCode()` methods, except Hibernate entities that require custom code there.
- **REST APIs:**
    - Return `ResponseEntity<T>` from all controller endpoints.
      - In case endpoint do not return anything, use `ResponseEntity<Void>` and return `204 No Content`.
    - All request classes have `Req` suffix. All response classes have `Resp` suffix.
    - Document all endpoints using Swagger/OpenAPI `@Operation`, `@Schema`, and custom meta-annotations like `@ApiAuthResponses`. `ProblemDetail` and derived classes are used for errors.
- **Validation:** Use `jakarta.validation` annotations (like `@NotBlank`, `@Email`) on incoming request records.

## 🧪 Testing Guidelines
- Use **JUnit 5** (Jupiter) and **AssertJ** for all assertions (`assertThat(...)`).
- Do not use Mockito `verify()` unless absolutely necessary; prefer testing actual state changes or return values.
- All integration tests must extend the `BaseIntegrationTest` abstract class.

## 🏗️ Architecture & Project Structure

- `common/` - Shared utilities, constants, and custom annotations (like `@NoCoverageGenerated`).
- `config/` - Classes annotated with `@Configuration` used to configure various aspects of Spring or application.
- `features/` - Logically separated pieces of business logic.
  - Each piece is in separate package and in turn has subpackages described below. Note not all subpackages must be present, if given piece do not require it.
    - `controllers/` - REST endpoints only. No business logic. Calls services.
    - `dto/` - Data Transfer Objects (must be Java `record` types).
    - `entities/` - Contains ORM entities.
    - `exceptions/` - Contains custom exceptions.
    - `mappers/` - Contains mappers for Data Transfer Objects that require it.
    - `repositories/` - Spring Data JPA interfaces.
    - `services/` - Core business logic. Classes here must be annotated with `@Service` and inherit from `BaseService`.
- `swagger/` - Classes used for Swagger/OpenAPI documentation.
- `system/` - System and supporting code. Unlike features above, these are used in any kind of project. 
  - `auth/` - Handles authorization and permissions.
  - `config/` - Handles system configuration feature.
  - `history/` - Handles system history feature.
  - `jwt/` - Handles JWT-related code.
  - `lockdown/` - Handles system lockdown feature.

## 🛑 What NOT to do
- Do not use generic `Exception` or `RuntimeException`. Always throw domain-specific exceptions that extend our `GeneralException`.
- Do not remove the `@Generated` or `@NoCoverageGenerated` annotations from exception classes or DTOs.
