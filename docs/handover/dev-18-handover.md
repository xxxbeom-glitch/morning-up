# [개발 18] 넘김 메모 (11-25)

다음 개발 방([개발 19])이 이어받을 것들이에요. 일 목록은 `python tools/pm/pm.py mine --as "[개발 19]"`로 다시 확인하고, owner가 [개발 18]인 열린 일은 `pm.py set 키 --as "[개발 19]" owner="[개발 19]"`로 바꿔요. 앞 방 메모 `dev-16-handover.md`, `dev-17-handover.md`의 '주의할 점'도 그대로 유효해요.

## 이 방에서 한 일 (모두 푸시함)

| 키 | 커밋 | 내용 | 상태 |
|---|---|---|---|
| TSK-197 | 662d21f | 결정 157: 홈 위 다음 알람 문구(`Header` onClick)를 누르면 `state.next.alarm` 편집. 다음 알람 없으면 안 눌림, TalkBack '다음 알람 편집' | QA 승인 |
| TSK-197 | 5e810a5 | 결정 159: `EditViewModel.timingChanged`(시각, 요일, 공휴일 쉬기) 바뀌면 저장 때 `skipOnceDate` 풀기, `EditExit.Saved.restReleased` → NavHost `REST_RELEASED_KEY` → 홈 토스트 '이번만 쉬기를 풀었어요…'. S0 안내 글도 같은 기준 | QA 승인 |
| TSK-197 | 674c7fb | 결정 160: H5 확인 창과 `pendingDelete` 없앰. `deleteSelected`가 바로 지우고 스낵바(여러 개 글). 스낵바 중 또 지우면 줄 선 토스트를 이어받음 | QA 승인 |
| TSK-197 | f058256 | 결정 161: '복사'→'복제', 토스트 하나/여러 개 | QA 승인 |
| TSK-197 | ef5949a | 결정 158 A안: 선택 바 '이번만 쉬기'(Lucide AlarmClockMinus). `HomeUiState.restable`(켜짐, 반복, 쉬는 중 아님, `checks.isActive` 아님, 쉬면 다음 울림이 바뀜), `restSelected`, `restDay`(결정 156 2, 3번). 선택 모드에서도 쉬는 중 글 보임(H3-3) | QA 승인 |
| TSK-198 | e1466cf, 036372b | 결정 162: `RestLabel.OnDate`, 다음 울림 날까지 `DATE_FROM_DAYS`(=7)일 이상이면 'M월 D일(O)에 다시 울려요'. 날로 셈 | QA 검토 대기 |

[QA 1]이 반려하면 그 고침도 [개발 19]가 이어받아요.

## 남은 일
- TSK-094(기기 호환성 대책 4개), TSK-178(소리 녹음 확인 기능): Leader 지시가 오면 시작해요.

## 주의할 점 (덧붙임)
- 새 규칙(CLAUDE.md '커밋과 푸시'): 단위가 끝나고 시험, 빌드가 통과하면 커밋하고 바로 `dev/overnight-11-17`에 푸시해요.
- QA에 APK를 알릴 때 빌드 시각(`ls -la --time-style=full-iso app/build/outputs/apk/debug/app-debug.apk`)을 같이 적어요. 11-25에 QA가 1분 앞선 옛 APK를 깔아 158이 안 보인다고 한 적이 있어요.
- 여러 줄 고칠 때는 scratchpad에 파이썬 스크립트를 Write로 만들어 돌리면 편해요(CRLF 파일이 많아서 줄끝을 맞춰 바꾸기). Bash heredoc 안에 한글 따옴표가 많으면 깨져요.
- `pm.py comment`는 `comment 키 "글" --as ...` 꼴이에요(`--note` 아님). `submit`은 `--note`를 받아요.
