#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
sdk="$PWD/.android-sdk"
mkdir -p "$sdk"
install_archive() {
    local name="$1" checksum="$2" dest="$3" staging
    staging="$(mktemp -d)"
    curl --fail --location --retry 3 "https://dl.google.com/android/repository/$name" -o "$staging/sdk.zip"
    printf '%s  %s\n' "$checksum" "$staging/sdk.zip" | sha1sum --check --status
    unzip -q "$staging/sdk.zip" -d "$staging/unpacked"
    mkdir -p "$(dirname "$dest")"
    mv "$staging/unpacked/"* "$dest"
    rm -rf "$staging"
}
if [[ ! -x "$sdk/cmdline-tools/12.0/bin/sdkmanager" ]]; then
    install_archive commandlinetools-linux-11076708_latest.zip d313adb7aedccf6cf0cfca51ec180f0059f5f8f8 "$sdk/cmdline-tools/12.0"
fi
if [[ ! -f "$sdk/platforms/android-35/android.jar" ]]; then
    install_archive platform-35_r02.zip 0bb560a90a7a2cbd0dd8348224d518b638fe7949 "$sdk/platforms/android-35"
fi
if [[ ! -x "$sdk/build-tools/35.0.0/aapt2" ]]; then
    install_archive build-tools_r35_linux.zip 2cfaa0bbb2336e9ec18ed3ecea84fa2e2af607bc "$sdk/build-tools/35.0.0"
fi
# Licenses are accepted only when explicitly requested by the operator.
if [[ "${ACCEPT_ANDROID_LICENSES:-}" == 1 ]]; then
    set +o pipefail
    yes | "$sdk/cmdline-tools/12.0/bin/sdkmanager" --sdk_root="$sdk" --licenses >/dev/null
    set -o pipefail
fi
printf 'sdk.dir=%s\n' "$sdk" > local.properties
