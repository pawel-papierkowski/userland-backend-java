# USERLAND-BACKEND-JAVA

This is very simple **backend project** demonstrating basics of Java and Spring Boot. It is intended to be used with **frontend project** that I will write later.

Project is in early stages. Description below sums up planned functionality, future features etc.

## Functionality

This project fully handles user. This is where name **UserLand** comes from.

You can think of it as baseline for any other project, as almost anything will need user account.

### Features

- Standard user:
  - User registration (with e-mail confirmation)
  - User login
  - User logout
  - Password reset
  - User account deletion
- Different rights (standard user vs panel admin operator)
- Admin user:
  - Viewing list of users, including pagination and filtration
  - User block/unblock

## GitHub config

You need to add these Repository Secrets:
- **DB_USERNAME**: Name of database user.
- **DB_PASSWORD**: Password for database user
- **DB_URL**: Address of database, for example jdbc:postgresql://some.host.com:5432/userland?sslmode=require .
- **GCP_PROJECT_ID**: Identificator of project on Google Cloud. Used for deploying project.
- **GCP_WORKLOAD_IDENTITY_PROVIDER**: For WIF login on Google Cloud.
- **JWT_SECRET**: JWT token secret. Must have 256 bits and be encoded in BASE64.
  - Best way to generate: in Linux/macOD/Git Bash terminal execute *openssl rand -base64 32*.

## Local startup

Ensure Java 25 Temurin is selected.

If you want to use real database instead of container, add in run config:
- **Environment variables**:
  - SPRING_DATASOURCE_URL=jdbc:postgresql://[URL]
  - SPRING_DATASOURCE_USERNAME=[NAME OF POSTGRESQL ACCOUNT]
  - SPRING_DATASOURCE_PASSWORD=[YOUR PASSWORD]
  - SPRING_DOCKER_COMPOSE_ENABLED=false

If you run test deployment locally via TestUserLandApplication, you need Docker engine running on your computer.
Same with running tests in general.

## Deployment

This app uses:
- **Google Cloud Run** for backend hosting.
- **Aiven** for PostgreSQL.

**UserLand** app is deployed via **GitHub Actions**.

## Endpoints

Server address on Google Cloud: https://userland-backend-java-299988087135.europe-central2.run.app

Important: due to use of free tier, first access might need a minute or so because everything needs to be spin up
(backed is zeroed out if not used for too long).

System has endpoints available publicly to use by frontend, PostMan etc.
- Spring Actuator: certain selected endpoints are available publicly, like health, metrics etc.
- UserLand: info about endpoints available publicly via Swagger.
  - https://userland-backend-java-299988087135.europe-central2.run.app/v3/api-docs (via PostMan)
  - https://userland-backend-java-299988087135.europe-central2.run.app/swagger-ui.html (can be used in browser)

## Tech stack

### Main

- **Java** 25 (Temurin)
- **Spring Boot** 4.0.5
- **PostgreSQL** 17.9 (Aiven)

### Dependencies

- Spring:
  - Web
  - Security
  - Validation
  - Actuator
  - DevTools
  - Docker Compose Support
- Database:
  - Testcontainers
  - PostgreSQL
  - Flyway
- Other
  - Lombok
  - Springdoc OpenAPI (Swagger UI)
  - JJWT
  - MapStruct
