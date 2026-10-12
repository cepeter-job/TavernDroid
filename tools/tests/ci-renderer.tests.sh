#!/usr/bin/env bash
set -eu

root=$(cd "$(dirname "$0")/../.." && pwd)
gate="${RENDERER_GATE_UNDER_TEST:-$root/tools/verify-ci-renderer.sh}"
parent="${RUNNER_TEMP:-$root/build}/renderer-contract-tests"
mkdir -p "$parent"
fixture=$(mktemp -d "$parent/run.XXXXXX")
mkdir -p "$fixture/sdk/platform-tools" "$fixture/bin"
cat > "$fixture/sdk/platform-tools/adb" <<'ADB'
#!/usr/bin/env bash
set -eu
printf '%s\n' "$*" >> "$CASE_DIRECTORY/commands"
case "$*" in
  '-s emulator-5554 shell pidof surfaceflinger; pidof system_server; service check activity')
    count=0
    if [ -f "$CASE_DIRECTORY/samples" ]; then count=$(cat "$CASE_DIRECTORY/samples"); fi
    count=$((count + 1)); printf '%s\n' "$count" > "$CASE_DIRECTORY/samples"
    surface=101; system=202
    if [ "$count" -gt 1 ]; then
      case "$SCENARIO" in
        surface_restart) surface=303 ;;
        system_restart) system=404 ;;
        late_restart) if [ "$count" -ge 8 ]; then system=404; fi ;;
        bad_state) surface='' ;;
      esac
    fi
    printf '%s\r\n%s\r\nService activity: found\r\n' "$surface" "$system"
    if [ "$SCENARIO" = state_adb_error ]; then exit 8; fi ;;
  "-s emulator-5554 shell date '+%m-%d %H:%M:%S.000'") printf '10-12 01:30:00.000\r\n' ;;
  '-s emulator-5554 shell am start -W -a android.settings.SETTINGS')
    printf 'settings\n' > "$CASE_DIRECTORY/foreground"
    if [ "$SCENARIO" = start_timeout ]; then printf 'Status: timeout\r\n'; else printf 'Status: ok\r\n'; fi ;;
  '-s emulator-5554 shell input keyevent KEYCODE_HOME')
    if [ "$SCENARIO" != home_ignored ]; then printf 'home\n' > "$CASE_DIRECTORY/foreground"; fi ;;
  '-s emulator-5554 shell dumpsys activity activities')
    foreground=$(cat "$CASE_DIRECTORY/foreground")
    if [ "$SCENARIO" = settings_not_resumed ] || [ "$foreground" = home ]; then
      printf '  topResumedActivity=ActivityRecord{abc u0 com.google.android.apps.nexuslauncher/.NexusLauncherActivity t1}\r\n'
    else
      printf '  topResumedActivity=ActivityRecord{def u0 com.android.settings/.Settings t2}\r\n'
    fi ;;
  '-s emulator-5554 exec-out screencap -p')
    if [ "$SCENARIO" = screenshot_empty ]; then exit 0; fi
    if [ "$SCENARIO" = screenshot_bad_header ]; then printf 'not a PNG'; exit 0; fi
    printf '\211PNG\r\n\032\nfixture'
    if [ "$SCENARIO" != screenshot_truncated ]; then printf '\000\000\000\000IEND\256B\140\202'; fi
    if [ "$SCENARIO" = screenshot_adb_error ]; then exit 8; fi
    if [ "$SCENARIO" = screenshot_timeout ]; then exit 124; fi ;;
  '-s emulator-5554 logcat -b all -d -v threadtime -T 10-12 01:30:00.000')
    if [ "$SCENARIO" = logcat_error ]; then exit 8; fi
    if [ "$SCENARIO" = native_assertion ]; then
      printf '10-12 01:30:02.000 F mapper.ranchu: Assertion failed: !rcEnc->featureInfo()->hasReadColorBufferDma\n'
    elif [ "$SCENARIO" = native_abort ]; then
      printf '10-12 01:30:02.000 F libc: Fatal signal 6 (SIGABRT), code -1 in tid 2158 (TaskSnapshotPer), pid 202 (system_server)\n'
    else
      printf '10-12 01:30:02.000 I ActivityTaskManager: smoke completed\n'
    fi ;;
  *) printf 'Unexpected device command: %s\n' "$*" >&2; exit 97 ;;
esac
ADB
cat > "$fixture/bin/sleep" <<'SLEEP'
#!/usr/bin/env bash
printf '%s\n' "$1" >> "$CASE_DIRECTORY/waits"
SLEEP
chmod +x "$fixture/sdk/platform-tools/adb" "$fixture/bin/sleep"

failures=0
for scenario in stable unchanged_backend missing_backend malformed_backend surface_restart system_restart late_restart bad_state state_adb_error start_timeout settings_not_resumed home_ignored screenshot_empty screenshot_bad_header screenshot_truncated screenshot_adb_error screenshot_timeout logcat_error native_assertion native_abort; do
  caseDirectory="$fixture/$scenario"
  mkdir -p "$caseDirectory/device-diagnostics"
  if [ "$scenario" = unchanged_backend ]; then
    printf 'INFO | emuglConfig_init: vulkan_mode_selected:swiftshader gles_mode_selected:swiftshader\n' > "$caseDirectory/emulator.log"
  elif [ "$scenario" = missing_backend ]; then
    printf 'INFO | Graphics backend: gfxstream\n' > "$caseDirectory/emulator.log"
  elif [ "$scenario" = malformed_backend ]; then
    printf 'INFO | emuglConfig_init: vulkan_mode_selected:lavapipe gles_mode_selected:\n' > "$caseDirectory/emulator.log"
  else
    printf 'INFO | emuglConfig_init: vulkan_mode_selected:lavapipe gles_mode_selected:angle\n' > "$caseDirectory/emulator.log"
  fi
  set +e
  PATH="$fixture/bin:$PATH" ANDROID_HOME="$fixture/sdk" ANDROID_SERIAL=emulator-5554 RUNNER_TEMP="$caseDirectory" SCENARIO="$scenario" CASE_DIRECTORY="$caseDirectory" \
    bash "$gate" > "$caseDirectory/result.log" 2>&1
  result=$?
  set -e
  if [ "$scenario" = stable ]; then
    waited=0
    if [ -f "$caseDirectory/waits" ]; then waited=$(awk '{ total += $1 } END { print total }' "$caseDirectory/waits"); fi
    screenshots=0
    settingsStarts=0; homeCommands=0
    if [ -f "$caseDirectory/commands" ]; then
      screenshots=$(grep -c 'exec-out screencap -p' "$caseDirectory/commands" || true)
      settingsStarts=$(grep -c 'shell am start -W -a android.settings.SETTINGS' "$caseDirectory/commands" || true)
      homeCommands=$(grep -c 'shell input keyevent KEYCODE_HOME' "$caseDirectory/commands" || true)
    fi
    if [ "$result" -ne 0 ] || [ "$waited" -lt 20 ] || [ "$screenshots" -ne 4 ] || [ "$settingsStarts" -ne 2 ] || [ "$homeCommands" -ne 2 ] || ! grep -q '^PASS:' "$caseDirectory/result.log"; then
      printf 'FAIL: stable renderer did not complete two settled activity/readback cycles.\n' >&2
      cat "$caseDirectory/result.log" >&2
      failures=$((failures + 1))
    fi
  else
    case "$scenario" in
      unchanged_backend) expected='Renderer is unchanged' ;;
      missing_backend|malformed_backend) expected='Selected renderer not captured' ;;
      surface_restart|system_restart|late_restart) expected='Android processes changed' ;;
      bad_state) expected='Invalid Android state' ;;
      state_adb_error) expected='Unable to read Android state' ;;
      start_timeout) expected='Settings did not launch successfully' ;;
      settings_not_resumed|home_ignored) expected='Unexpected resumed activity' ;;
      screenshot_empty|screenshot_bad_header|screenshot_truncated) expected='Incomplete PNG readback' ;;
      screenshot_adb_error|screenshot_timeout) expected='Screenshot command failed' ;;
      logcat_error) expected='Unable to collect smoke logcat' ;;
      native_assertion) expected='Native graphics assertion during smoke' ;;
      native_abort) expected='Native Android process abort during smoke' ;;
    esac
    if [ "$result" -eq 0 ] || ! grep -Fq "$expected" "$caseDirectory/result.log"; then
      printf 'FAIL: %s did not reject the intended failure.\n' "$scenario" >&2
      cat "$caseDirectory/result.log" >&2
      failures=$((failures + 1))
    fi
  fi
done
if [ "$failures" -ne 0 ]; then exit 1; fi
printf 'PASS: twenty renderer smoke contracts, including native crashes, ineffective transitions and incomplete readback.\n'
