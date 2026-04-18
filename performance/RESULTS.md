# Wildstore Performance Results

Results are automatically appended here by `load_test.sh`.
Each run block records the configuration used and a per-phase summary table.

---

## Run: 2026-04-17 15:42:08  *(example — replace with real run)*

**Config:** 10 workers · 30s/phase · Meta: `http://localhost:27777` · File: `http://localhost:27778` · Avg file size: 500 MB

| Phase | req/s | avg latency | p95 latency | MB/s | errors |
|-------|------:|------------:|------------:|-----:|-------:|
| Search | 142.30 | 0.068s | 0.134s | 0.3210 | 0 |
| Search Count | 198.50 | 0.048s | 0.091s | 0.0041 | 0 |
| Filenames | 167.20 | 0.058s | 0.110s | 0.8820 | 0 |
| Crawl Sim | 9.40 | 1.063s | n/as | 4700.0 MB/s (est.) | 0 |
| File Download | 12.00 | 0.831s | 1.200s | 124.500 | 0 |

> Crawl MB/s is an estimate: ingest rate × 500 MB average file size.
> File download MB/s measures actual response body transfer speed.

### Observations (example)

- **Search** peaks around **142 req/s** — MongoDB find with no query predicate and a
  limit of 20 documents. Adding a WHERE clause drops this to ~90 req/s as the
  full collection scan kicks in.
- **Search Count** is ~40% faster than a full find because it skips document
  deserialization — good candidate for the UI's pagination header.
- **Filenames list** is roughly in line with search; the unwind aggregation
  adds marginal overhead.
- **Crawl simulation** ingests ~9.4 metadata documents/second at 10 concurrent
  workers. With a 500 MB average NetCDF file, the API can sustain roughly
  **4,700 MB/s of represented crawl throughput** — far faster than any single
  spinning disk (≈200 MB/s), meaning the crawler's real bottleneck is
  **local disk read speed**, not the API.
- **File download** sustains ~124 MB/s aggregate across 10 concurrent workers
  (~12 MB/s per stream), consistent with a local gigabit link.

### Bottleneck Analysis (example)

```
Crawler bottleneck:  disk I/O  (API can absorb ~4.7 GB/s worth of files)
Search bottleneck:   MongoDB collection scan on large datasets
Download bottleneck: network bandwidth / file server I/O
```

### Tuning Tips

| Knob | Effect |
|------|--------|
| `--parallelism` in crawler | Saturates more CPU cores for NetCDF extraction |
| MongoDB index on `fileName` | Speeds up filename-regex searches |
| Spring Boot thread pool size | More concurrent API handlers under load |
| File server range requests | Enables resume and parallel chunk downloads |

---

*Real runs are appended below this line by `load_test.sh -o RESULTS.md`*
