#!/usr/bin/env bash
#
# Wildstore Performance Load Test
# Stress-tests search, file download, and metadata ingest (crawl simulation).

set -euo pipefail

# ── Colours ─────────────────────────────────────────────────────────────────────
RED='\033[0;31m'; GRN='\033[0;32m'; YLW='\033[1;33m'
BLU='\033[0;34m'; CYN='\033[0;36m'; MAG='\033[0;35m'
BOLD='\033[1m'; DIM='\033[2m'; RST='\033[0m'

# ── Defaults ────────────────────────────────────────────────────────────────────
META_URL="${META_URL:-http://localhost:27777}"
FILE_URL="${FILE_URL:-http://localhost:27778}"
TOKEN="${TOKEN:-}"
CONCURRENCY="${CONCURRENCY:-10}"
DURATION="${DURATION:-30}"
DIGEST="${DIGEST:-}"
AVG_FILE_MB="${AVG_FILE_MB:-500}"
OUTPUT_MD="${OUTPUT_MD:-}"   # if set, append results to this file

TMP_ROOT=$(mktemp -d)
trap 'rm -rf "$TMP_ROOT"' EXIT

# ── Argument parsing ─────────────────────────────────────────────────────────────
usage() {
    cat <<EOF
Usage: $(basename "$0") [options]

Options:
  -u URL     Meta server URL       (default: $META_URL)
  -f URL     File server URL       (default: $FILE_URL)
  -t TOKEN   Bearer auth token     (required; or set TOKEN env var)
  -c N       Concurrent workers    (default: $CONCURRENCY)
  -d N       Duration per phase    (default: ${DURATION}s)
  -D DIGEST  File digest for download test (phase skipped if omitted)
  -s MB      Avg NetCDF size in MB for crawl estimate (default: $AVG_FILE_MB)
  -o FILE    Append markdown results to FILE
  -h         Show this help

Environment variables override defaults: META_URL FILE_URL TOKEN CONCURRENCY
                                         DURATION DIGEST AVG_FILE_MB OUTPUT_MD
EOF
    exit 0
}

while getopts "u:f:t:c:d:D:s:o:h" opt; do
    case $opt in
        u) META_URL="$OPTARG"   ;;
        f) FILE_URL="$OPTARG"   ;;
        t) TOKEN="$OPTARG"      ;;
        c) CONCURRENCY="$OPTARG";;
        d) DURATION="$OPTARG"   ;;
        D) DIGEST="$OPTARG"     ;;
        s) AVG_FILE_MB="$OPTARG";;
        o) OUTPUT_MD="$OPTARG"  ;;
        h) usage ;;
        *) usage ;;
    esac
done

# ── Helpers ──────────────────────────────────────────────────────────────────────
die()    { echo -e "${RED}ERROR: $*${RST}" >&2; exit 1; }
info()   { echo -e "  ${DIM}$*${RST}"; }
ok()     { echo -e "  ${GRN}✓${RST} $*"; }
warn()   { echo -e "  ${YLW}⚠${RST}  $*"; }
metric() { printf "  %-22s ${BOLD}%s${RST}\n" "$1" "$2"; }

section() {
    local label="$1"
    local pad
    pad=$(printf '%0.s─' $(seq 1 $((54 - ${#label}))))
    echo -e "\n${BOLD}${CYN}── $label $pad${RST}"
}

# ── Prerequisite checks ──────────────────────────────────────────────────────────
for dep in curl awk bc; do
    command -v "$dep" >/dev/null 2>&1 || die "Required tool not found: '$dep'"
done

[[ -z "$TOKEN" ]] && die "Bearer token required.\n  Use -t TOKEN or set the TOKEN environment variable.\n  Get yours from: ${META_URL}/api/oauth/token"

# ── Banner ───────────────────────────────────────────────────────────────────────
echo -e "${BOLD}${MAG}"
cat <<'BANNER'
 ██╗    ██╗██╗██╗     ██████╗ ███████╗████████╗ ██████╗ ██████╗ ███████╗
 ██║    ██║██║██║     ██╔══██╗██╔════╝╚══██╔══╝██╔═══██╗██╔══██╗██╔════╝
 ██║ █╗ ██║██║██║     ██║  ██║███████╗   ██║   ██║   ██║██████╔╝█████╗
 ██║███╗██║██║██║     ██║  ██║╚════██║   ██║   ██║   ██║██╔══██╗██╔══╝
 ╚███╔███╔╝██║███████╗██████╔╝███████║   ██║   ╚██████╔╝██║  ██║███████╗
  ╚══╝╚══╝ ╚═╝╚══════╝╚═════╝ ╚══════╝   ╚═╝    ╚═════╝ ╚═╝  ╚═╝╚══════╝
            P E R F O R M A N C E   L O A D   T E S T
BANNER
echo -e "${RST}"
echo -e "  ${DIM}Meta server : ${BLU}$META_URL${RST}"
echo -e "  ${DIM}File server : ${BLU}$FILE_URL${RST}"
echo -e "  ${DIM}Workers     : ${YLW}$CONCURRENCY${RST}${DIM} concurrent${RST}"
echo -e "  ${DIM}Duration    : ${YLW}${DURATION}s${RST}${DIM} per phase${RST}"
[[ -n "$DIGEST" ]] && echo -e "  ${DIM}Digest      : ${DIGEST:0:16}…${RST}"
echo ""

RUN_DATE=$(date '+%Y-%m-%d %H:%M:%S')

# ── Health check ─────────────────────────────────────────────────────────────────
section "Health Check"

meta_code=$(curl -s -o /dev/null -w "%{http_code}" --connect-timeout 5 \
    -H "Authorization: Bearer $TOKEN" "$META_URL/api/oauth/user" 2>/dev/null || echo "000")

[[ "$meta_code" == "000" ]] && die "Cannot reach meta server at $META_URL"
ok "Meta server reachable (HTTP $meta_code)"

file_code=$(curl -s -o /dev/null -w "%{http_code}" --connect-timeout 5 \
    "$FILE_URL/" 2>/dev/null || echo "000")
SKIP_DOWNLOAD=false
if [[ "$file_code" == "000" ]]; then
    warn "File server at $FILE_URL not reachable — download phase will be skipped"
    SKIP_DOWNLOAD=true
else
    ok "File server reachable (HTTP $file_code)"
fi
[[ -z "$DIGEST" ]] && warn "No DIGEST provided — file download phase will be skipped"

# ── Phase engine ─────────────────────────────────────────────────────────────────
# Accumulate summary rows: "Phase|RPS|AvgLat|MB/s|Errors"
declare -a SUMMARY_ROWS=()

# run_phase <name> <url> <method> [body]
# Spawns CONCURRENCY workers for DURATION seconds, each hammering the endpoint
# with curl. Collects http_code / time_total / size_download per request.
run_phase() {
    local name="$1"
    local url="$2"
    local method="${3:-GET}"
    local body="${4:-}"

    local dir="$TMP_ROOT/${name// /_}"
    mkdir -p "$dir"
    local end_ts=$(( $(date +%s) + DURATION ))
    local start_ts=$(date +%s)

    # ── spawn workers ────────────────────────────────────────────────────────────
    local pids=()
    for w in $(seq 1 "$CONCURRENCY"); do
        (
            local out="$dir/w${w}.dat"
            while [ "$(date +%s)" -lt "$end_ts" ]; do
                r=$(curl -s -o /dev/null \
                    -w "%{http_code} %{time_total} %{size_download}" \
                    -X "$method" \
                    -H "Authorization: Bearer $TOKEN" \
                    -H "Content-Type: application/json" \
                    ${body:+-d "$body"} \
                    --max-time 15 \
                    "$url" 2>/dev/null) || r="000 15.0 0"
                echo "$r" >> "$out"
            done
        ) &
        pids+=($!)
    done

    # ── spinner ──────────────────────────────────────────────────────────────────
    local frames=('⠋' '⠙' '⠹' '⠸' '⠼' '⠴' '⠦' '⠧' '⠇' '⠏')
    local fi=0
    while kill -0 "${pids[0]}" 2>/dev/null; do
        local elapsed=$(( $(date +%s) - start_ts ))
        printf "\r  %s ${DIM}%-28s${RST} %ds / %ds" \
            "${frames[$((fi % 10))]}" "$name" "$elapsed" "$DURATION"
        sleep 0.15
        fi=$(( fi + 1 ))
    done
    printf "\r\033[K"
    wait "${pids[@]}" 2>/dev/null || true

    # ── stats via awk ────────────────────────────────────────────────────────────
    # collect all latency values for percentile calculation
    cat "$dir"/w*.dat 2>/dev/null \
        | awk '$1>=200&&$1<400{print $2}' \
        | sort -n > "$dir/lats.txt"

    local stats
    stats=$(cat "$dir"/w*.dat 2>/dev/null | awk -v dur="$DURATION" '
    BEGIN { ok=0; err=0; lat_sum=0; bytes=0; lat_min=9999; lat_max=0 }
    {
        code=$1; lat=$2; sz=$3
        if (code>=200 && code<400) {
            ok++; lat_sum+=lat; bytes+=sz
            if (lat<lat_min) lat_min=lat
            if (lat>lat_max) lat_max=lat
        } else { err++ }
    }
    END {
        rps  = (ok+err) / dur
        avg  = ok>0 ? lat_sum/ok : 0
        mb_s = bytes/dur/1024/1024
        printf "ok=%d err=%d rps=%.2f avg=%.3f min=%.3f max=%.3f mb_s=%.4f\n",
               ok, err, rps, avg, lat_min, lat_max, mb_s
    }')

    local ok_n err_n rps avg_lat min_lat max_lat mb_s
    ok_n=$(echo "$stats"   | grep -oP 'ok=\K[0-9]+')
    err_n=$(echo "$stats"  | grep -oP 'err=\K[0-9]+')
    rps=$(echo "$stats"    | grep -oP 'rps=\K[0-9.]+')
    avg_lat=$(echo "$stats" | grep -oP 'avg=\K[0-9.]+')
    min_lat=$(echo "$stats" | grep -oP 'min=\K[0-9.]+')
    max_lat=$(echo "$stats" | grep -oP 'max=\K[0-9.]+')
    mb_s=$(echo "$stats"   | grep -oP 'mb_s=\K[0-9.]+')

    # percentiles
    local total_ok="$ok_n"
    local p50 p95 p99
    p50=$(awk "NR==int($total_ok*0.50+0.5){print;exit}" "$dir/lats.txt" 2>/dev/null || echo "n/a")
    p95=$(awk "NR==int($total_ok*0.95+0.5){print;exit}" "$dir/lats.txt" 2>/dev/null || echo "n/a")
    p99=$(awk "NR==int($total_ok*0.99+0.5){print;exit}" "$dir/lats.txt" 2>/dev/null || echo "n/a")

    metric "Total requests:"     "$(( ok_n + err_n ))  (${ok_n} ok / ${err_n} errors)"
    metric "Throughput:"         "${rps} req/s"
    metric "Latency avg/p50:"    "${avg_lat}s / ${p50}s"
    metric "Latency p95/p99:"    "${p95}s / ${p99}s"
    metric "Latency min/max:"    "${min_lat}s / ${max_lat}s"
    metric "Response transfer:"  "${mb_s} MB/s"

    SUMMARY_ROWS+=("$name|$rps|$avg_lat|$p95|$mb_s|$err_n")
}

# ── Phase 1 — Search load ─────────────────────────────────────────────────────
section "Phase 1 — Search (POST /api/metadata/search)"
info "Hammering search with empty query, limit=20, $CONCURRENCY workers × ${DURATION}s"
SEARCH_BODY='{"searchQuery":"","limit":20,"offset":0}'
run_phase "Search" "$META_URL/api/metadata/search" POST "$SEARCH_BODY"

# ── Phase 2 — Search count ────────────────────────────────────────────────────
section "Phase 2 — Search Count (POST /api/metadata/search/count)"
info "Testing count aggregation performance, $CONCURRENCY workers × ${DURATION}s"
run_phase "Search Count" "$META_URL/api/metadata/search/count" POST "$SEARCH_BODY"

# ── Phase 3 — Filename listing ────────────────────────────────────────────────
section "Phase 3 — Filename Listing (GET /api/metadata/filenames)"
info "Paginated listing (limit=100), $CONCURRENCY workers × ${DURATION}s"
run_phase "Filenames" "$META_URL/api/metadata/filenames?limit=100&offset=0" GET

# ── Phase 4 — Crawl simulation (metadata ingest) ─────────────────────────────
section "Phase 4 — Crawl Simulation (POST /api/metadata)"
info "Posting synthetic metadata docs with unique digests to measure ingest rate"
info "Each doc represents ~${AVG_FILE_MB} MB → crawl throughput = ingest_rps × ${AVG_FILE_MB} MB/s"

# Build a minimal metadata JSON template; each worker inserts its worker-id
# and a nanosecond timestamp into digestString to ensure uniqueness.
META_TEMPLATE='{
  "digestString":"DIGEST_PLACEHOLDER",
  "fileName":["perf_test_DIGEST_PLACEHOLDER.nc"],
  "filePath":["/perf/test/"],
  "fileType":["nc"],
  "fileSize":524288000,
  "lastModified":1713369600000,
  "domain":1,
  "variables":[],
  "globalAttributes":[]
}'

INGEST_DIR="$TMP_ROOT/ingest"
mkdir -p "$INGEST_DIR"
ingest_end=$(( $(date +%s) + DURATION ))
ingest_start=$(date +%s)

ingest_pids=()
for w in $(seq 1 "$CONCURRENCY"); do
    (
        local_out="$INGEST_DIR/w${w}.dat"
        counter=0
        while [ "$(date +%s)" -lt "$ingest_end" ]; do
            # unique digest: worker + counter + nanoseconds
            dg="perf_w${w}_c${counter}_$(date +%s%N)"
            body="${META_TEMPLATE//DIGEST_PLACEHOLDER/$dg}"
            r=$(curl -s -o /dev/null \
                -w "%{http_code} %{time_total} %{size_upload}" \
                -X POST \
                -H "Authorization: Bearer $TOKEN" \
                -H "Content-Type: application/json" \
                -d "$body" \
                --max-time 15 \
                "$META_URL/api/metadata" 2>/dev/null) || r="000 15.0 0"
            echo "$r" >> "$local_out"
            counter=$(( counter + 1 ))
        done
    ) &
    ingest_pids+=($!)
done

frames=('⠋' '⠙' '⠹' '⠸' '⠼' '⠴' '⠦' '⠧' '⠇' '⠏')
fi=0
while kill -0 "${ingest_pids[0]}" 2>/dev/null; do
    elapsed=$(( $(date +%s) - ingest_start ))
    printf "\r  %s ${DIM}%-28s${RST} %ds / %ds" \
        "${frames[$((fi % 10))]}" "Crawl Simulation" "$elapsed" "$DURATION"
    sleep 0.15
    fi=$(( fi + 1 ))
done
printf "\r\033[K"
wait "${ingest_pids[@]}" 2>/dev/null || true

ingest_stats=$(cat "$INGEST_DIR"/w*.dat 2>/dev/null | awk -v dur="$DURATION" '
BEGIN { ok=0; err=0; lat_sum=0; lat_min=9999; lat_max=0 }
{
    code=$1; lat=$2
    if (code>=200 && code<400) {
        ok++; lat_sum+=lat
        if (lat<lat_min) lat_min=lat
        if (lat>lat_max) lat_max=lat
    } else { err++ }
}
END {
    rps = (ok+err) / dur
    avg = ok>0 ? lat_sum/ok : 0
    printf "ok=%d err=%d rps=%.2f avg=%.3f min=%.3f max=%.3f\n",
           ok, err, rps, avg, lat_min, lat_max
}')

i_ok=$(echo "$ingest_stats"  | grep -oP 'ok=\K[0-9]+')
i_err=$(echo "$ingest_stats" | grep -oP 'err=\K[0-9]+')
i_rps=$(echo "$ingest_stats" | grep -oP 'rps=\K[0-9.]+')
i_avg=$(echo "$ingest_stats" | grep -oP 'avg=\K[0-9.]+')
i_min=$(echo "$ingest_stats" | grep -oP 'min=\K[0-9.]+')
i_max=$(echo "$ingest_stats" | grep -oP 'max=\K[0-9.]+')
# Estimated crawl throughput in MB/s
i_crawl_mb=$(echo "$i_rps $AVG_FILE_MB" | awk '{printf "%.1f", $1 * $2}')

metric "Total inserts:"      "$(( i_ok + i_err ))  (${i_ok} ok / ${i_err} errors)"
metric "Ingest rate:"        "${i_rps} docs/s"
metric "Latency avg:"        "${i_avg}s  (min ${i_min}s / max ${i_max}s)"
metric "Estimated crawl:    " "${i_crawl_mb} MB/s  (at ${AVG_FILE_MB} MB avg file size)"

echo ""
warn "Test data inserted: $(( i_ok )) docs with prefix 'perf_test_*' in fileName."
warn "To clean up (requires ADMIN token):"
echo -e "  ${DIM}curl -s -X POST $META_URL/api/metadata/filenames \\"
echo -e "    -H 'Authorization: Bearer \$TOKEN' \\"
echo -e "    -H 'Content-Type: application/json' \\"
echo -e "    -d '[\"perf_test_*.nc\"]'${RST}"

SUMMARY_ROWS+=("Crawl Sim|$i_rps|$i_avg|n/a|$i_crawl_mb MB/s (est.)|$i_err")

# ── Phase 5 — File download throughput ───────────────────────────────────────
DOWNLOAD_MB_S="skipped"
if [[ "$SKIP_DOWNLOAD" == "true" || -z "$DIGEST" ]]; then
    section "Phase 5 — File Download"
    warn "Skipped (file server unreachable or no DIGEST provided)."
    warn "Re-run with: -D <sha256-digest>  or set DIGEST env var"
    SUMMARY_ROWS+=("File Download|–|–|–|skipped|–")
else
    section "Phase 5 — File Download (GET /api/file/$DIGEST)"
    info "Downloading file with $CONCURRENCY concurrent workers for ${DURATION}s"
    run_phase "File Download" "$FILE_URL/api/file/$DIGEST" GET
    # extract MB/s from last summary row
    DOWNLOAD_MB_S=$(echo "${SUMMARY_ROWS[-1]}" | awk -F'|' '{print $5}')
fi

# ── Summary table ─────────────────────────────────────────────────────────────
section "Summary"
echo ""
printf "  ${BOLD}%-22s  %10s  %9s  %9s  %12s  %7s${RST}\n" \
    "Phase" "req/s" "avg lat" "p95 lat" "MB/s" "errors"
printf "  %s\n" "$(printf '─%.0s' {1..78})"

for row in "${SUMMARY_ROWS[@]}"; do
    IFS='|' read -r ph rps avg p95 mbs errs <<< "$row"
    printf "  %-22s  %10s  %9s  %9s  %12s  %7s\n" \
        "$ph" "$rps" "${avg}s" "${p95}s" "$mbs" "$errs"
done
echo ""

# ── Markdown output ───────────────────────────────────────────────────────────
MD_FILE="${OUTPUT_MD:-$(dirname "$0")/RESULTS.md}"
{
cat <<MDBLOCK

---

## Run: $RUN_DATE

**Config:** ${CONCURRENCY} workers · ${DURATION}s/phase · Meta: \`$META_URL\` · File: \`$FILE_URL\` · Avg file size: ${AVG_FILE_MB} MB

| Phase | req/s | avg latency | p95 latency | MB/s | errors |
|-------|------:|------------:|------------:|-----:|-------:|
MDBLOCK
for row in "${SUMMARY_ROWS[@]}"; do
    IFS='|' read -r ph rps avg p95 mbs errs <<< "$row"
    printf "| %-20s | %s | %ss | %ss | %s | %s |\n" \
        "$ph" "$rps" "$avg" "$p95" "$mbs" "$errs"
done
echo ""
echo "> Crawl MB/s is an estimate: ingest rate × ${AVG_FILE_MB} MB average file size."
echo "> File download MB/s measures actual response body transfer speed."
} >> "$MD_FILE"

echo -e "  ${GRN}Results appended to:${RST} $MD_FILE"
echo ""
