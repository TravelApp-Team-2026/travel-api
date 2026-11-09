# travel-api

A personal travel diary app: a Spring Boot backend and a Flutter mobile app.

## Repository layout

- `backend/` - Spring Boot API
- `mobile/` - Flutter app
- `docker-compose.yml` - local PostgreSQL database

## Requirements

Docker Engine with Docker Compose v2.

## Start the database

```bash
cp .env.example .env   # then set your own POSTGRES_PASSWORD in .env
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
