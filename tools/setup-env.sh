#!/usr/bin/env bash
# One-time setup of a fresh Linux machine or cloud session so ./gradlew can build the app.
# Installs the Android SDK if it is missing and points the project at it.
set -euo pipefail
root="$(cd "$(dirname "$0")/.." && pwd)"
sdk="${ANDROID_HOME:-/opt/android-sdk}"
if [ ! -d "$sdk/platforms/android-36" ] || [ ! -d "$sdk/build-tools/36.0.0" ]; then
  mkdir -p "$sdk/cmdline-tools"
  if [ ! -x "$sdk/cmdline-tools/latest/bin/sdkmanager" ]; then
    curl -fsS -o /tmp/clt.zip https://dl.google.com/android/repository/commandlinetools-linux-13114758_latest.zip
    unzip -q -o /tmp/clt.zip -d "$sdk/cmdline-tools" && rm /tmp/clt.zip
    rm -rf "$sdk/cmdline-tools/latest" && mv "$sdk/cmdline-tools/cmdline-tools" "$sdk/cmdline-tools/latest"
  fi
  proxy=()
  if [[ "${HTTPS_PROXY:-}" =~ ^https?://([^:/]+):([0-9]+) ]]; then
    proxy=(--proxy=http "--proxy_host=${BASH_REMATCH[1]}" "--proxy_port=${BASH_REMATCH[2]}")
  fi
  yes | "$sdk/cmdline-tools/latest/bin/sdkmanager" "${proxy[@]}" --licenses >/dev/null || true
  "$sdk/cmdline-tools/latest/bin/sdkmanager" "${proxy[@]}" "platforms;android-36" "build-tools;36.0.0" "platform-tools"
fi
echo "sdk.dir=$sdk" > "$root/local.properties"

# Maven Central sometimes answers 429 (rate limit) to cloud machines; Google's mirror of it does not.
mkdir -p ~/.gradle/init.d
cat > ~/.gradle/init.d/youtop-mirror.gradle <<'GRADLE'
def mirror = "https://maven-central.storage-download.googleapis.com/maven2"
beforeSettings { settings ->
    settings.pluginManagement.repositories { google(); maven { url mirror } }
    settings.dependencyResolutionManagement.repositories { google(); maven { url mirror } }
}
GRADLE
echo "Ready. Build with: tools/build.sh"
