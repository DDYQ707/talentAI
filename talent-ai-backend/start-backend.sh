#!/bin/bash
ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"

# 按需填写 AI Key，不用 AI 可留空
export DASHSCOPE_API_KEY="${DASHSCOPE_API_KEY:-}"

mkdir -p logs

start() {
  local module=$1
  echo "Starting $module ..."
  mvn -pl "$module" spring-boot:run > "logs/${module}.log" 2>&1 &
  echo $! > "logs/${module}.pid"
}

start talent-gateway
sleep 5   # 等网关先起来
start talent-auth
start talent-job
start talent-resume
start talent-ai-agent
start talent-interview
start talent-analytics

echo "All 7 services starting. Logs in talent-ai-backend/logs/"
echo "Stop:    ./stop-backend.sh"
echo "Restart: ./restart-backend.sh"
