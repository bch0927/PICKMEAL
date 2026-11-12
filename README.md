# PICKMEAL

**Pleos Connect 기반 차량 특화 식사 추천 앱**

PICKMEAL은 메뉴 선택이 어려운 운전자를 위해 개인 취향과 예산을 바탕으로 음식을 추천하고, 차량 위치 기반 음식점 검색부터 Pleos 내비게이션 목적지·경유지 설정까지 연결하는 앱입니다.

2026 현대자동차그룹 차량용 앱 개발 공모전 출품작입니다.

## 1. 주요 기능

- 사용자 선호 음식 종류 및 식사 예산 설정
- 총 32종의 음식 중 개인화 메뉴 추천
- 매운맛·가격·최근 식사 등 거절 사유를 반영한 재추천
- 카카오 로컬 API 기반 차량 주변 3km 음식점 검색
- Pleos FusedLocation SDK를 활용한 차량 위치 수신
- Pleos NaviHelper SDK를 활용한 목적지 및 경유지 설정
- 음식점 검색 실패 시 오류 안내 및 재검색

## 2. 개발 기술

| 구분 | 사용 기술 |
|---|---|
| 개발 언어 | Kotlin |
| UI | Jetpack Compose |
| 개발 환경 | Android Studio |
| 차량 위치 | Pleos FusedLocation SDK 1.1.3 |
| 내비게이션 | Pleos NaviHelper SDK 3.0.5 |
| 음식점 검색 | Kakao Local API |
| 설정 저장 | Android SharedPreferences |
| 메뉴 추천 | 규칙 및 가중치 기반 추천 알고리즘 |

현재 버전은 터치 기반 MVP이며, 음성 인터페이스와 AI 기반 식사 이력 학습은 향후 개발 계획입니다.

## 3. 실행 방법

### APK 실행

1. Pleos Connect 호환 에뮬레이터를 실행합니다.
2. `apk/` 폴더의 Release APK를 설치합니다.
3. PICKMEAL을 실행하고 위치 권한을 허용합니다.
4. 선호 음식 종류와 예산을 설정합니다.
5. 메뉴 추천, 음식점 검색 및 내비게이션 연동 기능을 확인합니다.

### 소스 코드 실행

1. Android Studio에서 프로젝트를 엽니다.
2. Pleos SDK 저장소 접근이 가능한 개발 환경을 준비합니다.
3. 프로젝트 최상위 `local.properties`에 Android SDK 경로와 카카오 REST API 키를 설정합니다.
4. Gradle 동기화를 진행합니다.
5. Pleos Connect 에뮬레이터에서 앱을 실행합니다.

카카오 API 키 설정 예시:

```properties
KAKAO_REST_API_KEY=YOUR_KAKAO_REST_API_KEY
```

실제 API 키는 GitHub에 포함되지 않습니다.


## 4. 테스트 환경

- 플랫폼: Pleos Connect Emulator (42dot)
- Android API Level: 34 (Android 14)
- Supported API Levels: 34+
- System Image: Google APIs
- ABI: x86_64
- 화면 해상도: 2560 × 1440
- 화면 밀도: 160 dpi
- 테스트 위치: 서울 양재 인근
- 테스트 방식: Pleos Connect 에뮬레이터 기반 기능 검증


## 5. 검증한 기능

- 개인 취향 변경 후 추천 결과 반영
- 거절 사유에 따른 메뉴 재추천
- 실제 음식점 검색
- 네트워크 오류 및 복구 후 재검색
- 기존 목적지가 없는 경우 새로운 목적지 설정
- 기존 목적지가 있는 경우 경유지 추가
- 동일 화면에서 중복 내비게이션 요청 제한

실제 차량 환경에서의 검증은 별도로 필요합니다.

## 6. APK 다운로드

- APK 경로: `apk/오늘은 뭐 먹지?_PICKMEAL_release.apk`
- APK 유형: Release

## 7. 프로젝트 정보

- 앱 이름: PICKMEAL
- 팀 이름: 오늘은 뭐 먹지?
- 참가 공모전: 2026 현대자동차그룹 차량용 앱 개발 공모전
