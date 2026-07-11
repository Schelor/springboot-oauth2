
This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
./gradlew build          # Build the project
./gradlew test           # Run all tests
./gradlew bootRun        # Run the application (starts on http://localhost:8080/auth)
./gradlew clean          # Clean build artifacts
./gradlew test --tests="com.example.demogradle.AuthServerApplicationTests"  # Run a specific test
```

## Architecture Overview

This is a **Spring Boot 4 OAuth2 Authorization Server** using the `client_credentials` grant type. It issues JWT access tokens signed with RSA 2048-bit keys.

- **Java 25**, **Gradle**, **Jetty** (not Tomcat), **H2** in-memory DB (can be swapped for SQL Server)
- Base URL: `http://localhost:8080/auth`
- Token endpoint: `POST /auth/oauth2/token`
- JWKS endpoint: `GET /auth/.well-known/jwks.json`
- H2 Console: `http://localhost:8080/auth/h2-console`

### Security Filter Chains (in `SecurityConfig`)

Two `@Order`-annotated chains:
1. **Order 1** — Authorization Server chain: handles `/oauth2/**` and `/.well-known/**`
2. **Order 2** — Default chain: form login for all other endpoints (including H2 console with CSRF/iframe exemptions)

### Data Flow for Token Issuance

```
Client POST /oauth2/token (client_id + client_secret)
  → SecurityConfig (filter chains)
  → CustomRegisteredClientRepository.findByClientId()
  → SysOAuthClientRepository (JPA → H2 → sys_oauth_client table)
  → Returns RegisteredClient with CLIENT_CREDENTIALS grant, 2-hour token TTL
  → JWT signed with RSA key pair
```

### Key Source Files

| File | Purpose |
|------|---------|
| `config/SecurityConfig.java` | Security filter chains, RSA key pair, JWT decoder, BCrypt encoder |
| `service/CustomRegisteredClientRepository.java` | Bridges `SysOAuthClientPO` ↔ Spring's `RegisteredClient` |
| `entity/SysOAuthClientPO.java` | JPA entity for `sys_oauth_client` table |
| `repository/SysOAuthClientRepository.java` | JPA repo; key method: `findByClientId(String)` |
| `init/DataInitializer.java` | Seeds test client on startup (`test-client` / `test-secret`) |
| `controller/HelloController.java` | `GET /hello` test endpoint |

### OAuth2 Client Settings (hardcoded in `CustomRegisteredClientRepository`)

- Grant type: `CLIENT_CREDENTIALS`
- Auth method: `CLIENT_SECRET_POST`
- Access token TTL: 2 hours
- No refresh tokens, no consent required

### Database

H2 in-memory DB (`authdb`), DDL strategy `update`. To switch to SQL Server, update the driver dependency and datasource config in `application.yaml`.
