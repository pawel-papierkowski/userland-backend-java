# AI Agent Instructions for UserLand Backend

This file contains rules and context for AI coding agents working on the UserLand backend project. Always review these rules before writing code, modifying architecture, or running tests.

This is a modern **Java 25**, **Spring Boot 4.0.5** application. The backend serves a **REST API** and handles data persistence via ***PostgreSQL***.

## 🛠️ Tech Stack
- **Language:** Java 25 (Temurin)
- **Framework:** Spring Boot 4.0.5
- **Build Tool:** Maven
- **Database:** PostgreSQL via Spring Data JPA
- **Templating:** Thymeleaf (specifically for emails)

## ⌨️ Build & Run Commands
- Compile the project: `mvn clean compile`
- Run all tests: `mvn clean test`
- Run a single test class: `mvn test -Dtest=ClassNameTest`
- Generate JaCoCo coverage report: `mvn clean test jacoco:report`
- Start the application locally: `mvn spring-boot:run`

*Note: You do not need to start a local database manually. The project uses Testcontainers and Docker Compose support for local development and testing.*

## 🏗️ Project Structure
- `common/` - Shared utilities, constants, and custom annotations (like `@NoCoverageGenerated`).
- `config/` - Classes annotated with `@Configuration` used to configure various aspects of Spring or application.
- `features/` - Logically separated pieces of business logic (domain-driven package structure).
  - Each domain is in separate package and in turn has subpackages described below. Note not all subpackages must be present, if given domain do not require it.
    - `controllers/` - REST endpoints only. No business logic. Calls services.
    - `dto/` - Data Transfer Objects (must be Java `record` types).
    - `entities/` - Contains ORM entities.
    - `exceptions/` - Contains custom exceptions.
    - `mappers/` - Contains mappers for Data Transfer Objects that require it.
    - `repositories/` - Spring Data JPA interfaces.
    - `schedulers/` - Any schedulers used in this domain.
    - `services/` - Core business logic. Classes here must be annotated with `@Service`. Services that need it may inherit directly or indirectly from `BaseService`.
- `swagger/` - Classes used for Swagger/OpenAPI documentation.
- `system/` - System and supporting code. Unlike features above, these are used in any kind of project.
  - `auth/` - Handles authorization and permissions.
  - `config/` - Handles system configuration feature.
  - `history/` - Handles system history feature.
  - `jwt/` - Handles JWT-related code.
  - `lockdown/` - Handles system lockdown feature.

## 📐 Code Style & Conventions
- **Java Version:** Use modern Java 25 features (Records, Switch Expressions, Pattern Matching) wherever possible.
- **Dependency Injection:** Always use Constructor Injection. Never use `@Autowired` on fields (except in Test classes).
- **Lombok:** Always use Lombok to reduce boilerplate.
    - Use `@Data` or `@Value` for DTOs.
    - Use `@RequiredArgsConstructor` for dependency injection in `@Service` and `@RestController` classes (do not use `@Autowired` on fields).
    - Never write manual getters or setters.
    - Never write manual `equals()`/`hashCode()` methods, except Hibernate entities that require custom code.
- **REST APIs:**
    - Return `ResponseEntity<T>` from all controller endpoints.
      - In case endpoint do not return anything, use `ResponseEntity<Void>` and return `204 No Content`.
    - All request classes have `Req` suffix. Endpoints with requests have `@Valid` annotation.
    - All response classes have `Resp` suffix.
    - Document all endpoints using Swagger/OpenAPI `@Operation`, `@Schema`, and custom meta-annotations like `@ApiAuthResponses`. `ProblemDetail` and derived classes are used for errors.
- **Entities:**
  - Do not use `@Data` for Hibernate entities. Use `@Getter` and `@Setter`.
  - Map tables using `@Table(name = "...", schema = "...")`.
  - Generate manual `equals()`/`hashCode()` methods using **business key** (single field). If given entity do not have any field that can be used as business key, create UUID field that will be used as business key.
- **Validation:** Use `jakarta.validation` annotations (like `@NotBlank`, `@Email`) on entity fields and DTOs.
- **Comments:** Code is thoroughly commented.
  - All classes must have comment describing what this class is for.
  - All public methods must have comment describing what this method is for.
  - Protected and private classes can have comments.

## 🌐 Internationalization (I18n)
- We use a custom `I18nConfig` with a custom `YamlPropertiesPersister` to load `.yaml` files as message sources.
- Do not use standard `.properties` files for translations.
- Thymeleaf templates are located in `src/main/resources/templates/` and its subdirectories.
- Translation files are located in `src/main/resources/i18n/` and its subdirectories.
- Ensure `TemplateEngine` uses the `MessageSource` for evaluating `#{...}` tags.

## 🧪 Testing Guidelines
- Use **JUnit 5** (`@Test`) and **AssertJ** (`assertThat`) for all assertions.
- Do not use Mockito `verify()` unless absolutely necessary; prefer testing actual state changes or return values.
- Package structure: 
  - `test/` subpackage contains all classes that support testing:
    - `base/` - All base test classes. Test classes inherit appropriate base test class.
    - `config/` - Configuration classes that are specific to tests only.
    - `helpers/`:
      - `asserts/` - Assert complex entities that cannot be simply asserted recursively all at once for technical reasons.
      - `context/` - Context-related code that assists with tests.
      - `factories/` - Convenient classes that create more or less random entities for you (either hardcoded data or via Instancio).
      - `mocks/` - Auxiliary classes that help with mocking.
      - `problemDetail/` - Handles problem details in testing.
- Do not use context slicing (like `@DataJpaTest` or `@WebMvcTest`) unless explicitly asked; prefer `BaseIntegrationTest` to ensure configurations load correctly.

## 🚀 Deployment & CI/CD
- **Target:** Google Cloud Run (Serverless).
- **Memory Limit:** 512Mi (Severely constrained).
- **Build Method:** Paketo Buildpacks via `mvnw spring-boot:build-image`.
- **Rule:** Do not add heavy, long-running background polling dependencies (like Quartz, Kafka, or heavy schedulers) because Cloud Run throttles CPU to zero when not processing HTTP requests.

## 🛑 What NOT to do
- Do not use generic `Exception` or `RuntimeException`. Always throw domain-specific exceptions that extend our `GeneralException`.
- Do not remove the `@Generated` or `@NoCoverageGenerated` annotations from exception classes or DTOs.
- Do not modify `.github/workflows/deploy.yml` without explicit permission.
- 