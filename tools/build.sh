#!/usr/bin/env bash
# Runs the tests and builds the APK. Output: app/build/outputs/apk/release/app-release.apk
set -euo pipefail
cd "$(dirname "$0")/.."
./gradlew :app:testDebugUnitTest :app:assembleRelease \
  -Drobolectric.dependency.repo.url=https://maven-central.storage-download.googleapis.com/maven2 "$@"
ls -la app/build/outputs/apk/release/app-release.apk
