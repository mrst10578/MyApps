#!/usr/bin/env bash
set -euo pipefail
if ! command -v gradle >/dev/null; then
  echo 'Gradle 8.10.2 and Android SDK platform 35 are required. See README.md.' >&2
  exit 2
fi
gradle --no-daemon :app:assembleDebug
printf 'APK: app/build/outputs/apk/debug/app-debug.apk\n'
