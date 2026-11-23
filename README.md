# travel-api

A personal travel diary app: a Spring Boot backend and a Flutter mobile app.

## Repository layout

- `backend/` - Spring Boot API
- `mobile/` - Flutter app
- `docker-compose.yml` - local PostgreSQL database
- `docs/mockups/` - UI mockups, color palette and navigation map

## Requirements

Docker Engine with Docker Compose v2.

## Start the database

```bash
cp .env.example .env   # then set your own POSTGRES_PASSWORD and JWT_SECRET in .env
docker compose up -d --wait
```

Check that it works (`db` should be `healthy`):

```bash
docker compose ps
docker compose exec db pg_isready -U travel_api -d travel_api
```

The database listens on `127.0.0.1:5432` by default; change `POSTGRES_PORT` in `.env` if that port is taken. Data is kept in the `pgdata` volume.

```bash
docker compose down      # stop, data stays
docker compose down -v   # stop and delete all data
```

## Backend

Java 21, Spring Boot 4.1.2, Gradle 9.8.2 (Kotlin DSL, wrapper in `backend/`), SpringDoc OpenAPI 3.2.0. Base package: `pl.travelapi`.

Run (the database must be running and `.env` created as above):

```bash
cd backend
./gradlew bootRun
```

Swagger UI: `http://localhost:8080/swagger-ui.html`

### Authentication

The API is stateless and uses signed JWT access tokens (HS256). Send the token with every request except the public ones:

```
Authorization: Bearer <token>
```

- Public: `/api/v1/auth/**` and Swagger (`/swagger-ui.html`, `/v3/api-docs`). A token sent to a public path is ignored, so an expired token kept in the app never blocks login or registration.
- Everything else requires a valid token; otherwise the response is `401` in the format below.
- The token carries the user id in the `sub` claim and expires after `JWT_EXPIRATION` (default `24h`). There is no refresh token: after expiry the user logs in again.
- `JWT_SECRET` (at least 32 bytes) must be set in `.env`; the application does not start without it. Tests use their own secret from `backend/src/test/resources/config/application.yml`.
- In a controller: `@AuthenticationPrincipal Jwt jwt`, then `JwtTokenService.userId(jwt)`.

### API error format

Every error is an RFC 9457 `ProblemDetail` (`application/problem+json`). `title` and `detail` are user-facing (Polish) and can be shown in the app as they are. Validation errors also carry `fieldErrors` (field name -> message). Messages come straight from the validation annotations, so **request DTOs must define Polish `message`s** (e.g. `@Email(message = "Podaj poprawny adres e-mail.")`). An error of the whole object (class-level constraint) has no field; its message is returned as `detail`. Standard Spring errors (unknown path, wrong HTTP method, unsupported media type) are returned in the same format with Polish texts.

```json
{
  "type": "about:blank",
  "title": "Nieprawidłowe żądanie",
  "status": 400,
  "detail": "Przesłane dane są nieprawidłowe.",
  "instance": "/api/v1/auth/register",
  "fieldErrors": { "email": "Podaj poprawny adres e-mail." }
}
```

| Status | When |
|--------|------|
| 400 | invalid or unreadable request body |
| 401 | missing/invalid token or bad credentials |
| 403 | authenticated but not allowed |
| 404 | resource not found (`ResourceNotFoundException`) or unknown path |
| 405 / 415 | wrong HTTP method / unsupported media type |
| 409 | conflict, e.g. email already taken (`ConflictException`) |
| 500 | unexpected error, generic message, no stack trace |

## UI mockups

Mockups of the main screens, the color palette, the component sheet and the navigation map: [docs/mockups](docs/mockups/README.md).
