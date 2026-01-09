#!/usr/bin/env bash
set -euo pipefail

LOG_FILE="/var/log/eb-hooks.log"

log() {
  # Log to both eb-hooks.log and systemd journal (shows up in /var/log/messages)
  local msg="$1"
  echo "[postdeploy-healthcheck] $(date -u +'%Y-%m-%dT%H:%M:%SZ') ${msg}" | tee -a "$LOG_FILE" | logger -t postdeploy-healthcheck || true
}

curl_one() {
  local url="$1"
  local name="$2"

  log "Probing ${name}: ${url}"

  # -sS: silent but show errors
  # -m: max time seconds
  # -o: discard body (we log a short snippet separately)
  # -w: print status code + timing
  local metrics
  metrics="$(curl -sS -m 5 -o /tmp/eb-healthcheck-body.txt -w 'status=%{http_code} time=%{time_total}s\n' "$url" 2>&1 || true)"

  # Log metrics and a tiny body preview (helps distinguish proxy vs app responses)
  local body_preview
  body_preview="$(head -c 200 /tmp/eb-healthcheck-body.txt | tr '\n' ' ' | tr -s ' ' || true)"

  log "${name} result: ${metrics//$'\n'/ } body='${body_preview}'"
}

# Give the app/proxy a moment to settle after flip + reload.
sleep 2

curl_one "http://127.0.0.1/api/actuator/health" "nginx:/api/actuator/health"
curl_one "http://127.0.0.1:5000/api/actuator/health" "app-direct:/api/actuator/health"

log "Postdeploy health probes complete."
