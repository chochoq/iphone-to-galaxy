#!/bin/sh
set -eu

REPO_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$REPO_DIR"

check_sha256() {
  expected=$1
  file=$2
  actual=$(shasum -a 256 "$file" | awk '{print $1}')
  if [ "$actual" != "$expected" ]; then
    printf 'License text checksum mismatch: %s\n' "$file" >&2
    exit 1
  fi
}

check_sha256 3972dc9744f6499f0f9b2dbf76696f2ae7ad8af9b23dde66d6af86c9dfb36986 LICENSE
check_sha256 28a9529c7d0bb4dc51f4bf5c116a3d16ef247a052f7591466768ddf563fd1cf5 LICENSES/CC-BY-SA-4.0.txt
check_sha256 9ba9550ad48438d0836ddab3da480b3b69ffa0aac7b7878b5a0039e7ab429411 LICENSES/CC-BY-4.0.txt
check_sha256 cfc7749b96f63bd31c3c42b5c471bf756814053e847c10f3eb003417bc523d30 LICENSES/Apache-2.0.txt

cmp LICENSES/Apache-2.0.txt apps/airpods-glance/res/raw/apache_license_2_0.txt
cmp LICENSES/CC-BY-4.0.txt apps/airpods-glance/res/raw/cc_by_4_0_license.txt

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
