#!/usr/bin/env bash
# Blow 개발 환경 설치 (macOS, Apple Silicon)
#
#   bash scripts/setup-android.sh base   # 1단계: Android SDK + Gradle Wrapper
#   (Android Studio 에서 Pleos 이미지 설치 — README 참고)
#   bash scripts/setup-android.sh avd    # 3단계: Pleos 에뮬레이터 기기(AVD) 생성
#
# 다운로드 출처: dl.google.com (Android SDK), services.gradle.org (Gradle)
set -euo pipefail

export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export ANDROID_HOME="$HOME/Library/Android/sdk"
SDKMANAGER="$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager"
AVDMANAGER="$ANDROID_HOME/cmdline-tools/latest/bin/avdmanager"
PLEOS_IMAGE="system-images;android-34;pleos_car_emulator;arm64-v8a"
AVD_NAME="Pleos_Connect_2.1.0"
BLOW_DIR="$(cd "$(dirname "$0")/.." && pwd)"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

base() {
  if [[ ! -x "$SDKMANAGER" ]]; then
    echo "▶ Android command-line tools 설치"
    mkdir -p "$ANDROID_HOME/cmdline-tools"
    curl -fSL -o "$TMP/cmdtools.zip" \
      https://dl.google.com/android/repository/commandlinetools-mac-13114758_latest.zip
    unzip -q "$TMP/cmdtools.zip" -d "$TMP"
    mv "$TMP/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest"
  fi

  echo "▶ Google SDK 라이선스 — 내용을 확인하고 y 로 동의해 주세요"
  "$SDKMANAGER" --licenses

  echo "▶ SDK 패키지 설치 (platform-tools, emulator, Android 34)"
  "$SDKMANAGER" "platform-tools" "emulator" "platforms;android-34" "build-tools;34.0.0"

  echo "sdk.dir=$ANDROID_HOME" > "$BLOW_DIR/local.properties"

  if [[ ! -f "$BLOW_DIR/gradlew" ]]; then
    echo "▶ Gradle Wrapper 생성 (8.11.1)"
    curl -fSL -o "$TMP/gradle.zip" https://services.gradle.org/distributions/gradle-8.11.1-bin.zip
    unzip -q "$TMP/gradle.zip" -d "$TMP"
    (cd "$BLOW_DIR" && "$TMP/gradle-8.11.1/bin/gradle" wrapper --gradle-version 8.11.1 --no-daemon -q)
  fi

  echo "✅ 1단계 완료. 이제 Android Studio 에서 Pleos 이미지를 설치하세요 (README '개발 환경 설치' 2단계)."
}

avd() {
  if [[ ! -d "$ANDROID_HOME/system-images/android-34/pleos_car_emulator/arm64-v8a" ]]; then
    echo "❌ Pleos 이미지가 아직 없습니다. Android Studio 에서 먼저 설치해 주세요." >&2
    exit 1
  fi
  echo "▶ AVD 생성: $AVD_NAME"
  # -d: 이미지에 포함된 Pleos 차량 디스플레이 프로필 (2560x1440 가로)
  echo "no" | "$AVDMANAGER" create avd --force -n "$AVD_NAME" -k "$PLEOS_IMAGE" -d "Pleos_Connect_v2_0"

  # Pleos 가이드 권장 설정: RAM 4GB, Cold boot, GPU 하드웨어 가속
  CFG="$HOME/.android/avd/$AVD_NAME.avd/config.ini"
  for kv in "hw.ramSize=4096" "fastboot.forceColdBoot=yes" "hw.gpu.enabled=yes" "hw.gpu.mode=host"; do
    key="${kv%%=*}"
    grep -v "^$key" "$CFG" > "$CFG.tmp" || true
    echo "$kv" >> "$CFG.tmp"
    mv "$CFG.tmp" "$CFG"
  done
  echo "✅ 완료. 실행: \$ANDROID_HOME/emulator/emulator -avd $AVD_NAME"
}

case "${1:-}" in
  base) base ;;
  avd) avd ;;
  *) echo "usage: $0 base|avd" >&2; exit 2 ;;
esac
