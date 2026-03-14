#!/usr/bin/env bash
set -euo pipefail

REPO="SJSU-CS-systems-group/wildstore"
REGISTRY="ghcr.io/${REPO,,}"
MARKER_FILE="$(dirname "$0")/.current-release"
COMPOSE_FILE="$(dirname "$0")/docker-compose.prod.yml"

META_IMAGE="${REGISTRY}/wildstore-meta"
FILESERVE_IMAGE="${REGISTRY}/wildstore-fileserve"

# Fetch latest non-prerelease tag
LATEST=$(curl -sf "https://api.github.com/repos/${REPO}/releases/latest" | grep '"tag_name"' | cut -d '"' -f 4)

if [ -z "$LATEST" ]; then
  echo "No release found"
  exit 0
fi

# Compare with current deployment
CURRENT=""
if [ -f "$MARKER_FILE" ]; then
  CURRENT=$(cat "$MARKER_FILE")
fi

if [ "$LATEST" = "$CURRENT" ]; then
  echo "Already on ${LATEST}, nothing to do"
  exit 0
fi

echo "New release detected: ${LATEST} (current: ${CURRENT:-none})"

# Pull and tag images for deployment
for IMAGE in "$META_IMAGE" "$FILESERVE_IMAGE"; do
  docker pull "${IMAGE}:${LATEST}"
  docker tag  "${IMAGE}:${LATEST}" "${IMAGE}:deploy"
done

# Restart services
docker compose -f "$COMPOSE_FILE" down
docker compose -f "$COMPOSE_FILE" up -d

# Health check
echo "Waiting for services to become healthy..."
HEALTHY=false
for i in $(seq 1 30); do
  META_OK=$(curl -sf http://localhost:8080/actuator/health || true)
  FS_OK=$(curl -sf http://localhost:27778/actuator/health || true)
  if echo "$META_OK" | grep -q '"UP"' && echo "$FS_OK" | grep -q '"UP"'; then
    HEALTHY=true
    break
  fi
  echo "  attempt $i/30..."
  sleep 2
done

if [ "$HEALTHY" = true ]; then
  echo "$LATEST" > "$MARKER_FILE"
  echo "Deployed ${LATEST} successfully"
else
  echo "Health check failed – not updating marker. Will retry next run."
  docker compose -f "$COMPOSE_FILE" logs --tail=50
  exit 1
fi
