# Project Overview

Wildstore is a scientific data management system for NetCDF files. It provides metadata extraction, search, file sharing, and a web UI with map-based geospatial visualization. Built at SJSU as a multi-module Java/React application.

## Repository Structure

```
wildstore/
├── wildstore-common/      # Shared models, utilities, NetCDF processing
├── wildstore-meta/        # Main Spring Boot server + embedded React frontend
│   └── app/               # React 18 frontend source
├── wildstore-crawl/       # NetCDF file crawler for metadata extraction
├── wildstore-fileserve/   # File serving microservice
├── wildstore-cli/         # CLI for search, share links, cleanup
├── wildstore-testdata/    # Test data generation and NetCDF test fixtures
├── wsFetch/               # Python download utility
├── wildfirestorage-*/     # DEPRECATED - old module names, ignore these
└── pom.xml                # Parent Maven POM
```

## Tech Stack

**Backend:** Java 17, Spring Boot 3.5.0, Spring Data MongoDB, Spring Security OAuth2 (Google/GitHub), PicoCLI 4.7.6
**Frontend:** React 18, Redux Toolkit, React Router 6, Leaflet.js, Tailwind CSS 3 + DaisyUI
**Database:** MongoDB
**Build:** Maven multi-module, frontend-maven-plugin (Node 18, npm 9)
**Testing:** JUnit Jupiter 5.12, React Testing Library
**CI:** GitHub Actions (`.github/workflows/maven.yml`)

## Build Commands

```bash
# Full build (backend + frontend)
mvn package

# Build skipping tests
mvn package -DskipTests

# Run tests
mvn test

# Frontend only (dev)
cd wildstore-meta/app && npm install && npm start
```

## Running Locally

1. Start MongoDB on port 27017
2. Build: `mvn package`
3. Start metadata server: `java -jar wildstore-meta/target/wildstore-meta-server.jar --spring.data.mongodb.uri=mongodb://localhost:27017/wildfire --server.port=27777`
4. Start file server: `java -jar wildstore-fileserve/target/wildstore-fileserve.jar --custom.metadataServer=http://127.0.0.1:27777 --server.port=27778`

For frontend dev mode, run `npm start` from `wildstore-meta/app/` (serves on port 3000).

## Key Configuration

- **Meta server config:** `wildstore-meta/src/main/resources/application.yml` (port 27777)
- **File server config:** `wildstore-fileserve/src/main/resources/application.yml` (port 27778)
- **Frontend proxy:** `wildstore-meta/app/package.json` (proxies to localhost:8080 in dev)
- **Tailwind config:** `wildstore-meta/app/tailwind.config.js`
- OAuth2 requires Google/GitHub credentials in `properties.yml` or CLI args

## Code Conventions

- **Java package:** `edu.sjsu.wildstore` (subpackages: `.meta`, `.meta.controller`, `.meta.service`, `.meta.configuration`, `.meta.util`, `.meta.worker`)
- **Frontend components:** `wildstore-meta/app/src/components/` with subdirectories per feature
- **Redux store:** `wildstore-meta/app/src/redux/`
- **Compiler flag:** `-parameters` is enabled for reflection support
- **Module dependencies flow:** common → crawl/meta/fileserve/cli → testdata (test)

## CI/CD

GitHub Actions runs on push/PR to `main`:
- Ubuntu latest + MongoDB 6.0 service
- JDK 17 (Temurin)
- `mvn install -B -ntp`

## Changes

- When fixing problems create a test case to reproduce the problem
- Fix the problem
- Make sure the test cases pass
