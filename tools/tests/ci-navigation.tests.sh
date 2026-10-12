#!/usr/bin/env bash
set -eu

root=$(cd "$(dirname "$0")/../.." && pwd)
gate="${NAVIGATION_GATE_UNDER_TEST:-$root/tools/verify-ci-navigation.sh}"
parent="${RUNNER_TEMP:-$root/build}/navigation-contract-tests"
mkdir -p "$parent"
fixture=$(mktemp -d "$parent/run.XXXXXX")
mkdir -p "$fixture/sdk/platform-tools" "$fixture/bin"
# Substitute only the external device and clock wait; run the real gate logic.
cat > "$fixture/sdk/platform-tools/adb" <<'ADB'
#!/usr/bin/env bash
set -eu
case "$*" in
  '-s emulator-5554 shell cmd overlay enable-exclusive --user 0 --category com.android.internal.systemui.navbar.threebutton')
    [ "$SCENARIO" != overlay_rejected ] || exit 4 ;;
  '-s emulator-5554 shell cmd overlay list --user 0 com.android.internal.systemui.navbar.threebutton')
    printf 'android\n'
    if [ "$SCENARIO" = overlay_inactive ]; then
      printf '[ ] com.android.internal.systemui.navbar.threebutton\r\n'
    else
      printf '[x] com.android.internal.systemui.navbar.threebutton\r\n'
    fi ;;
  '-s emulator-5554 shell cmd overlay lookup --user 0 android android:integer/config_navBarInteractionMode')
    if [ "$SCENARIO" = wrong_mode ]; then printf '2\r\n'; else printf '0\r\n'; fi ;;
  '-s emulator-5554 shell pidof surfaceflinger; pidof system_server; service check activity')
    count=0
    if [ -f "$CASE_DIRECTORY/samples" ]; then count=$(cat "$CASE_DIRECTORY/samples"); fi
    count=$((count + 1))
    printf '%s\n' "$count" > "$CASE_DIRECTORY/samples"
    surface=101; system=202; activity='Service activity: found'
    if [ "$count" -eq 1 ]; then
      case "$SCENARIO" in
        empty_baseline) surface='' ;;
        missing_baseline) printf '%s\r\n%s\r\n' "$system" "$activity"; exit 0 ;;
      esac
    fi
    if [ "$count" -eq 2 ]; then
      case "$SCENARIO" in
        surface_restart) surface=303 ;;
        system_restart) system=404 ;;
        invalid_pid) surface='101 303' ;;
        missing_activity) activity='Service activity: not found' ;;
        adb_timeout) exit 124 ;;
      esac
    fi
    printf '%s\r\n%s\r\n%s\r\n' "$surface" "$system" "$activity"
    if [ "$SCENARIO" = adb_error_with_valid_state ] && [ "$count" -eq 2 ]; then exit 8; fi ;;
  *) printf 'Unexpected device command: %s\n' "$*" >&2; exit 97 ;;
esac
ADB
cat > "$fixture/bin/sleep" <<'SLEEP'
#!/usr/bin/env bash
printf '%s\n' "$1" >> "$CASE_DIRECTORY/waits"
SLEEP
chmod +x "$fixture/sdk/platform-tools/adb" "$fixture/bin/sleep"

failures=0
for scenario in stable overlay_rejected overlay_inactive wrong_mode surface_restart system_restart invalid_pid missing_activity adb_timeout empty_baseline missing_baseline adb_error_with_valid_state; do
  caseDirectory="$fixture/$scenario"
  mkdir -p "$caseDirectory"
  set +e
  PATH="$fixture/bin:$PATH" ANDROID_HOME="$fixture/sdk" ANDROID_SERIAL=emulator-5554 SCENARIO="$scenario" CASE_DIRECTORY="$caseDirectory" \
    bash "$gate" > "$caseDirectory/result.log" 2>&1
  result=$?
  set -e
  if [ "$scenario" = stable ]; then
    waited=0
    if [ -f "$caseDirectory/waits" ]; then waited=$(awk '{ total += $1 } END { print total }' "$caseDirectory/waits"); fi
    samples=0
    if [ -f "$caseDirectory/samples" ]; then samples=$(cat "$caseDirectory/samples"); fi
    if [ "$result" -ne 0 ] || [ "$waited" -lt 120 ] || [ "$samples" -lt 25 ] || ! grep -q '^PASS:' "$caseDirectory/result.log"; then
      printf 'FAIL: stable device was not observed throughout 120 seconds.\n' >&2
      failures=$((failures + 1))
    fi
  else
    case "$scenario" in
      overlay_rejected) expected='Failed to enable three-button navigation' ;;
      overlay_inactive) expected='Three-button overlay is not enabled' ;;
      wrong_mode) expected='Navigation mode is not 0' ;;
      surface_restart|system_restart) expected='Android processes changed' ;;
      invalid_pid|missing_activity|empty_baseline|missing_baseline) expected='Invalid Android state' ;;
      adb_timeout|adb_error_with_valid_state) expected='Unable to read Android state' ;;
    esac
    if [ "$result" -eq 0 ] || ! grep -Fq "$expected" "$caseDirectory/result.log"; then
      printf 'FAIL: %s did not reject the intended failure.\n' "$scenario" >&2
      cat "$caseDirectory/result.log" >&2
      failures=$((failures + 1))
    fi
  fi
done
if [ "$failures" -ne 0 ]; then exit 1; fi
printf 'PASS: twelve navigation gate contracts, including restarts, unavailable services, failed ADB and ineffective overlays.\n'
