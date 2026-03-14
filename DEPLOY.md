# Deployment Guide

## CD Pipeline Overview

The CD pipeline follows a **GitHub → Canary → Release** strategy:

```
Push to main
    │
    ▼
[build-and-push]         Build Docker images, push to GHCR, create GitHub pre-release
    │
    ▼
[canary-health-check]    Start containers in CI, curl /actuator/health
    │
    ▼
[promote-release]        Tag images as :latest, promote pre-release to full release
```

1. **Build and push**: On evfery merge to `main`, GitHub Actions builds Docker images for `wildstore-meta` and `wildstore-fileserve`, pushes them to GitHub Container Registry (GHCR), and creates a GitHub **pre-release** (canary).

2. **Canary health check**: A fresh Ubuntu VM spins up MongoDB and both app containers. It curls `/actuator/health` on each service in a retry loop (up to 60 seconds). If both return `{"status":"UP"}`, the canary passes. The VM is destroyed after — nothing stays running.

3. **Promote release**: If the canary passes, the images are re-tagged as `:latest` and the GitHub pre-release is promoted to a full release.

### Production deploys

A cron job on the production server runs `deploy/deploy.sh` nightly. The script checks the GitHub Releases API for a new full (non-prerelease) release. If one exists, it pulls the new images, restarts the containers, and verifies health. If the health check fails, it does **not** update the marker file, so it retries the next night.

## Port Configuration

The meta server's `application.yml` defaults to port **27777**, but we override it to **8080** everywhere via `server.port=8080` in docker-compose environment variables. This is because the Google OAuth redirect URIs are registered for `localhost:8080`. Using a different port would cause a `redirect_uri_mismatch` error. The same applies to production: the OAuth app must have the production URL registered.

The fileserve server defaults to port **27778** from its `application.yml` and does not need an override.

## OAuth Credentials

OAuth credentials live in an `oauth.yml` file on the host machine and are mounted into the container at runtime as a read-only volume:

```yaml
...
volumes:
  - ./oauth.yml:/app/oauth.yml:ro
environment:
  - spring.config.additional-location=file:/app/oauth.yml
...
```

```yaml
# oauth.yml
spring:
  security:
    oauth2:
      client:
        registration:
          github:
            clientId: <INSERT CLIENT ID>
            clientSecret: <INSERT CLIENT SECRET>
          google:
            clientId: <INSERT CLIENT ID>
            clientSecret: <INSERT CLIENT SECRET>
```

## Health Checks (Actuator)

Both services include Spring Boot Actuator, which exposes a `/actuator/health` endpoint that returns `{"status":"UP"}`. It is used by:

- The Dockerfile `HEALTHCHECK` (Docker monitors container health, visible in `docker ps`)
- The CD workflow canary check
- The production deploy script

## Running Locally with Docker

### Build and start

```bash
docker compose build
docker compose up
```

The app will be available at `http://localhost:8080`.

### Stop containers

```bash
docker compose down
```

### Stop containers and wipe the database

```bash
docker compose down -v
```

**Warning**: The `-v` flag deletes the MongoDB volume. All data is lost. Never do this in production unless you intend to start fresh.

## First-Time Setup

When running against an empty database (first run, or after wiping the volume), you need to set up an admin user and populate data.

### 1. Create an admin user

Start the app and log in via Google or GitHub at `http://localhost:8080`. This creates your user in MongoDB, but with a default role that doesn't have access to the search dashboard.

Promote yourself to admin using `mongosh` or MongoDB Compass:

```bash
mongosh mongodb://localhost:27017/wildfire
```

```javascript
db.userData.updateOne(
  { email: "your-email@example.com" },
  { $set: { role: "ROLE_ADMIN" } }
)
```

You should now have access to the search dashboard after refreshing.

### 2. Get an API token

After logging in as admin, go to the token page (`/token`). Copy your opaque token from the page. Create a `token.txt` file in the repo root:

```
token=your-token-here
```

### 3. Populate the database with test data

```bash
# Unzip test data into the repo root
unzip wildstore-testdata/src/main/resources/testdata.zip -d testdata/

# Generate a file list
find testdata/ -name '*.nc' > data_list.lst

# Run the crawler
java -jar wildstore-crawl/target/wildstore-crawler.jar \
  --metaURL=http://localhost:8080 \
  --tokenFile token.txt \
  data_list.lst
```

The crawler reads each NetCDF file, extracts metadata, and inserts it into MongoDB. You should now see data on the search dashboard.

## Production Notes

### Preserving data across deploys

The `deploy/docker-compose.prod.yml` uses a named Docker volume (`mongo-data`) for MongoDB. This volume persists across `docker compose down` and `docker compose up`. The deploy script **never** passes `-v`, so data survives deployments.

### Migrating from a standalone MongoDB to Docker

If the production server currently runs MongoDB directly (e.g., `mongod --dbpath ~/mongodb/data`), you can migrate the data into the Docker volume:

```bash
# 1. Export from the existing MongoDB
mongodump --uri="mongodb://localhost:27017/wildfire" --out=/tmp/wildfire-dump

# 2. Start just the MongoDB container
docker compose -f deploy/docker-compose.prod.yml up -d mongo

# 3. Import into the containerized MongoDB
mongorestore --uri="mongodb://localhost:27017/wildfire" /tmp/wildfire-dump/wildfire/

# 4. Start the rest of the services
docker compose -f deploy/docker-compose.prod.yml up -d
```

Note: `mongodump` and `mongorestore` need to be installed on the host (`mongodb-database-tools` package). The MongoDB container exposes port 27017 only on the internal Docker network by default — if you need to run `mongorestore` from the host, temporarily add `ports: - "27017:27017"` to the mongo service in the production compose file, then remove it after migration.
