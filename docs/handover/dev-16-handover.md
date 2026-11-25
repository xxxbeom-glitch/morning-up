# [개발 16] 넘김 메모 (11-25)

다음 개발 방([개발 17])이 이어받을 것들이에요. 일 목록은 `python tools/pm/pm.py mine --as "[개발 17]"`로 다시 확인해요(owner는 아직 [개발 16]이라 넘겨받을 때 바꿔요).

## 맡은 열린 일

| 키 | 상태 | 내용 | 메모 |
|---|---|---|---|
| TSK-190 | review(재제출) | 버튼 속이기를 버튼 하나가 공처럼 튕기게(결정 155) | [QA 1]이 ISS-048로 반려 → a3d8b1a로 고쳐 다시 냄. Z플립5 결과에 따라 속도값 조정 가능 |
| TSK-094 | todo | 기기 호환성 대책 4개(방해 금지 검사, 볼륨 0 올림, 강제 종료 흔적, 배터리 제한) | Leader 지시가 오면 시작 |
| TSK-178 | todo | 시험용 빌드에 소리 녹음 확인 기능 | Leader 지시가 오면 시작 |

## QA 확인 기다리는 결함 (모두 [QA 1] 확인 대기)

| 이슈 | 고친 커밋 | 내용 |
|---|---|---|
| ISS-048 | a3d8b1a | 울림 중 화면을 껐다 켜면 20초 세기가 사라짐 → `TrickRuns`(앱 전체 보관소)가 울림 한 번(trickKey) 동안 `TrickRun`을 들고 있음 |
| ISS-045 | a3d8b1a | W1 날씨 문구 줄바꿈을 `\n`으로 |
| ISS-047 | a3d8b1a | 다크 울림 화면 상태 표시줄 아이콘 흰색(`RingRoute`의 `systemBars` → `SystemBarIcons`) |
| ISS-043, 044 | a3d8b1a | 도움말 faq_a6 '이번만 쉬기', faq_a7 띄어쓰기 |
| ISS-046 | fec4e23 | 홈 목록을 상태 표시줄 아래에서 자르고 위 24dp 알파 마스크(`topFade`). 맨 위에서는 올린 만큼만 흐려짐(디자인에 없던 덧붙임, [디자인 17]에 알림) |

[QA 1]이 반려하면 그 고침도 [개발 17]이 이어받아요.

## 최근 커밋 (모두 로컬, 푸시 안 함)

- fec4e23 ISS-046 홈 목록 위 흐리기
- a3d8b1a ISS-048, 045, 047, 043, 044
- 5cdfa67 튕기는 버튼 112dp 고정, 실험실 설명 확정본(TSK-190)
- aa4f267 버튼 속이기를 공처럼 튕기기로(TSK-190, 결정 155)

## 주의할 점

- 작업 폴더에 다른 방이 고치고 커밋하지 않은 파일이 많아요(docs, tools/pm 등). 커밋할 때는 내가 바꾼 파일만 `git add`해요. 한 파일 안에 남의 변경이 섞였으면 `git show HEAD:파일`로 꺼내 내 바꿈만 적용한 뒤 `git hash-object -w`와 `git update-index --cacheinfo`로 넣어요.
- `pm.py comment`는 글을 위치 인자로 넣어요(`--note` 아님). 모든 쓰기에 `--as "[개발 17]"`.
- 폰 설치, 권한 변경, 알람 시험은 하지 않아요. [QA 1]과 Leader 몫이에요. 단위가 끝나면 APK 경로를 [QA 1]에 알려요.
- 시험: core:model은 `:core:model:test`, 나머지는 `:모듈:testDebugUnitTest`. 빌드 `:app:assembleDebug`, 검사 `detekt`. ktlint 위반 중 원래 있던 줄(ring RingRoute import 순서, settings 여러 파일 등)은 손대지 않았어요.
- 튕기는 버튼 규칙: 112dp 고정, 속도 0.4/0.7/1.1(화면 너비 배수/초), 반사 때 ±15도, 축에서 15도 이상. 처음 만진 때부터 20초 뒤 제자리. 문구 확정본은 docs/decisions-11-25.md 결정 155.
- 커밋 메시지 끝에 `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`. 개인정보, 사업자 정보는 넣지 않아요.
