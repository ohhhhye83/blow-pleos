#!/usr/bin/env bash
# 데모 음원 내려받기 — Pixabay Content License (무료·출처표기 불필요, 단 음원 단독 재배포 금지)
# 그래서 mp3 는 git 에 올리지 않고(.gitignore) 빌드된 APK 안에만 포함한다.
# 출처·작가 목록: docs/MUSIC_CREDITS.md
set -euo pipefail
DIR="$(cd "$(dirname "$0")/.." && pwd)/app/src/main/assets/music"
mkdir -p "$DIR"
while read -r id url; do
  [[ -z "$id" || "$id" == \#* ]] && continue
  [[ -s "$DIR/$id.mp3" ]] && { echo "✓ $id (있음)"; continue; }
  curl -fsSL -A "Mozilla/5.0" -o "$DIR/$id.mp3" "$url" && echo "↓ $id"
done <<'LIST'
city_sun         https://cdn.pixabay.com/download/audio/2023/09/14/audio_805a08fc89.mp3?filename=noobehouse-sunny-ocean-drive-citypop-beat-166601.mp3
neon_drive       https://cdn.pixabay.com/download/audio/2026/05/13/audio_7c4e4c12db.mp3?filename=mvnocopyrightmusic-midnight-drive-526856.mp3
rain_on_glass    https://cdn.pixabay.com/download/audio/2026/10/04/audio_15c9bcbccc.mp3?filename=kulakovka-rainy-day-lofi-614883.mp3
grey_avenue      https://cdn.pixabay.com/download/audio/2026/03/09/audio_279468eddd.mp3?filename=absolutesound-sweet-cloudy-lofi-497905.mp3
river_breeze     https://cdn.pixabay.com/download/audio/2026/08/01/audio_f58e9a7ef3.mp3?filename=bearstockmusic-ocean-breeze-578196.mp3
han_river_lights https://cdn.pixabay.com/download/audio/2024/11/21/audio_9973dd9e18.mp3?filename=lofcosmos-city-lights-lofi-267103.mp3
white_river      https://cdn.pixabay.com/download/audio/2024/10/14/audio_154ba2a203.mp3?filename=clavier-music-evening-snowfall-soft-winter-piano-music-250829.mp3
misty_bridge     https://cdn.pixabay.com/download/audio/2026/08/11/audio_6ce93261e0.mp3?filename=331music-morning-mist-calm-relaxing-ambient-583788.mp3
LIST
