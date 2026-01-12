#!/usr/bin/env bash
set -euo pipefail

LOG_FILE="/var/log/eb-hooks.log"

log() {
  local msg="$1"
  echo "[postdeploy-healthcheck] $(date -u +'%Y-%m-%dT%H:%M:%SZ') ${msg}" | tee -a "$LOG_FILE" | logger -t postdeploy-healthcheck || true
}

curl_one() {
  local url="$1"
  local name="$2"

  log "Probing ${name}: ${url}"

  local metrics
  metrics="$(curl -sS -m 5 -o /tmp/eb-healthcheck-body.txt -w 'status=%{http_code} time=%{time_total}s\n' "$url" 2>&1 || true)"

  local body_preview
  body_preview="$(head -c 200 /tmp/eb-healthcheck-body.txt | tr '\n' ' ' | tr -s ' ' || true)"

  log "${name} result: ${metrics//$'\n'/ } body='${body_preview}'"
}

retry() {
  local tries="$1"
  local sleep_s="$2"
  shift 2
  local i=1
  while [ "$i" -le "$tries" ]; do
    "$@" && return 0
    log "Attempt ${i}/${tries} failed; sleeping ${sleep_s}s"
    sleep "$sleep_s"
    i=$((i+1))
  done
  return 0  # never fail the hook
}

# Prefer EB-provided PORT, fallback to 5000
APP_PORT="${PORT:-5000}"

# Give the app/proxy a moment to settle after flip + reload.
sleep 2

retry 6 5 curl_one "http://127.0.0.1/api/actuator/health/liveness" "nginx:/api/actuator/health/liveness"
retry 6 5 curl_one "http://127.0.0.1:${APP_PORT}/api/actuator/health/liveness" "app-direct:${APP_PORT}/api/actuator/health/liveness"

log "Postdeploy health probes complete."
