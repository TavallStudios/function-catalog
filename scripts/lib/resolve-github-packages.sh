#!/usr/bin/env bash
set -euo pipefail

tavall_resolve_github_packages() {
    if [[ -n "${GITHUB_TOKEN:-}" ]]; then
        export GITHUB_ACTOR="${GITHUB_ACTOR:-github}"
        printf '%s\n' 'GitHub Packages credentials: environment token verified (redacted)'
        return 0
    fi

    local token_file="${GH_TOKEN_FILE:-}"
    if [[ "${token_file}" != '/run/tavall-github-token' \
            || -L "${token_file}" \
            || ! -f "${token_file}" \
            || ! -r "${token_file}" ]]; then
        printf '%s\n' 'GitHub Packages ephemeral token file is missing or unsafe' >&2
        return 1
    fi

    local token_size
    token_size="$(wc -c < "${token_file}")"
    if [[ ! "${token_size}" =~ ^[0-9]+$ || "${token_size}" -lt 1 || "${token_size}" -gt 4096 ]]; then
        printf '%s\n' 'GitHub Packages ephemeral token file has an invalid size' >&2
        return 1
    fi

    local token
    token="$(<"${token_file}")"
    if [[ -z "${token}" || "${token}" == *$'\n'* || "${token}" == *$'\r'* ]]; then
        printf '%s\n' 'GitHub Packages ephemeral token file is empty or malformed' >&2
        return 1
    fi

    export GITHUB_TOKEN="${token}"
    export GITHUB_ACTOR="${GITHUB_ACTOR:-github}"
    printf '%s\n' 'GitHub Packages credentials: ephemeral token file verified (redacted)'
}
