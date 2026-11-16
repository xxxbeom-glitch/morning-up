# 0단계 작업표: 준비 (빈 앱과 빌드 도구)

> 자동 실행 기록: 사용자 결정(11-16)에 따라 보여 주고 기다리지 않고 바로 진행했어요.

**목표:** `./gradlew build`가 통과하는 빈 앱(`com.lumian.morningup`, 디버그 `.debug`)과 GitHub Actions CI.

**구조:** Gradle Wrapper 8.14.3, AGP 8.13.0, Kotlin 2.2.10. 버전은 `gradle/libs.versions.toml` 하나에, 공통 빌드 설정은 `build-logic/convention`의 convention plugin에 모아요.

**근거:** `docs/dev-plan.md` 0단계, '개발 방식' 2번 코드 규칙.

## 공통 제약

- minSdk 26, targetSdk 36, compileSdk 36 (F19)
- 앱 ID `com.lumian.morningup`, 디버그 `applicationIdSuffix = ".debug"`
- 폰 설치, adb, 에뮬레이터 사용 없음 (11-16 사용자 결정)

## 작업

### 작업 1: Gradle Wrapper와 설정 파일
- 만들 것: `gradlew`, `gradlew.bat`, `gradle/wrapper/*`, `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `.gitignore`, `gradle/libs.versions.toml`
- 확인: `./gradlew --version`이 8.14.3

### 작업 2: build-logic convention plugin
- `morningup.android.application`, `morningup.android.library`, `morningup.android.compose`, `morningup.hilt`, `morningup.jvm.library`, `morningup.quality`(ktlint, detekt)
- 확인: 각 모듈 build 파일에 버전 숫자가 없어요

### 작업 3: 빈 `:app`
- `MorningUpApplication`(`@HiltAndroidApp`), `MainActivity`(Compose, edge-to-edge)
- 확인: `./gradlew :app:assembleDebug`

### 작업 4: CI
- `.github/workflows/ci.yml`: JDK 17, `./gradlew build`, 실패 시 보고서 올리기
- 확인: push 뒤 `gh run list`가 성공

### 작업 5: ADR 0001 모듈 구조
- `docs/adr/0001-module-structure.md`

## 끝 기준
`./gradlew build` 통과, 커밋, push, CI 초록불. (dev-plan의 '빈 앱이 폰에 설치돼 열려요'는 사용자가 아침에 확인해요.)
