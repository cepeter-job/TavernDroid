#!/usr/bin/env bash
# Collect only from the synthetic CI device; diagnostics must not alter test results.
set -u

mode=$1
directory="$RUNNER_TEMP/device-diagnostics"
adb="$ANDROID_HOME/platform-tools/adb"
serial="${ANDROID_SERIAL:-emulator-5554}"

host_snapshot() {
  date -u '+%Y-%m-%dT%H:%M:%SZ'
  cat /proc/meminfo
  for file in /proc/pressure/memory /proc/pressure/cpu /sys/fs/cgroup/memory.max /sys/fs/cgroup/memory.current /sys/fs/cgroup/memory.events; do
    printf '\n%s\n' "$file"
    cat "$file" 2>&1
  done
  ps -eo pid,ppid,comm,rss,pcpu,pmem --sort=-rss | head -n 21
}

case "$mode" in
  start)
    mkdir -p "$directory" || exit 1
    date -u '+%Y-%m-%d %H:%M:%S UTC' > "$directory/started-at.txt"
    { uname -sr; nproc; free -m; df -h; } > "$directory/host-info.txt" 2>&1
    cat "$ANDROID_HOME/emulator/source.properties" "$ANDROID_HOME/system-images/$IMAGE/google_apis/x86_64/source.properties" > "$directory/sdk-versions.txt" 2>&1
    grep -E '^(hw.ramSize|hw.cpu.ncore|disk.dataPartition.size)=' "$ANDROID_AVD_HOME/p1.avd/config.ini" > "$directory/avd-resources.txt"
    nohup bash "$0" host > "$directory/host-resources.log" 2>&1 < /dev/null &
    echo $! > "$directory/host.pid"
    nohup bash "$0" guest > "$directory/guest-logcat.log" 2> "$directory/guest-collector.log" < /dev/null &
    echo $! > "$directory/guest.pid"
    ;;
  host)
    while true; do
      host_snapshot
      timeout 5 "$adb" -s "$serial" shell 'date; pidof system_server; service check activity; cat /proc/meminfo' 2>&1
      sleep 5
    done
    ;;
  guest)
    while true; do
      date -u '+%Y-%m-%dT%H:%M:%SZ' >&2
      timeout 610 "$adb" -s "$serial" wait-for-device
      "$adb" -s "$serial" logcat -b all -v threadtime
      printf 'logcat exit: %s\n' "$?" >&2
      sleep 2
    done
    ;;
  finish)
    # A dead Android system may block ADB or dumpsys, so every postmortem is bounded.
    host_snapshot > "$directory/host-final.txt" 2>&1
    timeout 10 sudo -n journalctl -k --since "$(cat "$directory/started-at.txt")" --no-pager > "$directory/host-kernel.log" 2>&1
    timeout 10 "$adb" -s "$serial" logcat -b all -d -v threadtime > "$directory/guest-final-logcat.log" 2>&1
    for tag in system_server_crash system_server_watchdog; do
      timeout 10 "$adb" -s "$serial" shell dumpsys dropbox --print "$tag" > "$directory/$tag.log" 2>&1
    done
    for name in host guest; do
      pid=$(cat "$directory/$name.pid")
      if kill -0 "$pid" 2>/dev/null; then
        printf '%s collector %s was alive at cleanup\n' "$name" "$pid" >> "$directory/collector-status.txt"
        kill "$pid" 2>/dev/null
      else
        printf '%s collector %s exited before cleanup\n' "$name" "$pid" >> "$directory/collector-status.txt"
      fi
    done
    ;;
  *) exit 2 ;;
esac
exit 0
