#!/usr/bin/env bash
set -euo pipefail

URL_APP="http://127.0.0.1:5000/api/actuator/health/liveness"
URL_NGINX="http://127.0.0.1/api/actuator/health/liveness"

MAX_SECONDS=180
SLEEP_SECONDS=2

echo "[postdeploy-healthcheck] Waiting for app liveness: $URL_APP (max ${MAX_SECONDS}s)"

start=$(date +%s)
while true; do
  # Try app direct first (most important signal)
  code=$(curl -sS -o /tmp/hc_body.txt -w "%{http_code}" "$URL_APP" || true)

  if [[ "$code" == "200" ]]; then
    echo "[postdeploy-healthcheck] app-direct OK (200)"
    break
  fi

  now=$(date +%s)
  elapsed=$((now - start))
  if (( elapsed >= MAX_SECONDS )); then
    echo "[postdeploy-healthcheck] FAILED after ${elapsed}s. Last status=$code body:"
    cat /tmp/hc_body.txt || true
    exit 1
  fi

  echo "[postdeploy-healthcheck] Not ready yet (status=$code). Retrying in ${SLEEP_SECONDS}s..."
  sleep "$SLEEP_SECONDS"
done

# Optional: also confirm nginx proxy path works once app is up
code_nginx=$(curl -sS -o /tmp/hc_nginx_body.txt -w "%{http_code}" "$URL_NGINX" || true)
echo "[postdeploy-healthcheck] nginx status=$code_nginx"
if [[ "$code_nginx" != "200" ]]; then
  echo "[postdeploy-healthcheck] nginx body:"
  cat /tmp/hc_nginx_body.txt || true
  exit 1
fi

echo "[postdeploy-healthcheck] OK"
