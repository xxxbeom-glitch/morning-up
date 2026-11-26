# [개발 20] 넘김 메모 (11-26)

다음 개발 방([개발 21])이 이어받을 것들이에요. 일 목록은 `python tools/pm/pm.py mine --as "[개발 21]"`로 확인하고, owner가 [개발 20]인 열린 일은 `pm.py set 키 --as "[개발 21]" owner="[개발 21]"`로 바꿔요. 앞 방 메모 `dev-16-handover.md`~`dev-19-handover.md`의 '주의할 점'도 그대로 유효해요.

## 이 방에서 한 일 (모두 `dev/overnight-11-17`에 푸시함, 검토 [QA 1])

| 키 | 커밋 | 내용 |
|---|---|---|
| TSK-211 | c59e94d | 다크 모드 홈 별 무작위 반짝임(결정 173). `HomeSky.kt` `randomStars`, `starTwinkleAlpha`. 별 24~40개, 별마다 3~6초 주기로 투명도 0→1→0, 60초 시계. 움직임 줄이기면 고정 밝기 |
| TSK-212 | 84b67e1 | 버튼 속이기 빠르기 1~10단계 슬라이더(결정 174). `TrickSpeed`는 이제 `data class(level)`, 0.2+0.1×단계 화면너비/초. 옛 저장값 slow→2, normal→5, fast→9. 처음 값 6. `TrickSpeedSlider.kt`(T3-1) |
| TSK-210 | (아래 커밋) | M5 미션 포기 화면(결정 171, 172). 아래 '210 구조' |

## TSK-210 구조
- 알림(`RingNotifications.withEscape`): 바로 답장 칸을 없애고 제목 '미션을 못 하겠으면', 누르면 `RingActivity`가 `EXTRA_OPEN_ESCAPE`로 열려요.
- `RingActivity.openEscape()` → `RingController.openEscape()`가 화면을 `RingScreen.Escape`로 정하고, `RingService.send(EscapeOpened)`.
- 상태표 `RingEvent.EscapeOpened`: 울림, 쉬는 중이면 미션 중으로 넘김(소리 끔, 미션 시간 2분, 미션 화면은 안 띄움), 미션 중이면 미션 시간 다시 채움.
- 화면은 `:feature:mission`의 `EscapeScreen.kt`(`RingEscapeRoute`), `EscapeViewModel.kt`. `RingRoute`의 `escapeContent` 자리에 :app이 꽂아요. 늘 어두운 미션 바탕(디자인 19 추천대로 다크 한 가지).
- 1분(`MissionEscape.IDLE_MILLIS`) 동안 안 쓰면 `RingLink.backToMission()`으로 하던 미션 화면. 쓰는 동안 '미션 조작'을 10초에 한 번 보내 미션 시간이 안 끝나게 해요.
- 맞으면 1.2초 '맞아요. 알람을 껐어요' 뒤 `MissionEscapeAnswers.answer` → `MissionEscaped`(W1로). 틀리면 같은 함수로 틀림 기록.
- 예문은 시험 동안 `MissionEscape.TEST_SENTENCE` 하나(`MissionEscapeStore.sentences`). 출시 전에 `releaseSentences`(예문 20개)로 되돌려요.
- 폰에서 아직 안 봤어요: 키보드가 칸을 가리지 않는지(`imePadding`), 잠금 화면 위에서 키보드가 뜨는지, 알림 누를 때 화면이 바로 M5로 가는지.

## 남은 일
1. TSK-217 이번만 쉬기 되돌리기(결정 175, `research/N-rest-once-next-ring.md` 3, 4절).
   - `feature/home/.../HomeViewModel.kt` `restDay`(234줄 근처)를 '지금 뒤 다음 울림 날'(`NextRingCalculator.next(on, at, holidays)?.toLocalDate()`)로 바꿔요. 울리는 중, 남은 확인 대기 중은 지금처럼 `lastRingDate`.
   - `restable`(결정 158) 주석과 `restOnce` 주석(결정 156 문구)도 175로 고쳐요. 선택 모드 쉬기(445줄 근처)도 `restDay`를 써서 같이 바뀌어요.
   - 누른 순간 토스트 '목요일은 쉬어요. 금요일 오전 6:30에 울려요': `HomeMessage`에 새 종류(쉬는 날, 다음 울림)를 더하고 `HomeScreen.kt` `Messages`(831줄 근처)에서 `AlarmText.dayShort`, `AlarmText.time`으로 그려요. 선택 모드에서 여러 개면 문구를 따로 정하거나 하나일 때만 띄워요(Leader에게 물어도 돼요).
   - 시험: 화 6:30 울린 뒤 → 목, 수 밤 → 금, 금 밤 → 화, 울리는 중/확인 대기 중 → 그날.
2. TSK-218 W3 시간과 횟수(결정 176, 177, `docs/plan-8-recheck-time.md`). 완료 조건은 TSK-218에 Leader가 다시 적었어요.
   - W3 대답할 시간 1분 고정(결정 145의 3, 5, 10분 고르기 없앰). W2 '깼어요' 뒤 W3까지 기다리는 시간은 W2 설정과 따로 1, 3, 5분(처음 3분). W3 묻는 횟수 1, 2, 3번(처음 1번), 둘째부터도 같은 기다리는 시간 뒤.
   - 실험실 '여러 번 묻기'(T3-2, `LabSettings.multiAsk*`, `RingPlan.askTotal`, 무작위 간격)는 없애요(결정 177). 옛 저장값은 옮기지 않고 버려요.
   - 디자인 시안(TSK-219)은 나중이라 그때까지 기존 시트 부품(Choice Chip 등)으로 만들어요.
3. TSK-212 완료 조건 3(Z플립5에서 단계 값 확인), TSK-210 폰 확인은 Leader 설치 지시 때.

## 주의할 점 (덧붙임)
- 11-26에 `core/alarm/build/.../classes.jar`가 잠겨 빌드가 실패한 적이 있어요. `./gradlew --stop` 뒤 다시 돌리면 풀렸어요(다른 방 빌드와 겹칠 때).
- 시트 안 슬라이더처럼 합쳐진 접근성 노드는 시험에서 `useUnmergedTree = true`로 찾아요.
- detekt 줄 길이(120)는 시험 파일에도 걸려요. 고친 뒤 꼭 `detekt`까지 돌려요.
