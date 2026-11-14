#!/usr/bin/env bash
# Restores the release signing key from the private file kept in Google Drive
# ("YouTop private/youtop-signing.txt"). Usage: tools/setup-signing.sh <path to that file>
set -euo pipefail
src="${1:?path to youtop-signing.txt}"
root="$(cd "$(dirname "$0")/.." && pwd)"
mkdir -p "$root/keystore"
grep -E '^(storePassword|keyPassword|keyAlias)=' "$src" > "$root/keystore/signing.properties"
grep -E '^keystoreBase64=' "$src" | cut -d= -f2- | base64 -d > "$root/keystore/youtop-release.jks"
echo "Signing key restored to $root/keystore (git-ignored)."
