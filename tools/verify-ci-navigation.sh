#!/usr/bin/env bash
set -euo pipefail

adb="$ANDROID_HOME/platform-tools/adb"
serial="${ANDROID_SERIAL:-emulator-5554}"
overlay=com.android.internal.systemui.navbar.threebutton

adb_shell() {
  timeout 10 "$adb" -s "$serial" shell "$@" | tr -d '\r'
}

if ! adb_shell cmd overlay enable-exclusive --user 0 --category "$overlay"; then
  printf 'Failed to enable three-button navigation.\n' >&2
  exit 1
fi
overlays=$(adb_shell cmd overlay list --user 0 "$overlay")
if ! grep -Fxq "[x] $overlay" <<< "$overlays"; then
  printf 'Three-button overlay is not enabled: %s\n' "$overlays" >&2
  exit 1
fi
mode=$(adb_shell cmd overlay lookup --user 0 android android:integer/config_navBarInteractionMode)
if [ "$mode" != 0 ]; then
  printf 'Navigation mode is not 0: %s\n' "$mode" >&2
  exit 1
fi

read_android_state() {
  local state
  if ! state=$(adb_shell 'pidof surfaceflinger; pidof system_server; service check activity'); then
    printf 'Unable to read Android state.\n' >&2
    return 1
  fi
  local -a fields
  mapfile -t fields <<< "$state"
  if [[ ${#fields[@]} -ne 3 ]] ||
     [[ ! ${fields[0]} =~ ^[1-9][0-9]*$ || ! ${fields[1]} =~ ^[1-9][0-9]*$ || ${fields[2]} != 'Service activity: found' ]]; then
    printf 'Invalid Android state: %s\n' "$state" >&2
    return 1
  fi
  pids="${fields[0]} ${fields[1]}"
}

# SurfaceFlinger restarts also restart Zygote; boot_completed alone misses this.
read_android_state
baseline="$pids"
started=$SECONDS
date -u '+Navigation observation started: %Y-%m-%dT%H:%M:%SZ'
printf 'Three-button mode=0; baseline surfaceflinger/system_server PIDs: %s\n' "$baseline"
for ((sample = 1; sample <= 24; sample++)); do
  sleep 5
  read_android_state
  if [ "$pids" != "$baseline" ]; then
    printf 'Android processes changed: baseline=%s current=%s\n' "$baseline" "$pids" >&2
    exit 1
  fi
  printf 'Observation %s/24: PIDs=%s; activity service found.\n' "$sample" "$pids"
done
date -u '+Navigation observation finished: %Y-%m-%dT%H:%M:%SZ'
printf 'PASS: Android processes and activity service stayed stable across 120 seconds of waits (%ss elapsed).\n' "$((SECONDS - started))"
