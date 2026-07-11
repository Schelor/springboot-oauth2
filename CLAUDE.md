
This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Structure

Multi-module Gradle project with three independent Spring Boot 4 applications:

```
springboot-authserver/        # OAuth2 Authorization Server (fully implemented)
springboot-oauth2-client/     # OAuth2 Client (stub)
springboot-oauth2-server/     # OAuth2 Resource Server (stub)
```

Each module has its own `build.gradle` and is an independent Spring Boot application. There is no root `settings.gradle` that ties them together — they are co-located but not a composite build.

## Build & Run Commands

Run from within the target module directory (e.g., `cd springboot-authserver`):

```bash
./gradlew build                   # Build the module
./gradlew test                    # Run all tests
./gradlew bootRun                 # Run the application
./gradlew clean                   # Clean build artifacts
./gradlew test --tests="com.example.demogradle.AuthServerApplicationTests"  # Run a specific test
```

- **Java 25**, **Gradle**, **Jetty** (Tomcat explicitly excluded via dependency configuration)
- Aliyun Maven mirrors are configured for dependency resolution (Chinese mirror of Maven Central)

## Auth Server Architecture

See `springboot-authserver/CLAUDE.md` for full details. Summary:

- **Base URL**: `http://localhost:8080/auth`
- **Token endpoint**: `POST /auth/oauth2/token`
- **JWKS endpoint**: `GET /auth/.well-known/jwks.json`
- **H2 Console**: `http://localhost:8080/h2-console`

### Two Security Filter Chains (`SecurityConfig.java`)

1. **Order 1** — Authorization Server chain: handles `/oauth2/**` and `/.well-known/**`
2. **Order 2** — Default chain: form login for all other endpoints (H2 console with CSRF/iframe exemptions)

### Key Source Files (under `springboot-authserver/src/main/java/com/example/demogradle/`)

| File | Purpose |
|------|---------|
| `config/SecurityConfig.java` | Security filter chains, RSA key pair generation, JWT decoder, BCrypt encoder |
| `service/CustomRegisteredClientRepository.java` | Bridges JPA entity `SysOAuthClientPO` ↔ Spring's `RegisteredClient` |
| `entity/SysOAuthClientPO.java` | JPA entity for `sys_oauth_client` table |
| `repository/SysOAuthClientRepository.java` | JPA repo; key method: `findByClientId(String)` |
| `init/DataInitializer.java` | Seeds test client on startup (`test-client` / `test-secret`) |
| `controller/HelloController.java` | `GET /hello` test endpoint |

### Token Issuance Flow

```
Client POST /oauth2/token (client_id + client_secret)
  → SecurityConfig filter chains
  → CustomRegisteredClientRepository.findByClientId()
  → SysOAuthClientRepository (JPA → H2 → sys_oauth_client table)
  → RegisteredClient (CLIENT_CREDENTIALS grant, CLIENT_SECRET_POST, 2-hour TTL)
  → JWT signed with RSA 2048-bit key pair
```

### Database

H2 in-memory DB (`authdb`), DDL strategy `update`. Seeded at startup by `DataInitializer`. To switch to SQL Server, update the driver dependency and datasource config in `application.yaml`.