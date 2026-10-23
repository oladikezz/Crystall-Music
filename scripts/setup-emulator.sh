#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
sdk="$PWD/.android-sdk"
bash scripts/setup-android.sh
if [[ ! -x "$sdk/emulator/emulator" ]]; then
    tmp="$(mktemp -d)"
    curl --fail --location --retry 3 https://dl.google.com/android/repository/emulator-linux_x64-15261927.zip -o "$tmp/emulator.zip"
    printf '%s  %s\n' faec087d7c23d5b7dee18b505f59e0f4e3dc23dfba4c7eb3551e79ade0bb2406 "$tmp/emulator.zip" | sha256sum --check --status
    unzip -q "$tmp/emulator.zip" -d "$sdk"
    rm -rf "$tmp"
fi
if [[ ! -f "$sdk/system-images/android-35/google_apis/x86_64/system.img" ]]; then
    tmp="$(mktemp -d)"
    curl --fail --location --retry 3 https://dl.google.com/android/repository/sys-img/google_apis/x86_64-35_r09.zip -o "$tmp/image.zip"
    printf '%s  %s\n' 0103e6dab21290c4b9d16550a3ce99476f884eef "$tmp/image.zip" | sha1sum --check --status
    mkdir -p "$sdk/system-images/android-35/google_apis"
    unzip -q "$tmp/image.zip" -d "$sdk/system-images/android-35/google_apis"
    rm -rf "$tmp"
fi
# The archive lacks SDK repository metadata required by avdmanager.
python3 - "$sdk" <<'PYSDK'
import pathlib, sys
sdk = pathlib.Path(sys.argv[1])
metadata = '''<?xml version="1.0" encoding="UTF-8"?>
<repo:repository xmlns:repo="http://schemas.android.com/repository/android/common/02"
 xmlns:generic="http://schemas.android.com/repository/android/generic/02"
 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
 <localPackage path="emulator" obsolete="false">
  <type-details xsi:type="generic:genericDetailsType"/>
  <revision><major>36</major><minor>5</minor><micro>11</micro></revision>
  <display-name>Android Emulator 36.5.11</display-name>
 </localPackage>
</repo:repository>
'''
(sdk / "emulator/package.xml").write_text(metadata)
PYSDK
export ANDROID_HOME="$sdk"
printf 'no\n' | "$sdk/cmdline-tools/12.0/bin/avdmanager" create avd --name crystall-api35 --package 'system-images;android-35;google_apis;x86_64' --device pixel_5 --force
