#!/usr/bin/env bash
set -euo pipefail
adb="$ANDROID_HOME/platform-tools/adb"
serial="${ANDROID_SERIAL:-emulator-5554}"
directory="$RUNNER_TEMP/device-diagnostics"
mkdir -p "$directory"

selected=$(grep -m 1 'emuglConfig_init: vulkan_mode_selected:' "$RUNNER_TEMP/emulator.log" || true)
backend_pattern='vulkan_mode_selected:([[:alnum:]_]+) gles_mode_selected:([[:alnum:]_]+)($|[[:space:]])'
if [[ ! $selected =~ $backend_pattern ]]; then
  printf 'Selected renderer not captured: %s\n' "$selected" >&2
  exit 1
fi
vulkan="${BASH_REMATCH[1]}"
gles="${BASH_REMATCH[2]}"
printf 'Selected Vulkan=%s GLES=%s\n' "$vulkan" "$gles" | tee "$directory/renderer-selected.txt"
if [ "$vulkan" = swiftshader ] && [ "$gles" = swiftshader ]; then
  printf 'Renderer is unchanged; software selection did not test a different backend.\n' >&2
  exit 1
fi

adb_shell() {
  timeout 20 "$adb" -s "$serial" shell "$@" | tr -d '\r'
}

read_android_state() {
  local state
  if ! state=$(adb_shell 'pidof surfaceflinger; pidof system_server; service check activity'); then
    printf 'Unable to read Android state.\n' >&2
    exit 1
  fi
  local -a fields
  mapfile -t fields <<< "$state"
  if [[ ${#fields[@]} -ne 3 ]] ||
     [[ ! ${fields[0]} =~ ^[1-9][0-9]*$ || ! ${fields[1]} =~ ^[1-9][0-9]*$ || ${fields[2]} != 'Service activity: found' ]]; then
    printf 'Invalid Android state: %s\n' "$state" >&2
    exit 1
  fi
  pids="${fields[0]} ${fields[1]}"
  if [ -n "${baseline:-}" ] && [ "$pids" != "$baseline" ]; then
    printf 'Android processes changed: baseline=%s current=%s\n' "$baseline" "$pids" >&2
    exit 1
  fi
}

assert_resumed() {
  local activity
  activity=$(adb_shell dumpsys activity activities)
  printf '%s\n' "$activity" > "$directory/$2-activity.txt"
  if ! grep -Eq "^[[:space:]]*topResumedActivity=.* $1" <<< "$activity"; then
    printf 'Unexpected resumed activity; expected %s\n' "$1" >&2
    exit 1
  fi
}

capture_png() {
  local output="$directory/$1.png"
  if ! timeout 20 "$adb" -s "$serial" exec-out screencap -p > "$output" 2> "$directory/$1-screencap.log"; then
    printf 'Screenshot command failed: %s\n' "$1" >&2
    cat "$directory/$1-screencap.log" >&2
    exit 1
  fi
  local header footer
  header=$(od -An -tx1 -N8 "$output" | tr -d ' \n')
  footer=$(tail -c12 "$output" | od -An -tx1 | tr -d ' \n')
  # A PNG signature alone can accept an interrupted graphics readback.
  if [ "$(stat -c %s "$output")" -le 20 ] || [ "$header" != 89504e470d0a1a0a ] || [ "$footer" != 0000000049454e44ae426082 ]; then
    printf 'Incomplete PNG readback: %s\n' "$1" >&2
    exit 1
  fi
  sha256sum "$output"
  read_android_state
}

# Keep startup crashes in the diagnostic history, but scan only this smoke window.
started=$(adb_shell "date '+%m-%d %H:%M:%S.000'")
timestamp_pattern='^[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2}\.000$'
if [[ ! $started =~ $timestamp_pattern ]]; then
  printf 'Invalid guest timestamp: %s\n' "$started" >&2
  exit 1
fi
read_android_state
baseline="$pids"
printf 'Renderer smoke started: %s; PIDs=%s\n' "$started" "$baseline"
for cycle in 1 2; do
  if ! launch=$(adb_shell am start -W -a android.settings.SETTINGS); then
    printf 'Settings did not launch successfully.\n' >&2
    exit 1
  fi
  printf '%s\n' "$launch" > "$directory/settings-$cycle-launch.txt"
  if ! grep -Fxq 'Status: ok' <<< "$launch"; then
    printf 'Settings did not launch successfully: %s\n' "$launch" >&2
    exit 1
  fi
  assert_resumed 'com\.android\.settings/' "settings-$cycle"
  read_android_state
  capture_png "settings-$cycle"
  adb_shell input keyevent KEYCODE_HOME
  # Task snapshot persistence is asynchronous; observe after the HOME transition.
  for sample in 1 2 3 4 5; do
    sleep 2
    read_android_state
    assert_resumed '(com\.google\.android\.apps\.nexuslauncher|com\.android\.launcher3)/' "home-$cycle-$sample"
    printf 'Cycle %s settle %s/5: PIDs=%s; launcher resumed.\n' "$cycle" "$sample" "$pids"
  done
  capture_png "home-$cycle"
done
if ! timeout 20 "$adb" -s "$serial" logcat -b all -d -v threadtime -T "$started" > "$directory/renderer-readback-logcat.log" 2>&1; then
  printf 'Unable to collect smoke logcat.\n' >&2
  exit 1
fi
if grep -Fq 'Assertion failed: !rcEnc->featureInfo()->hasReadColorBufferDma' "$directory/renderer-readback-logcat.log"; then
  printf 'Native graphics assertion during smoke.\n' >&2
  exit 1
fi
if grep -Eq 'Fatal signal .*pid [0-9]+ \((surfaceflinger|system_server)\)' "$directory/renderer-readback-logcat.log"; then
  printf 'Native Android process abort during smoke.\n' >&2
  exit 1
fi
read_android_state
date -u '+Renderer smoke finished: %Y-%m-%dT%H:%M:%SZ'
printf 'PASS: four complete PNG readbacks and two Settings/HOME cycles, with stable Android processes across 20 seconds of settling waits.\n'
