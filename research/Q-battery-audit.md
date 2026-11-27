# Q. 배터리 최적화 제외 상태의 배터리 소모 점검 (TSK-271)

- 조사: [리서치 6], 2026-11-27. 코드는 `dev/overnight-11-17` 작업 폴더 기준(읽기만, 고치지 않음).
- 배경: 우리 앱은 첫 실행에 '배터리 최적화 제외'를 받아요(결정 191 필수 권한 4개). 제외되면 안드로이드가 앱을 덜 재우기 때문에, **앱이 스스로 잠을 안 자면** 밤새 배터리를 먹어요. 그래서 '알람을 기다리는 동안 아무것도 안 하는지'가 핵심이에요.

## 1. 결론 먼저

| 판단 | 내용 |
|---|---|
| 큰 문제 | **없어요.** 알람을 기다리는 동안 도는 것이 없어요. 예약은 알람마다 `setAlarmClock` 한 번, 반복 예약(`setRepeating`), 상시 포그라운드 서비스, 상시 웨이크락, 백그라운드 위치, 주기적 날씨 갱신이 모두 없어요 |
| 고칠 것(중간) | ① 잠들기 소리의 **타이머 없음**(밤새 재생) 선택지, ② 울림 화면 `FLAG_KEEP_SCREEN_ON`이 **울림이 끝난 뒤 화면이 남아 있을 때도** 유지되는지 확인 |
| 고칠 것(작음) | ③ 진단 기록을 쓸 때마다 2000줄 자르기 쿼리, ④ '다음 알람 늘 보이기'의 앱 안 기다림(배터리보다 정확성 문제) |
| QA에 넣을 것 | 밤새 대기 소모 측정(8시간, 알람 0개 / 3개), 울림 1회 소모, 잠들기 소리 30분 소모. 방법은 4절 |

## 2. 점검표

| # | 항목 | 코드 위치 | 언제 돈다 | 판정 | 메모 |
|---|---|---|---|---|---|
| 1 | 알람 예약 | `core/alarm/.../ExactAlarms.kt` `AndroidAlarmScheduler.schedule` | 알람마다 다음 1번만 `setAlarmClock` | 좋음 | 반복은 울린 뒤 다음 번을 다시 예약하는 방식. 시스템이 정확히 한 번 깨워요 |
| 2 | 울리는 중 타이머 | `ExactAlarms.kt` `RingTimers` | 쉬는 시간 끝, W2 묻기, 재확인만 `setAlarmClock`, 나머지는 `setExactAndAllowWhileIdle` | 좋음 | 기다리는 몇 분 동안 서비스를 멈춰 둬요(주석 확인). 깨우는 횟수가 울림 1회당 몇 번 수준 |
| 3 | 울림 서비스(포그라운드) | `ring/RingService.kt` | 울리는 동안만. 세션이 끝나면 `stopForeground` + `stopSelf` | 좋음 | `START_NOT_STICKY`라 죽어도 혼자 다시 살아나 돌지 않아요 |
| 4 | 웨이크락 | `ring/RingNotifications.kt` `openScreenNow()` | 울릴 때 화면 켜기용, **10초 시간 제한** | 좋음 | 시간 제한이 있어 풀리지 않을 걱정 없음. 부분 웨이크락(`PARTIAL_WAKE_LOCK`)은 앱 어디에도 없어요 |
| 5 | 화면 켜 두기 | `app/.../ring/RingActivity.kt` `FLAG_KEEP_SCREEN_ON` | 울림 화면이 떠 있는 동안 | **확인 필요(중간)** | 울림 화면이 W2 대기, 쉬는 시간에도 떠 있으면 그 몇 분 동안 화면이 켜져 있어요. 사용자가 끄지 않고 다시 잠들면 화면이 계속 켜진 채일 수 있어요. 쉬는 시간과 대기 상태에는 `clearFlags`로 화면이 꺼지게 두는 것 추천 |
| 6 | 소리, 진동 | `ring/AlarmSoundPlayer.kt` | 울리는 동안. 서서히 키우기 0.5초 간격, 화면 꺼짐 때 진동 다시 걸기 0.3초 뒤 1번 | 좋음 | `stop()`에서 핸들러, 수신기 해제 확인 |
| 7 | 통화 확인 | `ring/CallMonitor.kt` | 울리는 동안만. 안드로이드 12+는 듣기 방식(`addOnModeChangedListener`), 12 미만만 1초 폴링 | 좋음 | 우리 시험 폰(안드로이드 14+)은 폴링 없음. 멈출 때 해제 확인 |
| 8 | 흔들기 센서 | `feature/mission/.../ShakeShakeContent.kt` | 흔들기 미션 화면에서만, `SENSOR_DELAY_GAME`, 화면 나가면 해제(`onDispose`) | 좋음 | 백그라운드 센서 없음 |
| 9 | 위치 | `weather/WeatherRepository.kt` | 날씨 켬 + 권한 있을 때, `getCurrentLocation(NETWORK_PROVIDER)` **한 번**, 10초 제한 | 좋음 | GPS가 아닌 네트워크 위치, 대략 위치 권한만. 위치 구독(`requestLocationUpdates`) 없음 |
| 10 | 날씨 갱신 | `WeatherRepository.refresh()` | 홈을 열 때, 편집 화면에서, 날씨 설정을 켤 때. 30분 안에 다시 부르면 건너뜀 | 좋음 | 주기 작업(WorkManager) 없음. 6시간 지난 날씨는 안 보여 줌 |
| 11 | 접근성 서비스(전원 종료 방지) | `guard/PowerGuardService.kt`, `res/xml/power_guard_service.xml` | 폰 전체의 '창 바뀜' 신호만 받음, 화면 내용 읽기 꺼짐 | 좋음 | 지키는 중이 아니면 바로 돌아가요(`if (!guarding) return`). 창 바뀜은 하루 수백 번이라 비용 거의 없음 |
| 12 | 다음 알람 늘 보이기 | `NextAlarmNotice.kt` | 켰을 때, 앱 프로세스 안에서 다음 알람 시각까지 `delay` | 작음(정확성) | 코루틴 `delay`는 폰을 깨우지 않아 배터리는 괜찮아요. 대신 앱 프로세스가 죽으면 알림 내용이 지난 알람에 멈춰 있을 수 있어요. 울림 서비스와 부팅에서 다시 `start()`하므로 대부분 맞춰지지만, 알람을 '이번만 쉬기'한 날 등은 QA로 확인 |
| 13 | 홈 '몇 시간 몇 분 후' | `MinuteTicker.kt` | 홈 화면이 보일 때 1분마다 | 좋음 | 화면이 꺼지면 Compose가 멈춰요 |
| 14 | 시각 따라 테마 | `app/.../ui/ClockTheme.kt` | 앱이 떠 있을 때 30초마다 | 좋음 | 화면 안에서만 |
| 15 | 무한 움직임 | `HomeSky.kt`, `SleepScreen.kt`, `SleepPlayerScreen.kt`, `MissionFrame.kt`, `TrickLayer.kt`, `MissionExample.kt` | 그 화면이 보일 때 | 좋음(화면 켜짐 소모) | 화면 꺼지면 멈춤. 홈 하늘 움직임은 화면 켜진 동안 GPU를 써요. 측정에서 '홈 5분 켜 둠'으로 확인 |
| 16 | 잠들기 소리 | `sleep/SleepPlayer.kt`, `SleepPlaybackService.kt`(mediaPlayback 포그라운드) | 재생 중. 타이머가 있으면 끝나며 멈춤, **타이머 없음(`timerMinutes = null`)이면 계속** | **중간** | 사용자가 고른 것이지만, 잠든 뒤 밤새 재생되면 배터리를 크게 먹어요. 처음 값을 30분이나 60분 타이머로, '타이머 없음'을 고르면 아침 알람이 울릴 때 멈춤(`stopForAlarm`, 이미 있음) |
| 17 | 진단 기록 | `core/data/.../DiagnosticLog.kt`, `DiagnosticLogDao.append` | 알람 사건마다 1줄 쓰고 2000줄 넘는 것 지우기 | 작음 | 쓸 때마다 `DELETE ... NOT IN (SELECT ... LIMIT 2000)`. 사건이 몇십 건이라 괜찮지만 100줄마다 한 번만 자르면 더 가벼워요 |
| 18 | 부팅, 시간 변경 수신 | `receiver/Receivers.kt` `SystemEventReceiver` | 부팅, 시간 바뀜, 업데이트 때만 | 좋음 | 화면 켜짐/꺼짐 같은 잦은 방송을 매니페스트에 걸지 않았어요 |
| 19 | 네트워크 | 날씨 1회 요청(`HttpURLConnection`, 10초 제한) | 날씨 갱신 때만 | 좋음 | 푸시, 분석 SDK 없음 |

## 3. 고칠 것 추천 (우선순위)

| 순서 | 무엇 | 왜 | 크기 | 담당 |
|---|---|---|---|---|
| 1 | 울림 화면이 쉬는 시간, W2 대기, 끝난 뒤 남아 있을 때 `FLAG_KEEP_SCREEN_ON` 풀기 | 사용자가 화면을 두고 다시 잠들면 화면이 켜진 채 남을 수 있어요(5번) | 작음 | 개발 |
| 2 | 잠들기 소리 처음 타이머를 30분 이상 값으로, '타이머 없음'에 '아침 알람까지' 같은 끝 표시 | 밤새 재생 방지(16번) | 작음(기획 확인) | 기획 → 개발 |
| 3 | 진단 기록 자르기를 N줄마다 한 번 | 쓰기 비용 감소(17번) | 아주 작음 | 개발 |
| 4 | '다음 알람 늘 보이기'를 알람 예약 지점(`AndroidAlarmScheduler.schedule/cancel`)에서도 고치기 | 프로세스가 죽은 뒤에도 알림 내용이 맞게(12번). 배터리 아님 | 작음 | 개발 |

- **하지 말 것:** 알람을 확실히 울리려고 상시 포그라운드 서비스, 주기적 WorkManager 점검, 부분 웨이크락을 더하는 것. 지금 구조(`setAlarmClock` + 제외 권한)가 정석이고, 이것들을 더하면 Play '과도한 웨이크락' 지표에 걸릴 수 있어요.

## 4. QA 목록에 넣을 백그라운드 소모 측정 (verify: agent)

Z플립5에서 Claude가 adb로 돌리고 로그만 남겨요(사용자 몫 없음). 충전기를 빼야 정확해서 **무선 adb**로 해요. 폰 설정은 바꾸지 않아요.

### 4-1. 준비와 측정 명령
```
adb -s <Z플립5> shell dumpsys batterystats --reset          # 기록 초기화(설정 변경 아님)
adb -s <Z플립5> shell dumpsys deviceidle whitelist | grep lumian   # 제외 상태 확인
# ... 시나리오 시간만큼 둠 ...
adb -s <Z플립5> shell dumpsys batterystats com.lumian.wakeup > batt.txt
adb -s <Z플립5> shell dumpsys alarm | grep -A3 lumian > alarm.txt   # 예약 수, 깨운 횟수
adb -s <Z플립5> shell dumpsys power | grep -i "wake lock" > power.txt
adb -s <Z플립5> shell dumpsys activity services com.lumian.wakeup > svc.txt
adb -s <Z플립5> bugreport bugreport.zip    # 필요하면 Battery Historian으로 그림 보기
```

### 4-2. 시나리오와 통과 기준

| # | 시나리오 | 시간 | 볼 것 | 통과 기준(제안) |
|---|---|---|---|---|
| B1 | 알람 0개, 화면 꺼둠 | 8시간(밤) | batt.txt 앱 추정 소모(mAh), 웨이크락 합계, 깨운 횟수 | 앱 소모 **1% 미만**(1mAh 단위로 거의 0), 웨이크락 0, 앱이 깨운 횟수 0, 실행 중 서비스 0 |
| B2 | 알람 3개(다음날 아침), 날씨 켬, 다음 알람 늘 보이기 켬 | 8시간 | 위와 같음 | 깨운 횟수 = 울린 알람 수, 웨이크락은 울릴 때 10초짜리만, 대기 중 서비스 0 |
| B3 | 알람 1개 울림 → 끄지 않고 1분 울림, 쉬고 다시 울림 2번 → 미션으로 끔 | 15분 | 울림 서비스가 끝나고 사라지는지, 화면 켜짐 시간 | 끈 뒤 30초 안에 서비스 0, 화면 켜두기 플래그 없음(3절 1번 고친 뒤) |
| B4 | 잠들기 소리 30분 타이머 | 35분 | 타이머 끝에 서비스 멈춤, 웨이크락 | 끝난 뒤 서비스 0, 오디오 재생 0 |
| B5 | 홈 화면 켜 두기(하늘 움직임) | 5분 | 화면 켜진 동안 CPU 시간 | 비교 기록용(통과 기준 없음, 다음 빌드와 비교) |

- 기록은 `docs/exports/qa-battery/`에 텍스트로 남기고, 숫자 요약만 관리판 댓글에.
- 출시 뒤에는 Play Console '안드로이드 바이탈'의 배터리 지표(과도한 부분 웨이크락, 과도한 깨우기)를 확인해요. 우리 구조에서는 걸릴 일이 거의 없지만, 3절 '하지 말 것'을 어기면 걸려요.

## 5. 확인 못 한 것
- 실제 숫자(mAh)는 아직 재지 않았어요. 4절 B1~B5를 QA 방이 돌려야 나와요.
- 5번(화면 켜 두기)은 코드에서 플래그를 켜는 곳만 봤고, 쉬는 시간에 울림 화면이 닫히는지는 실기에서 확인해야 해요.
