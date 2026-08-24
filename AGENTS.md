# AI Agent Instructions for UserLand Backend

This file contains rules and context for AI coding agents working on the UserLand backend project. Always review these rules before writing code, modifying architecture, or running tests.

**UserLand** is a portfolio project for backend in Java. It is part of bigger project that contains frontend and backend.

This is a modern **Java 25**, **Spring Boot 4.1.0** application. The backend serves a **REST API** and handles data persistence via ***PostgreSQL***.

## General

### 🛠️ Tech Stack
- **Language:** Java 25 (Temurin)
- **Framework:** Spring Boot 4.1.0
- **Build Tool:** Maven
- **Database:** PostgreSQL 17 via Spring Data JPA
- **Templating:** Thymeleaf (specifically for emails)

## 💻 Terminal commands
- **Maven:** Use Maven wrapper `.\mvnw`.
  - Compile the project: `.\mvnw clean compile`
  - Run single test: `.\mvnw test "-Dtest=ClassNameTest#nameOfTestFunction"`
  - Run all tests from single file: `.\mvnw test "-Dtest=ClassNameTest"`
  - Run all tests for entire project: `.\mvnw clean test` (only at end of work/task to make sure nothing broke)
  - Generate JaCoCo coverage report: `.\mvnw clean test jacoco:report`
  - Start the application locally: `.\mvnw spring-boot:run`

*Note: You do not need to start a local database manually. The project uses Testcontainers and Docker Compose support for local development and testing.*

### 🚀 Deployment & CI/CD
- **Target:** Google Cloud Run (Serverless).
- **Memory Limit:** 512 Mi (Severely constrained).
- **Build Method:** Paketo Buildpacks via `mvnw spring-boot:build-image`.
- **Rule:** Do not add heavy, long-running background polling dependencies (like Quartz, Kafka, or heavy schedulers) because Cloud Run throttles CPU to zero when not processing HTTP requests.

### 🏗️ Project Structure
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
- `gcp/` - GCP-related code.
- `swagger/` - Classes used for Swagger/OpenAPI documentation.
- `system/` - System and supporting code. Unlike features above, these are used in any kind of project.
  - `auth/` - Handles authorization, JWT-related code and permissions.
  - `config/` - Handles system configuration feature.
  - `history/` - Handles system history feature.
  - `lockdown/` - Handles system lockdown feature.
- `utils/` - Utility classes: static helper classes and similar.

### ⚙️ Features
- `check` - Debug endpoints to get health and status of system. Independent of Actuator.
- `email` - Email handling services.
- `user` - User handling services.

## Code

### 📐 Code Style & Conventions
- **Java Version:** Use modern Java 25 features (Records, Switch Expressions, Pattern Matching) wherever possible.
- **Dependency Injection:**
  - Use Constructor Injection via `@RequiredArgsConstructor`.
  - Never use `@Autowired` on fields. Exceptions: `Test` classes and abstract classes.
- **Lombok:** Always use Lombok to reduce boilerplate.
    - Use `@Data` or `@Value` for DTOs.
    - Use `@RequiredArgsConstructor` for dependency injection in `@Service` and `@RestController` classes (do not use `@Autowired` on fields).
    - Never write manual getters or setters.
    - Never write manual `equals()`/`hashCode()` methods, except Hibernate entities that require custom code for these methods.
- **REST APIs:**
    - Return `ResponseEntity<T>` from all controller endpoints.
      - In case endpoint do not return anything, use `ResponseEntity<Void>` and return `204 No Content`.
    - All request classes have `Req` suffix. Endpoints with requests have `@Valid` annotation.
    - All response classes have `Resp` suffix.
    - Document all endpoints using Swagger/OpenAPI `@Operation`, `@Schema`, and custom meta-annotations like `@ApiAuthResponses`. `ProblemDetail` and derived classes are used for errors.
- **Database:**
  - We run on PostgreSQL via Hibernate.
  - We use Flyway. See `src/main/resources/db/migration/` files for structure of database.
- **Entities:**
  - Entity definition and annotations must be consistent with database structure.
  - When adding new fields, be careful with Instancio. In some cases you might want to ignore fields (especially if filled automatically by Hibernate).
  - Do not use Lombok's `@Data` for Hibernate entities. Use `@Getter` and `@Setter`.
  - Map tables using `@Table(name = "...", schema = "...")`.
  - Association annotations like `@ManyToOne` must always specify fetch type explicitly.
  - Generate manual `equals()`/`hashCode()` methods using **business key** (single field). If given entity do not have any field that can be used as business key, create dedicated UUID field that will be used as business key. Any exception to this rule must be documented.
- **Validation:** Use `jakarta.validation` annotations (like `@NotBlank`, `@Email`) on entity fields and DTOs.
- **Comments:** Code is thoroughly commented.
  - All classes must have comment describing what this class is for.
  - All public methods must have comment describing what this method is for. You can omit this requirement for `@Override`d methods.
  - Protected and private classes can have comments.

### 💼 Business Rules
- `User` and `UserProfile` have 1:1 relationship and profile always exist if user exist.

### 🌐 Internationalization (I18n)
- We use a custom `I18nConfig` with a custom `YamlPropertiesPersister` to load `.yaml` files as message sources.
- Do not use standard `.properties` files for translations.
- Thymeleaf templates are located in `src/main/resources/templates/` and its subdirectories.
- Translation files are located in `src/main/resources/i18n/` and its subdirectories.
- Ensure `TemplateEngine` uses the `MessageSource` for evaluating `#{...}` tags.

### 🧪 Testing Guidelines
- Tests use singleton Postgres container. Ensure reset of database state before each test that uses database.
- Use **JUnit 5** (`@Test`) and **AssertJ** (`assertThat`) for all assertions.
- Do not use Mockito `verify()` unless absolutely necessary; prefer testing actual state changes or return values.
- Package structure: 
  - `test/` subpackage contains all classes that support testing:
    - `base/` - All main base test classes. Inherited intermediate classes (still called `Base`, but specific to given feature) live in relevant features.
      Actual test classes inherit appropriate intermediate test class.
    - `config/` - Configuration classes that are specific to tests only.
    - `helpers/`:
      - `asserts/` - Assert complex entities that cannot be simply asserted recursively all at once for technical reasons.
      - `context/` - Context-related code that assists with tests.
      - `factories/` - Convenient classes that create more or less random entities for you (either hardcoded data or via Instancio).
      - `mocks/` - Auxiliary classes that help with mocking.
      - `problemDetail/` - Handles problem details in testing.
- Do not use context slicing (like `@DataJpaTest` or `@WebMvcTest`) unless explicitly asked; prefer `BaseIntegrationTest` to ensure configurations load correctly.

## 🔎 Reviewing code

When I ask for review, in order of importance:
- Analyze general purpose and functionality.
- Check code for bugs, mistakes and other potential issues. If there are a lot of stuff here, skip rest of steps: we need to fix that stuff first.
- Verify algorithm and logic. Is this correct way to do it? Can it be done better?
- Make sure common programming principles (like DRY) are followed.
- Find tests for reviewed code and review them too. If tests are missing, note their absence and plan what tests should be added. Do not add them automatically unless explicitly asked.
- I might ask to review same code multiple times (to re-check code after changes implemented from previous review). Re-read files as necessary.
  - You can skip some steps if appropriate (for example, skip purpose/functionality analysis if purpose and functionality is already known).
  - If previously reported issues still exist, inform about them again unless they were explained or rejected.

## 🛑 What NOT to do
- Do not use generic `Exception` or `RuntimeException`. Always throw domain-specific exceptions that extend our `GeneralException`.
- Do not remove or add the `@Generated` or `@NoCoverageGenerated` annotations from exception classes or DTOs.
- Do not modify `.github/workflows/deploy.yml` without explicit permission.