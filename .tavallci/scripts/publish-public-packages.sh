#!/usr/bin/env bash
set -euo pipefail

repository_root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd -P)"
cd "${repository_root}"

version="$(tr -d '[:space:]' < gradle/release-version.txt)"
if [[ ! "${version}" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    printf 'Invalid locked release version: %s\n' "${version}" >&2
    exit 64
fi

exec ./gradlew --no-daemon --max-workers=1 \
    -PtavallVersion="${version}" \
    clean check publish
