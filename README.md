# Blow

> **도로를 감상하세요. 현대가 당신의 DJ가 되어드릴게요.**
> 지금 달리는 도로와 그 순간의 날씨에 맞춰, 손대지 않아도 음악을 불러오는 차량용 음악 앱 — Pleos Connect

**Blow** = 음악을 *불러오다* + 창밖 풍경처럼 음악이 *불어오다*.
운전자는 평소처럼 Pleos 내비를 쓰고, Blow는 뒤에서 위치를 따라가다가 **음악 도로**에 들어서는 순간
**낮/밤 × 맑음·흐림·비·눈** 8가지 무드 중 지금 날씨에 맞는 노래를 틀어 줍니다. 길은 내비가, 음악은 Blow가.

**▶ 시연 영상:** https://youtu.be/Z9Pe65mXMNM

## 앱 이름 · 한 줄 소개
- 앱 이름: **Blow**
- 한 줄 소개: 지금 달리는 도로와 실시간 날씨에 맞춰 노래를 자동으로 불러오는 차량용 DJ 앱

## 주요 기능
| 기능 | 내용 | Pleos SDK |
|---|---|---|
| 도로 DJ | 음악 도로 진입 순간 곡이 서서히 커지며 시작, 화면에 DJ 멘트 | NaviHelper `onCurrentLocationInfo` |
| 8무드 선곡 | 실시간 기상 데이터 → 낮/밤 × 맑음·흐림·비·눈. 차량 센서(와이퍼 등)로 추정하지 않음 | — |
| 끊김 없는 음악 | 도로는 노래를 켜는 '스위치' — 구간을 벗어나도 곡은 끝까지, 다음 도로에선 두 곡을 겹쳐 넘김 | — |
| 내비 카드 | 진행 방향이 위인 지도 위에 음악 도로(주황)·내 차를 표시, 내비의 다음 회전·남은 시간을 그대로 표시, 누르면 Pleos 내비 지도 | `onTBTInfo` `onDestinationInfo` `requestNavigationScreen` |
| 길안내 시작 제안 | 내비에서 길안내를 시작하면 "Blow로 음악 들으실래요?" | `onRouteStarted` |
| 앨범 아트 연출 | 노래가 나오면 앨범 커버와 커버에서 뽑은 색의 그라데이션 | — |

## 실행 방법
### 1. APK로 바로 실행 (권장)
1. [Releases](../../releases/latest)에서 `Blow_release.apk` 를 받습니다.
2. Pleos Connect Emulator 를 켠 뒤 설치하고 실행합니다.
   ```bash
   adb install -r Blow_release.apk
   adb shell am start -n app.blow/.MainActivity
   ```
3. 화면 오른쪽 위 **▶ 데모** 를 누르면 가상 주행(60km/h)으로 "남부순환로 양재 구간"에 진입하는 장면이 재현됩니다.
   - 에뮬레이터의 차량 위치는 움직이지 않으므로, 진입 장면은 데모 주행으로 확인합니다.
   - 날씨 칩(예: "11° 비 오는 낮")을 누르면 무드를 바꿔 같은 도로에서 곡이 어떻게 바뀌는지 볼 수 있습니다.

### 2. 소스에서 빌드
```bash
./gradlew :app:assembleDebug           # 디버그 APK
./gradlew :app:assembleRelease         # 릴리스 APK (app/build/outputs/apk/release/)
./gradlew :app:testDebugUnitTest       # 단위 테스트
```
- 오프라인 데모 음원(선택): `bash scripts/fetch-music.sh` — Pixabay 로열티프리 음원을 `app/src/main/assets/music/` 에 받습니다.
  (Pixabay 라이선스상 음원 단독 재배포가 불가해 저장소에는 넣지 않고 APK 안에만 포함합니다.)

## 테스트 환경 · 필요한 설정
- **Pleos Connect Emulator 2.1.0** (Android Automotive, API 34, arm64) — [설치 가이드](https://document.pleos.ai/docs/connect/guide/getting-started/application-development/setup-connect-sdk-emulator)
- Android Studio, JDK 17 이상 · minSdk 28 · Kotlin 2.0 · Jetpack Compose · Media3
- Pleos SDK: `ai.pleos.playground:NaviHelper:3.0.5` (maven `https://nexus-playground.pleos.ai/repository/maven-releases/`)
- 권한: `pleos.car.permission.NAVI_ROUTE`, `pleos.car.permission.NAVI_ROUTE_SEARCH`, `INTERNET`, `POST_NOTIFICATIONS`
- **인터넷 연결 필요** — 음원 미리듣기·앨범 아트(Apple iTunes Search API), 날씨(MET Norway, 실패 시 Open-Meteo), 지도(OpenStreetMap)
  - 모두 키가 필요 없는 공개 API입니다. 미리듣기를 받지 못하면 같은 무드의 오프라인 데모 음원으로 재생합니다.

## APK 위치
**[Releases → Blow_release.apk](../../releases/latest)**

## 음원 · 지도 · 데이터 출처
- 음원: Apple Music 공식 30초 미리듣기(iTunes Search API) 스트리밍 — 음원 파일은 저장소·APK에 포함하지 않습니다.
- 오프라인 데모 음원: Pixabay Content License — [docs/MUSIC_CREDITS.md](docs/MUSIC_CREDITS.md)
- 지도: © OpenStreetMap contributors
- 날씨: MET Norway Locationforecast, Open-Meteo

## 구조
```
app/src/main/java/app/blow/
├── BlowViewModel.kt   위치 → 음악 도로 판정 → 날씨 무드 → 선곡 → 재생 (앱의 두뇌)
├── location/          NaviLocationSource(Pleos NaviHelper), DemoDriveSource(시연용), NaviGuide(내비 안내 요약)
├── road/              RoadZone(음악 도로), RoadCatalog(도로별 8무드 선곡표), ZoneTracker(진입·이탈 판정)
├── weather/           WeatherMood(8무드), WeatherRepository
├── player/            BlowPlayer(덱 2개 크로스페이드), PreviewCatalog(미리듣기·앨범 아트)
├── dj/                DjVoice(DJ 멘트)
└── ui/                BlowScreen, NaviCard(내비 카드), WeatherSheet(무드 선택), Theme
docs/PROPOSAL.md       기획서
```
