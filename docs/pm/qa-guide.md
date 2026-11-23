# 모닝업 QA 기준

알려진 공식 기준을 우리 앱(안드로이드 알람 미션 앱, 1인 개발)에 맞게 줄였어요. 일은 관리판 묶음 7(QA)에 있고, 품목 원본은 `docs/dev-plan.md` '개발 후 필수 체크리스트' 72줄이에요(11-23 [기획 4]).

## 1. 바탕으로 삼은 기준

| 기준 | 가져온 것 | 확인 |
|---|---|---|
| 안드로이드 앱 품질 핵심 기준 (Core app quality) https://developer.android.com/docs/quality-guidelines/core-app-quality | `Stability_ANR`, `App_Startup_Time`(2초), `Power_Management`, `Graceful_Degradation`(권한을 빼도 동작), `Touch_Target_Size`(48dp), `Visual_Contrast`(4.5:1, 큰 글자 3:1), `Content_Description`, `Theme_Support`, `State_Preservation`, `App_Data_Backup` | 웹에서 확인(11-23) |
| 잠자기 모드와 앱 대기 시험 (Doze, App Standby) https://developer.android.com/training/monitoring-device-state/doze-standby | `dumpsys deviceidle force-idle`, `unforce`, `dumpsys battery unplug`/`reset`, `am set-inactive` 명령. `setAlarmClock()`은 잠자기 모드에서도 제때 울림 | 웹에서 확인(11-23) |
| 정확한 알람 예약 https://developer.android.com/develop/background-work/services/alarms/schedule | 권한을 껐다 켜면 다시 예약, 재부팅 뒤 다시 예약, 안드로이드 14 백업 복원 뒤 권한이 꺼져 있을 수 있음 | 웹에서 확인(11-23) |
| 구글 플레이 사전 출시 보고서 (Pre-launch report) https://support.google.com/googleplay/android-developer/answer/9842757 | 올리면 자동으로 돌아가는 안정성, 성능, 접근성 결과. 비정상 종료 0을 관문 조건으로 | 웹에서 확인(11-23) |
| 심각도 3단계, 재발생 처리, 관문(gate) 방식 | 일반적인 QA 관행 | 아는 것으로 씀(웹 확인 안 함) |

## 2. 누가 확인하나 (`verify`)

| 값 | 보드 표시 | 누가, 어떻게 | 예 |
|---|---|---|---|
| `auto` | 자동 시험 | 개발 방이 단위, 계측 테스트를 쓰고 돌려요. QA는 결과만 봐요 | 다음 울릴 시각 계산, 미션 규칙 |
| `agent` | Claude 확인 | `[QA 1]`이 adb, 에뮬레이터, 로그(logcat, dumpsys), 화면 캡처로 직접 확인해요 | 잠자기 모드 울림, 권한 끄고 켜기, 문구 검사 |
| `user` | 사용자 확인 | 사용자가 실제 폰으로 봐야 해요. 실제 아침 알람, 며칠 방치, 플립5 접기, 소리 크기와 느낌, 화면 인상 | 주말 넘김 알람, 통화 중 알람, 시안 비교 |

- `user` 일은 `[QA 1]`이 준비(빌드 설치, 비교 자료)를 끝내고 `submit`하면 **바로 `waiting_user`**로 가요. 사용자가 확인하면 Leader가 `approve`해요. 보드 위 '사용자가 볼 QA' 숫자를 누르면 이것만 보여요.
- `user` 일은 **하루 1~2개, 15~20분**으로 묶고, 완료 조건(acceptance)에 '이렇게 확인하세요' 단계를 적어요.
- **폰 조용한 시간**: 밤 10시~아침 9시에는 설치, 권한 변경, 알람 시험을 하지 않아요. 시험 알람은 9시~22시 사이로 맞춰요. 사용성 테스트 기간이 끝나면 실제 아침 알람 시험을 사용자와 날짜를 정해 해요.
- 일 하나에는 `verify` 값 하나만 줘요. 섞이면 일을 나눠요.

## 3. 심각도

| 심각도 | 기준 | 예 |
|---|---|---|
| `high` | 알람이 안 울리거나 늦게 울릴 수 있음, 끌 수 없음, 앱이 죽음, 데이터(알람)가 사라짐, 스토어 정책 위반 | 잠자기 모드에서 안 울림, 업데이트 뒤 알람 사라짐 |
| `medium` | 기능은 되지만 틀린 동작, 안내가 틀림, 특정 기기나 모드에서만 깨짐 | 배너가 안 사라짐, 플립 바깥 화면 잘림 |
| `low` | 보기, 문구, 작은 불편 | 줄 간격, 가운뎃점 남음 |

## 4. 흐름

1. `[QA 1]`이 `claim` → 체크리스트 줄마다 확인 → `check-item`.
2. 실패한 줄은 `add-issue --title "증상 한 줄" task=TSK-xxx severity=..` (repro, expected, actual 적기). 개발 일과 연결되면 그 개발 일을 `reject`.
3. 모두 통과하면 `submit`(user 일은 사용자 차례로).
4. 고친 이슈는 개발 방이 `resolve`, QA가 다시 확인해 `approve` 또는 `reject`(재발생 횟수 +1).
5. 결정 136~143 개발 일(TSK-088, 089, 094, 096)은 검토자가 `[QA 1]`이고, 짝 검증 일(TSK-138~141)이 그 일을 deps로 기다려요.

## 5. 통과 조건 (품질 관문)

| 관문 | 날짜 | 조건 | 막는 일 |
|---|---|---|---|
| 품질 관문 1 (TSK-156) | 11-26 | 알람 핵심 A 항목 중 Claude 확인 일(TSK-133~137) 모두 done, 열린 high 이슈 0 | 비공개 테스트 시작 TSK-104 |
| 품질 관문 2 (TSK-157) | 12-10 | 체크리스트 72줄과 기준 추가 줄 모두 통과, 열린 high 이슈 0, 사전 출시 보고서 비정상 종료 0 | 프로덕션 신청 TSK-116 |

모든 확인은 마지막에 **서명한 출시 빌드(R8 켬)**로 다시 해요. 기기는 갤럭시 S24+, Z플립5, 안드로이드 12 에뮬레이터(정확한 알람 권한 시험용)예요.

## 6. 체크리스트 72줄 나눔

| 확인 방법 | 일 | 줄 |
|---|---|---|
| Claude 확인 | TSK-133~137 (A 알람 울림, G5), TSK-144 (C 흐름), 145 (D 권한), 146 (E 접근성), 147 (F 문구), 148 (G 데이터), 149 (H 출시 빌드). 짝 검증 TSK-138~141 은 체크리스트 밖 | 49 |
| 자동 시험 | TSK-142 (B 시각 계산, G4), 143 (C 미션 규칙) | 9 |
| 사용자 확인 | TSK-150 (A8 주말), 151 (A11, A20, E5 플립과 통화), 152 (A13, A16, C7 소리와 흔들기), 153 (E1, E2 시안), 154 (H2, H4~H6, H10 Play Console), 155 (출시판 실사용) | 14 + 실사용 1 |

체크리스트 줄 앞 코드(`A1`, `C7`)는 dev-plan 섹션과 순서예요. `(기준 추가)` 줄은 위 1번 기준에서 더한 것이에요.

## 바뀐 기록
- 11-23 [기획 4]: 처음 씀. Leader 지시(QA 묶음, 관문 둘, verify 구분).
