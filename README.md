# USERLAND-BACKEND-JAVA

This is simple **backend project** demonstrating basics of modern Java and Spring Boot. It is intended to be used with **frontend project** that I will write later.

Project is actively developed and functionality is already partially implemented.

Description below sums up intended state when project is completed: functionality, features etc.

## Basic info

Author: Paweł Papierkowski

Date: 2026

Link to webpage: https://github.com/pawel-papierkowski/userland-backend-java

## Functionality

This project fully manages user. This is where name **UserLand** comes from.

You can think of it as baseline for any other project, as almost any project, system or application will need user accounts.

### Features

- Standard user:
  - User **registration**.
  - User **activation** via email.
  - User **login**.
  - User **logout**.
  - **Password reset** via email.
  - User **account deletion** (confirmation via email).
- Handling **user permissions** (standard user vs panel admin operator).
- **Admin user**:
  - Viewing **table of users**, including pagination and filtration.
  - Viewing data of selected user.
    - Editing data of user. 
    - Viewing user **history**. 
    - User **lock/unlock**.
- Other options: 
  - **System lockdown**: if turned on, only users with admin rights can call any endpoint.
  - **System history** for global events like lockdown. 

## GitHub config

You need to add these Repository Secrets:
- General configuration:
  - **PROFILE**: Informs system what is nature of environment. Allowed values: `PROD`, `STAGE`, `DEV`, `TEST`.
- Database:
  - **DB_USERNAME**: Name of database user.
  - **DB_PASSWORD**: Password for database user
  - **DB_URL**: Address of database, for example `jdbc:postgresql://some.host.com:5432/userland?sslmode=require` .
- Google Cloud: 
  - **GCP_PROJECT_ID**: Identificator of project on Google Cloud. Used for deploying project.
  - **GCP_WORKLOAD_IDENTITY_PROVIDER**: For WIP login on Google Cloud.
- Email:
  - **EMAIL_HOST**: Host for standard email provider (Google etc).
  - **EMAIL_USERNAME**: Username (full email address) for standard email provider (Google etc).
  - **EMAIL_PASSWORD**: Password (or App password) for standard email provider (Google etc).
  - **TEP_RESEND_APIKEY**: API key for Transactional Email Provider called Resend.
- Other:
  - **JWT_SECRET**: JWT token secret. Must have at least 256 bits (32 bytes) and be string encoded in BASE64.
    - Best way to generate: in Linux/macOD/Git Bash terminal execute `openssl rand -base64 32`.

## Local startup

Ensure Java 25 Temurin is installed and selected.

If you want to use real database instead of container, add in run config:
- **Environment variables**:
  - `SPRING_DATASOURCE_URL`=jdbc:postgresql://[URL]
  - `SPRING_DATASOURCE_USERNAME`=[NAME OF POSTGRESQL ACCOUNT]
  - `SPRING_DATASOURCE_PASSWORD`=[YOUR PASSWORD]
  - `SPRING_DOCKER_COMPOSE_ENABLED`=false

## Testing

If you run test deployment locally via `TestUserLandApplication`, you need Docker engine running on your computer.
Same with running tests in general.

### Coverage

For informative coverage you need to configure your coverage tool. In particular, you need to exclude:
- `org.portfolio.userland.swagger` package and everything inside
- All classes that have names ending in `Exception`.

This project uses **JaCoCo**. It is already configured in `pom.xml`.

## Deployment

This app uses (free tier for all of these):
- **Google Cloud Run** for backend hosting.
- **Aiven** for PostgreSQL database hosting.
- **Resend** (or JavaMailSender) for email services.

**UserLand** app is deployed via **GitHub Actions**.

For portfolio, email address pawel.papierkowski.portfolio@gmail.com is used and any emails from this system will have this address as sender.

## Design notes

- All date/time fields are **without timezone**. Frontend should convert it properly to show date/time on screen in local timezone.
- **Kafka** was considered for demonstration purposes (email retries), but not used since it won't work well with restrictions typical of Google Cloud free tier, where this project lives. GCP is serverless, but Kafka would require system to be up at all times.

## Endpoints

Server address on Google Cloud: https://userland-backend-java-299988087135.europe-central2.run.app

Important: due to use of free tier, first access might need a minute or so because everything needs to be spin up
(backend is zeroed out if not used for too long).

UserLand has endpoints available publicly to use by frontend, PostMan etc.
- **Spring Actuator**: certain selected endpoints are available publicly, like health, metrics etc.
- **UserLand**: info about endpoints available publicly via Swagger.
  - https://userland-backend-java-299988087135.europe-central2.run.app/v3/api-docs (via PostMan)
  - https://userland-backend-java-299988087135.europe-central2.run.app/swagger-ui.html (can be used in browser)

## Tech stack

### Main

- **Java** 25 (Temurin)
- **Spring Boot** 4.0.5
- **PostgreSQL** 17.9 (Aiven)

### Dependencies

- Spring:
  - **Web**: REST endpoints
  - **Email**: Spring's way to send emails
  - **Security**: secures API endpoints
  - **Validation**: validate data
  - **Actuator**: gives endpoints to check on system state
  - **DevTools**: additional dev tools
  - **Docker Compose Support**: use containers for stuff like database when project is executed locally
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
