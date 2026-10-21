#!/usr/bin/env bash
set -euo pipefail
ING=http://localhost:8081
QRY=http://localhost:8082

function wait_up() {
  local base_url=$1
  local name=$2
  local health_url="${base_url%/}/actuator/health"
  local attempts=0
  until curl -s "$health_url" | grep -q '"status":"UP"'; do
    if (( attempts >= 45 )); then
      echo "FAIL: $name did not become healthy at $health_url within 90 seconds."
      exit 1
    fi
    ((attempts += 1))
    sleep 2
  done
  echo "PASS $name ready"
}

wait_up "$ING" "ING"
wait_up "$QRY" "QRY"

function ts() {
  date -u -d "$1 hours ago" +%Y-%m-%dT%H:%M:%SZ 2>/dev/null \
    || date -u -v-"$1"H +%Y-%m-%dT%H:%M:%SZ
}

function post() {
  local body=$1
  curl -s -o /dev/null -w "%{http_code}" \
    -H 'Content-Type: application/json' \
    -d "$body" "$ING/api/v1/readings"
}

function check() {
  local name=$1
  local expected=$2
  local actual=$3
  if [[ "$expected" == "$actual" ]]; then
    echo "PASS $name"
  else
    echo "FAIL $name: expected $expected, got $actual"
    exit 1
  fi
}

# Smoke test: ingest some readings and query stats
# each run has its own sensorId thus its own data set,
# so we can run this multiple times without interference
RUN=$(date +%s)
SENSOR_A="smoke-${RUN}-a"
SENSOR_B="smoke-${RUN}-b"
# T2 is computed once and used by both the 2h post and the duplicate post
T2=$(ts 2)

# Ingest: sensor a temperature 10, outside the default 24h range, inside 72h
check "sensor a temperature 50h ago" 201 \
    "$(post "{\"sensorId\":\"$SENSOR_A\",\"timestamp\":\"$(ts 50)\",\"metrics\":{\"temperature\":10}}")"

# Ingest: sensor a temperature 20, outside the default 24h range, inside 72h
check "sensor a temperature 30h ago" 201 \
    "$(post "{\"sensorId\":\"$SENSOR_A\",\"timestamp\":\"$(ts 30)\",\"metrics\":{\"temperature\":20}}")"

# Ingest: sensor a temperature 30, the only reading inside the default 24h range
check "sensor a temperature 2h ago" 201 \
    "$(post "{\"sensorId\":\"$SENSOR_A\",\"timestamp\":\"$T2\",\"metrics\":{\"temperature\":30}}")"

# Ingest: a second metric for sensor a, so the temperature queries must filter by metric
check "sensor a humidity 30h ago" 201 \
    "$(post "{\"sensorId\":\"$SENSOR_A\",\"timestamp\":\"$(ts 30)\",\"metrics\":{\"humidity\":50}}")"

# Ingest: a second sensor with temperature 40, for the multi-sensor query
check "sensor b temperature 30h ago" 201 \
    "$(post "{\"sensorId\":\"$SENSOR_B\",\"timestamp\":\"$(ts 30)\",\"metrics\":{\"temperature\":40}}")"

# Duplicate (same sensor, metric, timestamp) is accepted with 201 but not stored again
check "duplicate sensor a temperature 2h ago" 201 \
    "$(post "{\"sensorId\":\"$SENSOR_A\",\"timestamp\":\"$T2\",\"metrics\":{\"temperature\":30}}")"

# Invalid body (empty sensorId, no metrics, no timestamp) returns 400
check "bad payload" 400 \
  "$(post '{"sensorId":"","metrics":{}}')"

# Unknown stat is rejected with 400
check "median stat" 400 \
  "$(curl -s -o /dev/null -w "%{http_code}" "$QRY/api/v1/stats?sensorIds=$SENSOR_A&metrics=temperature&stat=median")"

# from 5h ago gives a range under 24h, rejected with 400
check "range under 24 hours" 400 \
  "$(curl -s -o /dev/null -w "%{http_code}" "$QRY/api/v1/stats?sensorIds=$SENSOR_A&metrics=temperature&from=$(ts 5)")"

# Required metrics parameter missing returns 400
check "missing metrics" 400 \
  "$(curl -s -o /dev/null -w "%{http_code}" "$QRY/api/v1/stats?sensorIds=$SENSOR_A")"

# avg of 10, 20, 30 is 20
check "avg temp a from 72h ago" true \
  "$(curl -s "$QRY/api/v1/stats?sensorIds=$SENSOR_A&metrics=temperature&stat=avg&from=$(ts 72)" |
  jq '.results[0].value == 20')"

# min of 10, 20, 30 is 10
check "min temp a" true \
  "$(curl -s "$QRY/api/v1/stats?sensorIds=$SENSOR_A&metrics=temperature&stat=min&from=$(ts 72)" |
  jq '.results[0].value == 10')"

# max of 10, 20, 30 is 30
check "max temp a" true \
  "$(curl -s "$QRY/api/v1/stats?sensorIds=$SENSOR_A&metrics=temperature&stat=max&from=$(ts 72)" |
  jq '.results[0].value == 30')"

# No from/to: only the 2h reading is in the last 24h, so avg is 30
check "avg temp a default range" true \
  "$(curl -s "$QRY/api/v1/stats?sensorIds=$SENSOR_A&metrics=temperature&stat=avg" |
  jq '.results[0].value == 30')"

# sum of 10, 20, 30 is 60; 90 would mean the duplicate was stored
check "sum temp a" true \
  "$(curl -s "$QRY/api/v1/stats?sensorIds=$SENSOR_A&metrics=temperature&stat=sum&from=$(ts 72)" |
  jq '.results[0].value == 60')"

# Two sensors requested: two result rows (one per sensor)
check "max temp both sensors count" true \
  "$(curl -s "$QRY/api/v1/stats?sensorIds=$SENSOR_A,$SENSOR_B&metrics=temperature&stat=max&from=$(ts 72)" |
  jq '.results | length == 2')"

# Rows are ordered by sensor id, so the second row is sensor b with max 40
check "max temp sensor b" true \
  "$(curl -s "$QRY/api/v1/stats?sensorIds=$SENSOR_A,$SENSOR_B&metrics=temperature&stat=max&from=$(ts 72)" |
  jq '.results[1].value == 40')"

echo "PASS all checks"
