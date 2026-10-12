#!/usr/bin/env bash
set -eu

# Exercise the workflow's real EXIT handler with failing diagnostic commands.
root=$(cd "$(dirname "$0")/../.." && pwd)
parent="${RUNNER_TEMP:-$root/build}/diagnostic-cleanup-tests"
mkdir -p "$parent"
fixture=$(mktemp -d "$parent/run.XXXXXX")
mkdir -p "$fixture/tools" "$fixture/bin"
awk '/^          cleanup\(\) \{/ { capture=1 } capture { sub(/^          /, ""); print } capture && /^}/ { exit }' "$root/.github/workflows/android.yml" > "$fixture/cleanup.sh"
test -s "$fixture/cleanup.sh"
cat > "$fixture/tools/ci-device-diagnostics.sh" <<'STUB'
printf '%s\n' "$1" >> diagnostics-called.txt
exit 9
STUB
cat > "$fixture/bin/adb" <<'STUB'
#!/usr/bin/env bash
printf '%s\n' "$*" >> adb-called.txt
exit 8
STUB
chmod +x "$fixture/bin/adb"

for capture in true false; do
  for expected in 0 7; do
    set +e
    (
      cd "$fixture"
      export PATH="$fixture/bin:$PATH" CAPTURE_DIAGNOSTICS="$capture"
      bash -e -c 'source cleanup.sh; trap cleanup EXIT; exit "$1"' _ "$expected"
    )
    actual=$?
    set -e
    if [ "$actual" -ne "$expected" ]; then
      printf 'Cleanup changed exit %s to %s (capture=%s)\n' "$expected" "$actual" "$capture" >&2
      exit 1
    fi
  done
done
test "$(grep -c '^finish$' "$fixture/diagnostics-called.txt")" -eq 2
test "$(grep -c '^-s emulator-5554 emu kill$' "$fixture/adb-called.txt")" -eq 4
printf 'PASS: diagnostic and emulator cleanup failures preserve workload exits 0 and 7.\n'
