# Quality Checks for the App

## Automated Checks
- Run unit and integration tests with Maven.
- Execute CI on every push or pull request targeting develop.
- Keep the app buildable and testable in a clean environment.

## Local Commands
```bash
cd app/videogen
mvn test
mvn package
```

## Quality Goals
- No failing tests in CI.
- The Spring Boot app should start successfully.
- Health endpoint should remain available.
