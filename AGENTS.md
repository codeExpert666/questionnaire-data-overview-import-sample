# Repository Guidelines

## Project Structure & Module Organization

This repository is a Spring Boot 3 sample for questionnaire Excel template download and import. Java source lives in `src/main/java/com/acme/questionnaire`, grouped by role: `controller`, `service`, `excel`, `mapper`, `model`, `dto`, `param`, `ref`, `config`, and `exception`. MyBatis XML mappings are in `src/main/resources/mapper`, and runtime configuration is in `src/main/resources/application.yml`. Database reference material is under `db/`, including `schema-reference.sql` and migration notes.

## Build, Test, and Development Commands

- `mvn clean package`: compile the Java 17 application, run tests, and build the Spring Boot artifact under `target/`.
- `mvn test`: run the Maven test phase. The repository currently has no `src/test` tests, so add tests before relying on this as meaningful coverage.
- `mvn spring-boot:run`: start the sample service locally. Configure MySQL and Redis through `application.yml` defaults or environment variables such as `SPRING_DATASOURCE_URL`.

## Coding Style & Naming Conventions

Use standard Java conventions with 4-space indentation and clear package-level separation. Keep class names descriptive and role-based, for example `QuestionnaireDataOverviewExcelService`, `QuestionnaireOpinionImportListener`, and `AnswerMapper`. Prefer constructor injection with Lombok `@RequiredArgsConstructor`, as used in existing services. Keep MyBatis mapper interfaces and XML files paired by name, such as `AnswerMapper.java` and `AnswerMapper.xml`.

## Testing Guidelines

Use Spring Boot’s test stack from `spring-boot-starter-test`. Place unit and integration tests under `src/test/java` with names ending in `Test` or `IntegrationTest`. Prioritize tests around Excel parsing, validation rollback behavior, category resolution, and MyBatis writer operations. When database behavior is involved, make setup data explicit and document any required MySQL or Redis dependency in the test class or README.

## Commit & Pull Request Guidelines

Git history currently contains only `Initial import`, so use concise imperative commit subjects going forward, for example `Add import validation tests` or `Document database setup`. Keep each commit focused on one concern. Pull requests should include a short summary, test evidence such as `mvn test`, and any configuration or schema changes.

## Security & Configuration Tips

Do not commit real database passwords, Redis credentials, or production endpoint values. Use environment variables for datasource overrides. Keep upload limits and row limits in `questionnaire.import` aligned with deployment capacity before using this sample in production.
