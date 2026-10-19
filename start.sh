#!/bin/bash
if ! pgrep -f "node server.js" > /dev/null; then
  echo "[AutoStart] Starting QR Friend Backend Server..."
  nohup node server.js > server.log 2>&1 &
fi
