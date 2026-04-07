# USERLAND-BACKEND-JAVA

This is very simple backend project demonstrating basics of Java and Spring Boot. It is intended to be used with frontend project that I will write later.

Project is in early stages. Description below sums up planned functionality, features etc.

## Functionality

This project fully handles user. This is where name UserLand comes from.

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

## Tech stack

### Main

- Java 25
- Spring Boot 4.0.5

### Uses

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
