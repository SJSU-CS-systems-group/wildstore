# Wildstore Performance Load Test

A Bash load-generator that stress-tests three key Wildstore paths:
search query throughput, metadata ingest rate (crawl simulation), and file
download speed — all driven by plain `curl` with no extra dependencies.

## Prerequisites

| Tool | Why |
|------|-----|
| `curl` | HTTP requests |
| `awk` | Stats calculation |
| `bc` | Floating-point arithmetic |
| Running Wildstore meta server | Port 27777 by default |
| Running Wildstore file server | Port 27778 (optional, for download phase) |
| Bearer token | `USER` role minimum; `ADMIN` for crawl cleanup |

Get your bearer token from the running server:

```bash
curl -s http://localhost:27777/api/oauth/token \
  -H "Cookie: <your-session-cookie>"
```

Or copy it from the browser's Network tab after logging in.

## Quick Start

```bash
cd performance/

# Minimum — only meta server, search + ingest phases
TOKEN=your_token_here ./load_test.sh

# Full run with file download test
TOKEN=tok DIGEST=abc123sha256 ./load_test.sh

# Crank it up
TOKEN=tok CONCURRENCY=25 DURATION=60 ./load_test.sh
```

## All Options

```
./load_test.sh [options]

  -u URL     Meta server URL           default: http://localhost:27777
  -f URL     File server URL           default: http://localhost:27778
  -t TOKEN   Bearer auth token         (required)
  -c N       Concurrent workers        default: 10
  -d N       Duration per phase (sec)  default: 30
  -D DIGEST  SHA-256 digest of a file for the download phase
  -s MB      Average NetCDF file size  default: 500 MB (used for crawl estimate)
  -o FILE    Append markdown results to FILE (default: RESULTS.md in this dir)
  -h         Help
```

All flags can also be set via environment variables:

```
META_URL  FILE_URL  TOKEN  CONCURRENCY  DURATION  DIGEST  AVG_FILE_MB  OUTPUT_MD
```

## Test Phases

### Phase 1 — Search (`POST /api/metadata/search`)

Hammers the full-text search endpoint with an empty query (returns all
documents, paginated at 20). Measures how many search requests the server
can handle per second under load.

### Phase 2 — Search Count (`POST /api/metadata/search/count`)

Same query, count-only endpoint. The MongoDB aggregation pipeline is
different from a find, so this isolates counting performance.

### Phase 3 — Filename Listing (`GET /api/metadata/filenames`)

Paginated filename list (limit=100). Tests the aggregation + unwind pipeline
that powers the crawler's change-detection preflight.

### Phase 4 — Crawl Simulation (`POST /api/metadata`)

Each worker posts a unique synthetic metadata document (a minimal NetCDF
record with a fresh UUID-style digest). This isolates **API ingest rate**,
which is one of the two bottlenecks in a real crawl (the other is local
disk read speed).

**Estimated crawl MB/s** = `ingest_rps × AVG_FILE_MB`

For example, if the API accepts 8 docs/s and average file size is 500 MB,
estimated crawl throughput is **4,000 MB/s** (disk I/O would be the real
limit at that point).

> **Cleanup:** test documents are inserted with `fileName` prefix `perf_test_*`.
> The script prints a ready-to-run `curl` cleanup command at the end of phase 4.
> You need an ADMIN-role token to delete them.

### Phase 5 — File Download (`GET /api/file/<digest>`)

Skipped unless `-D <digest>` is supplied. Concurrently downloads the same
file N times and reports actual response body transfer rate in MB/s.

Find a valid digest:

```bash
curl -s "$META_URL/api/metadata/filenames?limit=1&offset=0" \
  -H "Authorization: Bearer $TOKEN" \
  | python3 -c "import json,sys; d=json.load(sys.stdin); print(d[0]['fileName'])"
# then look up its digest:
curl -s "$META_URL/api/metadata?filename=<name>" \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool | grep digestString
```

## Example Invocations

```bash
# Quick smoke-test (5 workers, 15s each phase)
TOKEN=abc ./load_test.sh -c 5 -d 15

# Sustained soak test (50 workers, 5 min per phase)
TOKEN=abc ./load_test.sh -c 50 -d 300

# Include file download, write results to a dated file
TOKEN=abc DIGEST=deadbeef123 ./load_test.sh -d 60 -o results_$(date +%F).md

# Use non-default server addresses (e.g., staging)
./load_test.sh -u http://staging:27777 -f http://staging:27778 -t abc

# Adjust crawl estimate for small files (e.g., 50 MB average)
./load_test.sh -t abc -s 50
```

## Reading the Output

```
  Phase                   req/s     avg lat    p95 lat        MB/s   errors
  ──────────────────────────────────────────────────────────────────────────
  Search                  142.30     0.068s     0.134s       0.3210       0
  Search Count            198.50     0.048s     0.091s       0.0041       0
  Filenames               167.20     0.058s     0.110s       0.8820       0
  Crawl Sim                 9.40     1.063s        n/a   4700.0 MB/s       2
  File Download            12.00     0.831s     1.200s     124.500       0
```

- **req/s** — successful + failed requests per second across all workers
- **avg / p95 lat** — mean and 95th-percentile response time in seconds
- **MB/s** — for search/list: response body transfer; for crawl: estimated
  throughput at the configured average file size; for download: raw transfer

Results are automatically appended to `RESULTS.md` in this directory.
