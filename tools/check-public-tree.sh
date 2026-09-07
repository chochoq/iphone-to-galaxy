#!/bin/sh
set -eu

REPO_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$REPO_DIR"

if ! git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  printf '%s\n' 'Run this check after initializing the staging Git repository.' >&2
  exit 1
fi

forbidden_paths='(^|/)(build|build-test|\.local|deps|keystore|memory|state|tmp)(/|$)|\.(apk|aab|aar|jar|jks|keystore|idsig|mp4|mov|webm|log)$'
if git ls-files | grep -E "$forbidden_paths"; then
  printf '%s\n' 'Forbidden generated, private, or vendored file is tracked.' >&2
  exit 1
fi

forbidden_text='(/Users/[^/[:space:]]+|강남|진흥|새벽의|라니|([[:xdigit:]]{2}:){5}[[:xdigit:]]{2})'
if git grep -n -I -E "$forbidden_text" -- . ':!tools/check-public-tree.sh'; then
  printf '%s\n' 'Potential personal identifier or machine-local path is tracked.' >&2
  exit 1
fi

printf '%s\n' 'Public tree checks passed'
