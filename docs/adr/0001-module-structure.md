# ADR 0001. 모듈 구조와 빌드 설정

- 날짜: 2026-11-17
- 상태: 확정 (dev-plan.md '개발 방식' 1번을 그대로 따름)

## 배경

혼자 오래 유지보수할 앱이에요. 기능을 고칠 때 다른 곳이 깨지지 않고, 나중에 다른 개발자가 와도 바로 읽을 수 있어야 해요.

## 결정

1. 구글 Now in Android 견본 앱 방식으로 모듈을 나눠요. 처음에는 6개만 만들어요.

| 모듈 | 종류 | 하는 일 |
|---|---|---|
| `:app` | 안드로이드 앱 | 시작점, 길 안내(Navigation), 화면 모듈 묶기 |
| `:core:model` | 순수 코틀린 | 알람, 미션, 울림 상태 같은 데이터 모양 |
| `:core:designsystem` | 안드로이드 + Compose | 색, 글자, 간격 토큰, 움직임 토큰, 공통 부품 |
| `:core:data` | 안드로이드 | Room, DataStore, 저장소 창구(Repository) |
| `:core:alarm` | 안드로이드 | 다음 시각 계산, 예약, 울림 서비스, 복구 |
| `:core:testing` | 안드로이드 | 가짜 시계, 가짜 저장소, 테스트 도우미 |

2. 의존 방향: `feature → core`만 허용하고 `feature`끼리는 서로 모르게 해요. 화면 이동은 `:app`에서만 이어요.
3. 버전은 `gradle/libs.versions.toml` 한 파일에, 공통 빌드 설정은 `build-logic`의 convention plugin 5개(`morningup.android.application`, `.android.library`, `.android.compose`, `.hilt`, `.jvm.library`)에 둬요. 모듈 build 파일에는 버전 숫자를 쓰지 않아요.
4. 자동 검사: ktlint(모양), detekt(코드 냄새), Android Lint. `./gradlew build` 하나로 모두 돌고, 하나라도 실패하면 빌드가 실패해요.
5. 도구 버전: Gradle 8.14.3, AGP 8.13.0, Kotlin 2.2.10, KSP 2.2.10-2.0.2, Hilt 2.57.1, Compose BOM 2025.09.00, Room 2.7.2. 2026-11-17 기준 최신보다 조금 낮지만, 서로 맞는다고 확인된 조합을 골랐어요(밤새 혼자 작업하며 버전 충돌로 멈추지 않게). 나중에 한 번에 올려요.

## 결과

- 좋은 점: 엔진(`:core:alarm`)과 계산이 화면 없이 테스트돼요. 화면 모듈을 고쳐도 엔진이 다시 빌드되지 않아요.
- 비용: 모듈이 늘수록 빌드 설정이 복잡해지는데, convention plugin으로 한곳에 모아 줄였어요.
- detekt 1.23.8은 Kotlin 2.0.21로 만들어져서, detekt 실행용 Kotlin 버전을 2.0.21로 고정했어요(`CodeQuality.kt`).
