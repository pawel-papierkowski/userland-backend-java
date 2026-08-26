# USERLAND-BACKEND-JAVA

This project is **backend** part of **UserLand system**, designed to work with frontend project `userland-frontend-vue`.

It demonstrates basics of modern Java and Spring Boot project focused on pure user management. It was made from scratch using Spring Initializr.

Project is finished. Please read entire file before doing anything. You will need to configure some stuff before you can deploy project.

## Basic info

Author: **Paweł Papierkowski**

Date: **2026**

Used IDE: **IntelliJ IDEA**

- Public backend address (GCP): https://userland-backend-java-299988087135.europe-central2.run.app
- Locally run server: http://localhost:8080/
- Link to source code: https://github.com/pawel-papierkowski/userland-backend-java

### Deployment

This app uses (free tier for all of these):
- **Google Cloud**:
  - **Run** for backend hosting.
  - **Tasks** for email send requests hosting. Note: in local development emails are sent as is without using Cloud Tasks.
- **Aiven** for PostgreSQL database hosting.
- **Resend** (or `JavaMailSender`) for email services.

**UserLand** app is deployed via **GitHub Actions**.

UserLand uses *support@pawelpapierkowski.net.pl* address as sender in emails sent by system.

### Functionality

This project fully manages user. This is where name **UserLand** comes from.

You can think of it as baseline for other projects, as almost any project, system or application will need user accounts.

### Features

- Standard user:
  - User **registration**.
  - User **activation** via email.
  - User **login**.
  - User **logout**.
  - **Editing** of your own user account.
  - Safe **email change** (sensitive operation).
  - **Password reset** via email.
  - User **account deletion** via email.
- Handling **user permissions** (standard user vs panel admin operator).
- **Admin user**:
  - Viewing **table of users**, including pagination and filtration.
  - Viewing data of selected user (both base user and profile).
    - Editing data of user (requires separate permission). 
    - Viewing user **config**, **tokens**, **JWTs**, **history** and **permissions**.
    - Only admin or operator with appropriate permissions can edit **config**/**permissions** records.
    - User **lock/unlock**.
- Other options: 
  - **System lockdown**: if turned on, only users with admin rights can call any endpoint.
  - **System history** for global events like lockdown. 
- Other features:
  - **Debug endpoints** `/api/checks/*` that allow testing various scenarios (access without/with authentication, error handling etc.) for frontend development. 
  - Sending **emails** (JavaMailSender or Resend).

### Design notes

- All date/time fields are **without timezone**. Frontend should convert it properly to show date/time on screen in local timezone.
- **Kafka** was considered for demonstration purposes (email retries), but not used since it won't work well with restrictions typical of Google Cloud free tier, where this project lives. GCP is serverless, but Kafka would require system to be up at all times. So I decided on **GCP Cloud Tasks**.
- While this portfolio project exists as single-instance on throttled GCP (or locally), it is written with multiple instances in mind:
  - We use `ShedLock` to ensure no issues with many identical shedulers running at once.
  - Cache life is short, so cache eviction on instance A will not cause stale results on instance B for too long.

### Security

- Features instant revocation of JWT (so also permission enforcement), instant enforcement of locked user.
  Only lockdown enforcement can be slightly delayed (system config table in DB is cached with 20s TTL).
- System uses JWT for all API requests that require security (for example, some endpoints require admin panel access permissions).
- Every user has `iam.user_permissions` and `iam.jwt` subtables.
  - System generates JWT with embedded permissions on login/prolong. JWT is persisted in database.
  - Backend verifies JWT in request against JWT in database, so JWT revocation with immediate effect is possible.
  - Permission changes via admin panel revoke all JWTs for given user, forcing them to re-log.

## GitHub config

System uses variables and secrets defined in GitHub.

Some of these have default values in YAML configuration, but you must override everything, especially keys, secrets and passwords.
Default values are strictly for local development and even then sensitive values like keys and passwords should be in run configuration in your IDE.
YAML configuration contains only placeholder values.

### Repository Variables
These values are visible and freely editable in GitHub panel.

- General configuration:
  - **BUILD**: Informs about build of system. Allowed values: `PROD`, `STAGE`, `DEV`, `TEST` (corresponds to values in `EnAppBuild`).
- Google Cloud:
  - **GCP_URL**: Address of system backend hosted on GCP Cloud Run.
  - **GCP_SERVICE_ACC**: Name of service account for GCP project.
  - **GCP_GAR_LOCATION**: Data center region.
  - **GCP_REPOSITORY**: GCP Artifact Registry Repository.
  - **GCP_DASHBOARD_NAME**: Name that backend will have in the Google Cloud Run dashboard.
  - **GCP_TASKS_QUEUE_EMAIL**: GCP Cloud Tasks queue name for queued emails.
- Email:
  - **EMAIL_DEFAULT_PROVIDER**: Default provider. Allowed values: `plain`, `resend`.
  - **EMAIL_SENDER**: Sender address. Example: `no-reply@your.company.domain.com`.
  - **EMAIL_RESPONSE**: Response address. Example: `support@your.company.domain.com`.

### Repository Secrets
These values are encrypted and write-only in GitHub panel.

- Database:
  - **DB_URL**: Address of database, for example `jdbc:postgresql://some.host.com:5432/userland?sslmode=require`.
  - **DB_USERNAME**: Name of database user.
  - **DB_PASSWORD**: Password for database user.
- Google Cloud:
  - **GCP_PROJECT_ID**: Identificator of project on Google Cloud. Used for deploying project.
  - **GCP_WORKLOAD_IDENTITY_PROVIDER**: For WIP login on Google Cloud.
- Email:
  - **EMAIL_HOST**: Host for standard `plain` provider (Google etc).
  - **EMAIL_USERNAME**: Username (full email address) for standard `plain` provider (Google etc).
  - **EMAIL_PASSWORD**: Password (or App password) for standard `plain` provider (Google etc).
  - **TEP_RESEND_APIKEY**: API key for Transactional Email Provider called Resend. For `resend` provider.
- Other:
  - **JWT_SECRET**: JWT token secret. Must have at least 256 bits (32 bytes) and be string encoded in BASE64. To generate:
    - In Linux/macOS/Git Bash terminal execute `openssl rand -base64 32`.
    - In Windows terminal (PowerShell) execute `$b = [byte[]]::new(32); [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); [Convert]::ToBase64String($b)`.

## Local startup

In your IDE, ensure Java 25 Temurin is installed and selected.

You need to configure environment variables for your run configuration. Most variables have defaults, but some must be declared.

- Necessary **environment variables** (without them project will just fail to start):
  - You must explicitly set `JWT_SECRET`. System checks for unset/placeholder values to prevent misconfiguration silently using known key. 
- Needed **environment variables** (without them some parts of project won't work properly):
  - If you use email provider `plain`:
    - You must fill these variables: `EMAIL_HOST`, `EMAIL_USERNAME`, `EMAIL_PASSWORD`.
    - Keep in mind `plain` won't work on GCP Cloud Run - Google will block it.
  - If you use TEP Resend (email provider `resend`):
    - You need proper api key in `TEP_RESEND_APIKEY` provided by Resend.
    - You will also need custom domain registered in Resend.
- Optional **environment variables**:
  - If you want to use real database (like local PostgreSQL instance) instead of database in container, add in run config:
    - `SPRING_DATASOURCE_URL`=jdbc:postgresql://[URL] (keep in mind local instance likely does not require SSL)
    - `SPRING_DATASOURCE_USERNAME`=[NAME OF POSTGRESQL ACCOUNT]
    - `SPRING_DATASOURCE_PASSWORD`=[YOUR PASSWORD FOR ACCOUNT ABOVE]
    - `SPRING_DOCKER_COMPOSE_ENABLED`=false

## Testing

If you run test deployment locally via `TestUserLandApplication`, you need Docker engine running on your computer.
Same with running tests in general.
Additionally, you will need to add Maven goal `generate-resources` to run configuration before running any tests (caused by `build-info` execution in POM, needed to generate `META-INF/build-info.properties`).

### Commands

Note we use Maven wrapper.

Run all tests for this project: `.\mvnw.cmd clean test`

Run all tests from given test file: `.\mvnw.cmd clean test -Dtest=ClassNameTests`

### Coverage

For informative coverage you need to configure your coverage tool. In particular, you need to exclude:
- `org.portfolio.userland.swagger` package and everything inside
- All classes that have names ending in `Exception`.

This project uses **JaCoCo**. It is already configured in `pom.xml`.

## Endpoints

Server address on Google Cloud: https://userland-backend-java-299988087135.europe-central2.run.app

**Important**: due to use of free tier, first access might need a minute or so because everything needs to be spin up
(backend is zeroed out if not used for too long). It is advised to send request first to `/api/check/alive` endpoint and
wait for response. Now server should be up and responding normally.

UserLand has endpoints available publicly to use by frontend, PostMan etc.
- **Spring Actuator**: certain selected endpoints are available publicly, like health, metrics etc.
- **UserLand**: info about endpoints available publicly via Swagger.
  - https://userland-backend-java-299988087135.europe-central2.run.app/v3/api-docs (via PostMan)
  - https://userland-backend-java-299988087135.europe-central2.run.app/swagger-ui.html (can be used in browser)

## Tech stack

### Main

- **Java** 25 (Temurin)
- **Spring Boot** 4.1.0
- **PostgreSQL** 17.9 (Aiven)

### Dependencies

- Spring:
  - **Web**: REST endpoints
  - **Email**: Spring's way to send emails
  - **Security**: secures API endpoints
  - **Validation**: validate data
  - **Actuator**: provides endpoints to check on system state
  - **DevTools**: additional dev tools like hot restart
  - **Docker Compose Support**: use containers for stuff like database when project is executed locally
  - **Cache**: with Caffeine under the hood to handle caching in project.
- Database:
  - **PostgreSQL**: popular relational database
  - **Flyway**: versioning of database
- Tests:
  - **Instancio**: easily create randomized instances of entities for tests
  - **Awaitlility**: allow testing of async code
  - **Testcontainers**: instantiate real database (or anything else needed) for tests in container
  - **JaCoCo**: for coverage
- Other
  - **Lombok**: reduce Jave boilerplate code
  - **JJWT**: popular JWT library
  - **MapStruct**: translate DTO to actual entities
  - **ShedLock**: prevent issues with schedulers in environment like Kubernets
  - **Springdoc OpenAPI (Swagger UI)**: documenting API endpoints
