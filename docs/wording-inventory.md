# 앱 '일어나' 전체 문구 목록 (TSK-296)

- 작성: [기획 12], 2026-11-27. 나중에 문구를 한꺼번에 고칠 때 한눈에 보려고 만든 목록이에요. 코드는 고치지 않았어요.
- **기준 커밋: `4e8d795`** (dev/overnight-11-17, 11-27 13:14 [개발 28] 알람음과 잠들기 이름 리소스 옮김). 이 뒤에 바뀐 문구는 들어 있지 않아요.
- 모은 곳: 모든 모듈의 `src/*/res/values*/` 문구 파일 13개(strings.xml, strings_lab_escape.xml, sound_names.xml), 코드(Kotlin) 속 한국어, `AndroidManifest.xml`과 접근성 서비스 설정, 미션 콘텐츠 파일(`mission_content.json`).
- 화면 ID는 `docs/pm/naming.md`를 따라요. 용어 기준은 `docs/plan-11-wording.md`예요.

## 한눈에 보기

| 무엇 | 개수 |
|---|---|
| 문구 리소스 키 | **678개** (문자열 671개, 문자열 묶음 7개) |
| 화면에 나오는 글 줄 수 | **733줄** (문자열 671 + 묶음 속 항목 62) |
| 코드에 직접 적힌, 사용자에게 보이는 한국어 | 3곳 (12절) |
| 복수형(plurals) 리소스 | 0개. 횟수는 모두 '%1$d번', '%1$d개'처럼 한 형태로 써요 |
| 디버그 전용(`src/debug`) 문구 | 0개. 디버그 코드에는 한국어 화면 글이 없어요 (13절) |
| 쓰는 곳이 없는 리소스 | 15개 (15-3절) |
| 미션 콘텐츠(퀴즈 350문제, 단어 7단계) | 화면 문구가 아니라 따로 셌어요 (14절) |

## 목차

| 절 | 줄 수 |
|---|---|
| 1. 앱 이름과 하단 탭 | 8 |
| 2. 첫 실행 (O) | 31 |
| 3. 알람 탭 홈 (H) | 75 |
| 4. 알람 생성과 편집 (S) | 141 |
| 5. 알람 울림 (D) | 3 |
| 6. 미션 (M) | 17 |
| 7. 알람이 울린 뒤 (W) | 24 |
| 8. 잠들기 탭 (Z, 지금 숨김) | 14 |
| 9. 설정 탭 (T) | 224 |
| 10. 화면 밖: 알림 패널과 폰 설정 | 91 |
| 11. 여러 화면이 같이 쓰는 이름과 형식 | 105 |
| (1~11절 합계) | 733 |
| 12. 코드에 직접 적힌 한국어 | 3곳 |
| 13. 디버그 전용 | 0 |
| 14. 미션 콘텐츠 (참고) | 따로 |
| 15. 점검 메모 | |

## 표 읽는 법

- **문구**: 화면에 나오는 그대로예요. 줄바꿈은 `<br>`, 묶음(배열)은 `1) 2) 3)` 번호로 적었어요. `%1$s`, `%1$d` 같은 자리에는 실제 값이 들어가요. 예시는 '어떤 상황에 보이나' 칸에 '예:'로 적었어요.
- **보이는 방식**: 제목, 본문, 버튼, 줄 이름(설정 줄 왼쪽 글), 줄 값(오른쪽이나 아래 글), 시트 제목, 토스트, 스낵바, 알림 제목 등. '화면 읽기용'은 눈에 보이지 않고 톡백 같은 화면 읽기 프로그램만 읽는 글이에요.
- **리소스 키와 위치**: `키` (파일 줄임:줄) 다음 줄에 그 키를 쓰는 코드 파일:줄을 모두 적었어요. 한 키를 여러 곳에서 쓰면 쓰는 곳이 여러 개 나와요.
- 파일 줄임: `app` = app/src/main, `app(mission)`, `app(onboarding)` = 확인용 앱, `alarm` = core/alarm, `alarm/sound_names` = core/alarm의 sound_names.xml, `ui` = core/ui, `edit`, `home`, `mission`, `ring`, `settings`, `sleep` = feature 아래 각 모듈, `settings/lab_escape` = strings_lab_escape.xml.
- 다른 키인데 글이 똑같은 것은 15-4절에 모아 두었어요. 한 글을 바꿀 때 같이 바꿀지 거기서 보면 돼요.

## 1. 앱 이름과 하단 탭

| 문구 | 어떤 상황에 보이나 | 보이는 방식 | 리소스 키와 위치 |
|---|---|---|---|
| 일어나 | 폰 홈 화면 아이콘 아래, 앱 목록, 최근 앱 | 앱 이름 | `app_name` (app:3)<br>쓰는 곳: AndroidManifest.xml:17 |
| 일어나 미션 | 미션 확인용 앱 아이콘 이름(디버그 확인용) | 앱 이름 | `app_name` (app(mission):4)<br>쓰는 곳: AndroidManifest.xml:17 |
| 일어나 온보딩 | 온보딩 확인용 앱 아이콘 이름(디버그 확인용) | 앱 이름 | `app_name` (app(onboarding):4)<br>쓰는 곳: AndroidManifest.xml:17 |
| 알람 | 하단 탭 첫째 칸 | 탭 이름(글자 없이 아이콘만 보이면 화면 읽기용) | `tab_alarm` (app:6)<br>쓰는 곳: MorningUpApp.kt:37, MorningUpApp.kt:107 |
| 잠들기 | 하단 탭 둘째 칸(잠들기 탭은 지금 숨김) | 탭 이름 | `tab_sleep` (app:7)<br>쓰는 곳: MorningUpApp.kt:39, MorningUpApp.kt:108 |
| 설정 | 하단 탭 셋째 칸 | 탭 이름 | `tab_settings` (app:8)<br>쓰는 곳: MorningUpApp.kt:38, MorningUpApp.kt:109 |
| 확인 필요 | 권한이 꺼져 있을 때 설정 탭에 붙는 표시 | 탭 배지 읽기용 이름 | `tab_settings_alert` (app:13)<br>쓰는 곳: MorningUpApp.kt:95 |
| %1$s 화면은 준비 중이에요 | 탭 화면이 아직 연결되지 않았을 때 빈 자리. 예: '잠들기 화면은 준비 중이에요' | 본문(임시) | `tab_placeholder` (app:11)<br>쓰는 곳: MorningUpApp.kt:116 |

## 2. 첫 실행 (O)

| 문구 | 어떤 상황에 보이나 | 보이는 방식 | 리소스 키와 위치 |
|---|---|---|---|
| 일어나 | O1 스플래시 로고 | 화면 읽기용 이름 | `onboarding_splash_desc` (settings:113)<br>쓰는 곳: OnboardingPage.kt:33 |
| 건너뛰기 | (쓰는 곳 없음) | 버튼 | `onboarding_skip` (settings:114)<br>쓰는 곳: 없음 |
| %1$d / %2$d 단계 | O4-1~O4-4 위쪽 진행 표시. 예: '2 / 4 단계' | 진행 글 | `first_run_progress` (edit:197)<br>쓰는 곳: FirstRunAlarm.kt:256 |
| 몇 시에 깨워 드릴까요? | O4-1 알람 시간 고르기 | 화면 제목 | `first_run_time_title` (edit:186)<br>쓰는 곳: FirstRunAlarm.kt:169 |
| 매일 울려요. 요일은 나중에 바꿀 수 있어요 | O4-1 시간 고르는 휠 아래 | 안내 글 | `first_run_time_note` (edit:187)<br>쓰는 곳: FirstRunAlarm.kt:290 |
| 알람 화면 배경을 골라 주세요 | O4-2 알람 화면 배경 고르기 | 화면 제목 | `first_run_bg_title` (edit:188)<br>쓰는 곳: FirstRunAlarm.kt:170 |
| 어떤 소리로 깨워 드릴까요? | O4-3 벨소리 고르기 | 화면 제목 | `first_run_sound_title` (edit:189)<br>쓰는 곳: FirstRunAlarm.kt:171 |
| 알람을 해제할 미션을 골라 주세요 | O4-4 미션 고르기 | 화면 제목 | `first_run_mission_title` (edit:190)<br>쓰는 곳: FirstRunAlarm.kt:172 |
| 미션 없이 | O4-4 맨 위 '미션 없이' 카드 | 카드 제목 | `first_run_mission_none` (edit:191)<br>쓰는 곳: FirstRunAlarm.kt:485 |
| 버튼을 눌러 알람을 해제해요 | O4-4 '미션 없이' 카드 아래 | 카드 설명 | `first_run_mission_none_desc` (edit:192)<br>쓰는 곳: FirstRunAlarm.kt:491 |
| 이 폰에서는 쓸 수 없는 미션이에요 | O4-4 흔들기 센서가 없는 폰에서 Shake Shake 카드 | 카드 설명(흐리게) | `first_run_mission_no_sensor` (edit:193)<br>쓰는 곳: FirstRunAlarm.kt:414 |
| 다음 | O4-1~O4-3 아래 | 버튼 | `first_run_next` (edit:194)<br>쓰는 곳: FirstRunAlarm.kt:154 |
| 완료 | O4-4 마지막 단계 아래 | 버튼 | `first_run_done` (edit:195)<br>쓰는 곳: FirstRunAlarm.kt:154 |
| 건너뛰기 | O4 위쪽 오른쪽 | 글자 버튼 | `first_run_skip` (edit:196)<br>쓰는 곳: FirstRunAlarm.kt:212 |
| 알람이 울릴 때 화면을 띄우려면<br>알림 권한이 필요해요 | O4-1a 알림 권한을 묻는 카드 | 카드 제목 | `first_run_notif_title` (settings:116)<br>쓰는 곳: FirstRunPermissions.kt:122 |
| 허용하지 않으면 소리만 나고 해제 화면이 안 뜰 수 있어요 | O4-1a 카드 제목 아래 | 카드 본문 | `first_run_notif_desc` (settings:117)<br>쓰는 곳: FirstRunPermissions.kt:123 |
| 허용하기 | O4-1a 카드, P5 위치 권한 대화상자 | 버튼(확인 쪽) | `ask_allow` (settings:101)<br>쓰는 곳: FirstRunPermissions.kt:124, WeatherSettings.kt:95 |
| 나중에 | O4-1a 카드, P5 위치 권한 대화상자, P6 전원 종료 방지 시트 | 버튼(미루는 쪽) | `ask_later` (settings:100)<br>쓰는 곳: FirstRunPermissions.kt:138, PowerGuardSheet.kt:18, WeatherSettings.kt:94 |
| 알람이 제때 울리도록<br>아래 권한을 켜 주세요 | O5 필수 권한 목록 맨 위 | 화면 제목 | `first_run_perm_title` (settings:118)<br>쓰는 곳: FirstRunPermissions.kt:250 |
| 알람 | O5 목록 첫 줄(알림과 전체 화면 알림을 묶은 줄) | 줄 이름 | `first_run_perm_alarm` (settings:119)<br>쓰는 곳: FirstRunPermissions.kt:75 |
| 잠금 화면에서도 알람 화면을 바로 띄워요 | O5 '알람' 줄 아래 | 줄 설명 | `first_run_perm_alarm_desc` (settings:120)<br>쓰는 곳: FirstRunPermissions.kt:75 |
| 배터리 사용 제한 해제 | O5 목록 둘째 줄 | 줄 이름 | `first_run_perm_battery` (settings:121)<br>쓰는 곳: FirstRunPermissions.kt:76 |
| 절전 모드에서도 알람이 늦지 않게 해요 | O5 배터리 줄 아래 | 줄 설명 | `first_run_perm_battery_desc` (settings:122)<br>쓰는 곳: FirstRunPermissions.kt:76 |
| 다른 앱 위에 표시 | O5 목록 셋째 줄 | 줄 이름 | `first_run_perm_overlay` (settings:123)<br>쓰는 곳: FirstRunPermissions.kt:77 |
| 다른 앱을 쓰는 중에도 알람 화면을 띄워요 | O5 다른 앱 위에 표시 줄 아래 | 줄 설명 | `first_run_perm_overlay_desc` (settings:124)<br>쓰는 곳: FirstRunPermissions.kt:77 |
| 전원 종료 방지 | O5 목록 넷째 줄 | 줄 이름 | `first_run_perm_power` (settings:125)<br>쓰는 곳: FirstRunPermissions.kt:78 |
| 알람이 울리는 동안 전원 메뉴가 열리면 바로 닫아요 | O5 전원 종료 방지 줄 아래 | 줄 설명 | `first_run_perm_power_desc` (settings:126)<br>쓰는 곳: FirstRunPermissions.kt:78 |
| 설정하기 | O5 각 줄 오른쪽, 아직 꺼져 있을 때 | 작은 버튼 | `first_run_perm_set` (settings:127)<br>쓰는 곳: FirstRunPermissions.kt:324 |
| 켜짐 | O5 각 줄 오른쪽, 켜졌을 때 | 상태 글 | `first_run_perm_done` (settings:128)<br>쓰는 곳: FirstRunPermissions.kt:279 |
| 시작하기 | O5 맨 아래 | 버튼 | `first_run_start` (settings:129)<br>쓰는 곳: FirstRunPermissions.kt:260 |
| 닫기 | O5 위쪽 닫기 아이콘 | 화면 읽기용 이름 | `first_run_close` (settings:130)<br>쓰는 곳: FirstRunPermissions.kt:236 |

## 3. 알람 탭 홈 (H)

| 문구 | 어떤 상황에 보이나 | 보이는 방식 | 리소스 키와 위치 |
|---|---|---|---|
| %1$s 후<br>알람이 울려요 | H0 맨 위 큰 글, 켜진 알람이 있을 때. 예: '7시간 20분 후(줄바꿈)알람이 울려요' | 큰 제목(두 줄) | `home_next_headline` (home:5)<br>쓰는 곳: HomeScreen.kt:335 |
| %1$s %2$s | H0 큰 제목 아래 날짜와 시각. 예: '11월 16일 (월) 오전 6:30' | 보조 글 | `home_next_subline` (home:7)<br>쓰는 곳: HomeScreen.kt:337 |
| 다음 알람 편집 | H0 맨 위 글을 누르면 다음 알람 편집으로 감 | 화면 읽기용 이름 | `home_next_edit` (home:4)<br>쓰는 곳: HomeScreen.kt:312 |
| 예정된 알람이<br>없어요 | H1 빈 홈, 켜진 알람이 없을 때 맨 위 | 큰 제목(두 줄) | `home_empty_headline` (home:9)<br>쓰는 곳: HomeScreen.kt:342 |
| 첫 알람을 만들어 볼까요? | H1 빈 홈 가운데 | 본문 제목 | `home_empty_title` (home:10)<br>쓰는 곳: HomeScreen.kt:801 |
| 알람 생성 | H1 빈 홈 가운데 | 버튼 | `home_empty_button` (home:11)<br>쓰는 곳: HomeScreen.kt:807 |
| 알람 추가 | H0 목록 위 + 아이콘 | 화면 읽기용 이름 | `home_add` (home:13)<br>쓰는 곳: HomeScreen.kt:555 |
| 더보기 | H0 목록 위 점 세 개 아이콘 | 화면 읽기용 이름 | `home_more` (home:14)<br>쓰는 곳: HomeScreen.kt:557 |
| %1$s, %2$s | 알람 카드 하나를 화면 읽기 프로그램이 읽을 때. 예: '오전 6:30, 평일' | 화면 읽기용 | `home_card_description` (home:16)<br>쓰는 곳: HomeScreen.kt:450 |
| 오늘 | 한 번만 울리는 알람 카드의 요일 자리, 오늘 울릴 때 | 카드 보조 글 | `home_once_today` (home:43)<br>쓰는 곳: HomeScreen.kt:746 |
| 내일 | 한 번만 울리는 알람 카드의 요일 자리, 내일 울릴 때 | 카드 보조 글 | `home_once_tomorrow` (home:44)<br>쓰는 곳: HomeScreen.kt:746 |
| 이번만 쉬기 | H8 반복 알람 스위치를 끄면 카드에 나옴 | 카드 안 글자 버튼 | `home_rest_offer` (home:26)<br>쓰는 곳: HomeScreen.kt:535 |
| %1$s요일에 다시 울려요 | H8 이번만 쉬는 중인 카드. 예: '목요일에 다시 울려요' | 카드 보조 글 | `home_resting` (home:27)<br>쓰는 곳: HomeScreen.kt:480 |
| %1$d월 %2$d일(%3$s)에 다시 울려요 | H8 쉬는 중, 7일 넘게 남았을 때. 예: '12월 3일(목)에 다시 울려요' | 카드 보조 글 | `home_resting_date` (home:29)<br>쓰는 곳: HomeScreen.kt:482 |
| %1$s 뒤 다시 울려요 | H8 쉬는 중, 24시간 안일 때. 예: '19시간 43분 뒤 다시 울려요' | 카드 보조 글 | `home_resting_within` (home:31)<br>쓰는 곳: HomeScreen.kt:478 |
| %1$s요일은 쉬어요. %2$s요일 %3$s에 울려요 | H8 '이번만 쉬기'를 누른 순간. 예: '목요일은 쉬어요. 금요일 오전 6:30에 울려요' | 토스트 | `home_rested` (home:33)<br>쓰는 곳: HomeScreen.kt:827 |
| %1$s요일은 쉬어요. %2$s %3$s에 울려요 | H8 누른 순간, 다음 울림이 7일 이상 뒤. 예: '토요일은 쉬어요. 12월 5일 (토) 오전 7:10에 울려요' | 토스트 | `home_rested_date` (home:35)<br>쓰는 곳: HomeScreen.kt:825 |
| 알람을 저장했어요. %1$s 뒤에 울려요 | H6 편집에서 저장하고 돌아왔을 때. 예: '알람을 저장했어요. 7시간 20분 뒤에 울려요' | 토스트 | `home_saved` (home:19)<br>쓰는 곳: HomeScreen.kt:850 |
| 이번만 쉬기를 풀었어요. %1$s 뒤에 울려요 | 쉬는 중이던 알람을 저장해 쉬기가 풀렸을 때. 예: '이번만 쉬기를 풀었어요. 7시간 20분 뒤에 울려요' | 토스트 | `home_saved_rest_released` (home:20)<br>쓰는 곳: HomeScreen.kt:850 |
| 알람을 저장했어요 | 꺼진 알람을 저장했을 때 | 토스트 | `home_saved_off` (home:21)<br>쓰는 곳: HomeScreen.kt:853 |
| 알람을 지웠어요 | H7 알람 하나를 지웠을 때 | 스낵바 | `home_deleted` (home:22)<br>쓰는 곳: HomeScreen.kt:867 |
| 알람 %1$d개를 지웠어요 | H7 여러 개를 지웠을 때. 예: '알람 3개를 지웠어요' | 스낵바 | `home_deleted_many` (home:23)<br>쓰는 곳: HomeScreen.kt:869 |
| 되돌리기 | H7 삭제 스낵바 오른쪽 | 스낵바 버튼 | `home_undo` (home:24)<br>쓰는 곳: HomeScreen.kt:871 |
| 알람을 복제했어요 | 선택 모드에서 하나를 복제했을 때 | 토스트 | `home_copied_one` (home:51)<br>쓰는 곳: HomeScreen.kt:857 |
| 알람 %1$d개를 복제했어요 | 여러 개를 복제했을 때. 예: '알람 2개를 복제했어요' | 토스트 | `home_copied` (home:52)<br>쓰는 곳: HomeScreen.kt:859 |
| 재확인을 해제했어요 | 편집에서 저장해 남은 재확인(W2, W3)이 멈췄을 때 | 토스트 | `home_checks_stopped` (home:54)<br>쓰는 곳: HomeScreen.kt:862 |
| 울리는 중인 알람이에요. 울림 화면에서 해제해 주세요 | 울리는 중인 알람 카드를 눌렀을 때 | 토스트 | `home_ringing_blocked` (home:71)<br>쓰는 곳: HomeScreen.kt:863 |
| 모든 알람 끄기 | H3 더보기 메뉴, 켜진 알람이 있을 때 | 메뉴 줄 | `home_menu_all_off` (home:38)<br>쓰는 곳: HomeScreen.kt:570 |
| 모든 알람 켜기 | H3 더보기 메뉴, 모두 꺼져 있을 때 | 메뉴 줄 | `home_menu_all_on` (home:39)<br>쓰는 곳: HomeScreen.kt:570 |
| 선택 | H3 더보기 메뉴 | 메뉴 줄 | `home_menu_select` (home:40)<br>쓰는 곳: HomeScreen.kt:574 |
| %1$d개 선택됨 | H3 선택 모드 위쪽. 예: '2개 선택됨' | 머리글 | `home_selected_count` (home:41)<br>쓰는 곳: HomeScreen.kt:620 |
| 전체 선택 | H3 선택 모드 위쪽 | 글자 버튼 | `home_select_all` (home:42)<br>쓰는 곳: HomeScreen.kt:656 |
| 취소 | H3 선택 모드 위쪽 | 글자 버튼 | `home_select_cancel` (home:45)<br>쓰는 곳: HomeScreen.kt:670 |
| 켜기 | H3 선택 모드 아래 막대 | 아이콘 버튼 글 | `home_action_on` (home:46)<br>쓰는 곳: HomeScreen.kt:701 |
| 끄기 | H3 선택 모드 아래 막대 | 아이콘 버튼 글 | `home_action_off` (home:47)<br>쓰는 곳: HomeScreen.kt:704 |
| 이번만 쉬기 | H3 선택 모드 아래 막대 | 아이콘 버튼 글 | `home_action_rest` (home:48)<br>쓰는 곳: HomeScreen.kt:707 |
| 복제 | H3 선택 모드 아래 막대 | 아이콘 버튼 글 | `home_action_copy` (home:49)<br>쓰는 곳: HomeScreen.kt:709 |
| 삭제 | H3 선택 모드 아래 막대 | 아이콘 버튼 글 | `home_action_delete` (home:50)<br>쓰는 곳: HomeScreen.kt:710 |
| 재수면 여부를 %1$d분 후 재확인할게요 | H0 위쪽 띠, 알람 해제 뒤 W2를 기다릴 때. 예: '재수면 여부를 3분 후 재확인할게요' | 띠 제목 | `home_wake_check_soon_band` (home:66)<br>쓰는 곳: RecheckBanner.kt:152 |
| 곧 재수면 여부를 재확인할게요 | 같은 띠, 남은 시간이 1분 안일 때 | 띠 제목 | `home_wake_check_soon_band_now` (home:67)<br>쓰는 곳: RecheckBanner.kt:154 |
| 완전히 일어나셨으면 재확인을 해제해주세요 | 같은 띠 제목 아래 | 띠 설명 | `home_wake_check_soon_desc` (home:69)<br>쓰는 곳: RecheckBanner.kt:156 |
| 해제 | 같은 띠 오른쪽 | 띠 글자 버튼 | `home_recheck_release` (home:70)<br>쓰는 곳: RecheckBanner.kt:158 |
| 곧 재수면 여부를 재확인할게요 | H0 위쪽 띠, W3 재확인이 남았을 때 | 띠 제목 | `home_recheck_title` (home:56)<br>쓰는 곳: RecheckBanner.kt:106 |
| 재확인이 %1$d번 남았어요 | 같은 띠, 실험실 좀비모드로 남은 횟수가 있을 때. 예: '재확인이 2번 남았어요' | 띠 제목 | `home_recheck_title_count` (home:58)<br>쓰는 곳: RecheckBanner.kt:104 |
| 완전히 일어나셨으면 재확인을 해제해주세요 | W3 띠 제목 아래 | 띠 설명 | `home_recheck_desc` (home:57)<br>쓰는 곳: RecheckBanner.kt:108 |
| 완전히 일어나셨으면 재확인을 해제해주세요 | 좀비모드 W3 띠 제목 아래 | 띠 설명 | `home_recheck_desc_one` (home:59)<br>쓰는 곳: RecheckBanner.kt:108 |
| 해제 | W3 띠 오른쪽 | 띠 글자 버튼 | `home_recheck_action` (home:60)<br>쓰는 곳: RecheckBanner.kt:109 |
| %1$s에 다시 울려요 | H0 위쪽 띠, 다시 울림이나 미루기로 쉬는 중. 예: '오전 6:35에 다시 울려요' | 띠 제목 | `home_resting_band` (home:61)<br>쓰는 곳: RecheckBanner.kt:124 |
| 지금 미션을 하면 바로 끝나요 | 다시 울림으로 쉬는 중 띠 아래 | 띠 설명 | `home_resting_band_desc` (home:62)<br>쓰는 곳: RecheckBanner.kt:125 |
| 알람을 미뤘어요. 지금 미션을 하면 바로 끝나요 | 미루기로 쉬는 중 띠 아래 | 띠 설명 | `home_snoozed_band_desc` (home:65)<br>쓰는 곳: RecheckBanner.kt:125 |
| 미션 하기 | 쉬는 중 띠 오른쪽 | 띠 글자 버튼 | `home_resting_band_action` (home:63)<br>쓰는 곳: RecheckBanner.kt:126 |
| 알람이 소리 없이 울렸어요 | 앱을 열었을 때, 지난 알람이 방해 금지 모드로 소리 없이 울렸으면 한 번 | 대화상자 제목 | `silenced_ring_title` (app:15)<br>쓰는 곳: MorningNavHost.kt:407 |
| 방해 금지 모드가 알람 소리를 막고 있었어요. '알람'을 켜 두면 다음부터 소리가 나요. | 같은 대화상자 | 대화상자 본문 | `silenced_ring_message` (app:16)<br>쓰는 곳: MorningNavHost.kt:408 |
| 닫기 | 같은 대화상자 | 버튼 | `silenced_ring_close` (app:17)<br>쓰는 곳: MorningNavHost.kt:409 |
| 설정 열기 | 같은 대화상자 | 버튼 | `silenced_ring_open` (app:18)<br>쓰는 곳: MorningNavHost.kt:410 |
| %1$s. 그 밖에 %2$d개 더 | H2 배너 설명 끝, 꺼진 권한이 더 있을 때(T2 권한 화면 배너도 같음). 예: '절전 때문에 알람이 울리지 않을 수 있어요. 그 밖에 2개 더' | 배너 설명 | `banner_more` (settings:76)<br>쓰는 곳: PermissionBanner.kt:91 |
| 켜기 | H2 배너 오른쪽(다른 앱 위에 표시, 알림, 전체 화면 알림, 정확한 알람, 배터리 제한) | 배너 버튼 | `banner_turn_on` (settings:55)<br>쓰는 곳: PermissionBanner.kt:43, PermissionBanner.kt:47, PermissionBanner.kt:49, PermissionBanner.kt:51, PermissionBanner.kt:53, PermissionBanner.kt:55 |
| 풀기 | H2 배너 오른쪽(배터리 사용 제한) | 배너 버튼 | `banner_release` (settings:66)<br>쓰는 곳: PermissionBanner.kt:57 |
| 보기 | H2-7 배너 오른쪽(방해 금지 모드) | 배너 버튼 | `banner_view` (settings:68)<br>쓰는 곳: PermissionBanner.kt:45 |
| 알람이 울려도 화면이 안 떠요 | H2 알림과 다른 앱 위에 표시가 둘 다 꺼졌을 때 | 배너 제목 | `banner_both` (settings:56)<br>쓰는 곳: PermissionBanner.kt:49 |
| 알림과 '다른 앱 위에 표시'를 켜 주세요. 미션을 못 할 때 해제하는 길도 알림으로 와요 | 같은 배너 | 배너 설명 | `banner_both_desc` (settings:57)<br>쓰는 곳: PermissionBanner.kt:49 |
| 알림이 꺼져 있어요 | H2 알림이 꺼졌을 때 | 배너 제목 | `banner_notifications` (settings:58)<br>쓰는 곳: PermissionBanner.kt:51 |
| 알람 화면이 안 뜰 수 있어요.<br>미션을 못 할 때 해제하는 길도 알림으로 와요 | 같은 배너 | 배너 설명 | `banner_notifications_desc` (settings:59)<br>쓰는 곳: PermissionBanner.kt:51 |
| '다른 앱 위에 표시'가 꺼져 있어요 | H2 다른 앱 위에 표시가 꺼졌을 때 | 배너 제목 | `banner_overlay` (settings:60)<br>쓰는 곳: PermissionBanner.kt:53 |
| 홈 버튼을 누르면 알람 화면이 사라질 수 있어요 | 같은 배너 | 배너 설명 | `banner_overlay_desc` (settings:61)<br>쓰는 곳: PermissionBanner.kt:53 |
| '전체 화면 알림'이 꺼져 있어요 | H2 전체 화면 알림이 꺼졌을 때 | 배너 제목 | `banner_full_screen` (settings:62)<br>쓰는 곳: PermissionBanner.kt:55 |
| 잠금 화면에서 알람 화면이 바로 안 뜰 수 있어요 | 같은 배너 | 배너 설명 | `banner_full_screen_desc` (settings:63)<br>쓰는 곳: PermissionBanner.kt:55 |
| 배터리 사용 제한이 켜져 있어요 | H2 배터리 사용 제한이 켜졌을 때 | 배너 제목 | `banner_battery` (settings:64)<br>쓰는 곳: PermissionBanner.kt:57 |
| 절전 때문에 알람이 울리지 않을 수 있어요 | 같은 배너 | 배너 설명 | `banner_battery_desc` (settings:65)<br>쓰는 곳: PermissionBanner.kt:57 |
| 배터리가 '제한'으로 되어 있어요 | H2-6 배터리가 '제한'일 때 | 배너 제목 | `banner_restricted` (settings:69)<br>쓰는 곳: PermissionBanner.kt:43 |
| 알람이 울리지 않을 수 있어요 | 같은 배너 | 배너 설명 | `banner_restricted_desc` (settings:70)<br>쓰는 곳: PermissionBanner.kt:43 |
| 방해 금지 모드가 알람 소리를 막아요 | H2-7 방해 금지 모드가 알람 소리를 막을 때 | 배너 제목 | `banner_silenced` (settings:71)<br>쓰는 곳: PermissionBanner.kt:45 |
| '알람' 허용을 켜 주세요 | 같은 배너 | 배너 설명 | `banner_silenced_desc` (settings:72)<br>쓰는 곳: PermissionBanner.kt:45 |
| 알람이 몇 분 늦게 울릴 수 있어요 | H2-8 정확한 알람이 꺼졌을 때(안드로이드 12) | 배너 제목 | `banner_exact` (settings:73)<br>쓰는 곳: PermissionBanner.kt:47 |
| '정확한 알람'을 켜 주세요 | 같은 배너 | 배너 설명 | `banner_exact_desc` (settings:74)<br>쓰는 곳: PermissionBanner.kt:47 |

## 4. 알람 생성과 편집 (S)

### S0 편집 화면

| 문구 | 어떤 상황에 보이나 | 보이는 방식 | 리소스 키와 위치 |
|---|---|---|---|
| 알람 생성 | S0 새 알람을 만들 때 | 화면 제목 | `edit_title_new` (edit:4)<br>쓰는 곳: EditScreen.kt:240 |
| 알람 편집 | S0 이미 있는 알람을 고칠 때 | 화면 제목 | `edit_title` (edit:3)<br>쓰는 곳: EditScreen.kt:240 |
| 뒤로 | S0, S1, S5, S8, 세부 화면, O4 위쪽 뒤로 아이콘 | 화면 읽기용 이름 | `edit_back` (edit:5)<br>쓰는 곳: BackgroundPage.kt:131, DetailPages.kt:89, EditScreen.kt:242, FirstRunAlarm.kt:204, MissionPickPage.kt:98, SoundPage.kt:96 |
| %1$s 후 알람이 울려요 | S0 시간 휠 위. 예: '7시간 20분 후 알람이 울려요' | 안내 글 | `edit_until` (edit:7)<br>쓰는 곳: EditScreen.kt:381 |
| 오전 오후 | S0 오전/오후 휠 | 화면 읽기용 이름 | `edit_period` (edit:8)<br>쓰는 곳: EditScreen.kt:445 |
| 시 | S0 시 휠 | 화면 읽기용 이름 | `edit_hour` (edit:9)<br>쓰는 곳: EditScreen.kt:455, EditScreen.kt:463 |
| 분 | S0 분 휠 | 화면 읽기용 이름 | `edit_minute` (edit:10)<br>쓰는 곳: EditScreen.kt:498, EditScreen.kt:506 |
| : | S0 시와 분 사이 | 글자(쌍점) | `edit_colon` (edit:11)<br>쓰는 곳: EditScreen.kt:477 |
| 반복 | S0 반복 카드 | 카드 제목 | `edit_card_repeat` (edit:13)<br>쓰는 곳: EditScreen.kt:518 |
| %1$s요일 | S0 요일 칸 하나. 예: '월요일' | 화면 읽기용 | `edit_day_description` (edit:17)<br>쓰는 곳: EditScreen.kt:529 |
| 공휴일에는 끄기 | S0 반복 카드 안 | 스위치 줄 이름 | `edit_skip_holidays` (edit:14)<br>쓰는 곳: EditScreen.kt:534 |
| 대체공휴일 포함 | 같은 줄 아래 | 스위치 줄 설명 | `edit_skip_holidays_value` (edit:15)<br>쓰는 곳: EditScreen.kt:535 |
| 기상 미션 | S0 미션 카드 | 카드 제목 | `edit_card_mission` (edit:19)<br>쓰는 곳: EditScreen.kt:549 |
| 미션 추가 | S0 미션 카드 빈 칸, S1 미션 선택 | 빈 칸 글 | `edit_mission_add_title` (edit:22)<br>쓰는 곳: EditMissionSlots.kt:147, EditMissionSlots.kt:174 |
| %1$s 빼기 | S0 고른 미션 칸의 빼기 아이콘. 예: 'Drop Words 빼기' | 화면 읽기용 | `edit_mission_remove` (edit:23)<br>쓰는 곳: EditMissionSlots.kt:117 |
| 다시 울림 | S0 미션 카드 줄, S3 다시 울림 시트와 세부 화면 | 줄 이름, 시트 제목 | `edit_ring_again` (edit:24)<br>쓰는 곳: DetailPages.kt:123, EditScreen.kt:561, EditSheets.kt:211 |
| %1$d분, %2$d번 | S0 다시 울림 줄 값. 예: '5분, 3번' | 줄 값 | `edit_ring_again_value` (edit:25)<br>쓰는 곳: EditScreen.kt:565 |
| %1$d분, 무제한 | S0 다시 울림 줄 값, 횟수 무제한일 때. 예: '5분, 무제한' | 줄 값 | `edit_ring_again_until_off` (edit:26)<br>쓰는 곳: EditScreen.kt:564 |
| 미루기 | S0 미루기 줄, 미루기 시트 | 줄 이름, 시트 제목 | `edit_snooze` (edit:36)<br>쓰는 곳: EditScreen.kt:578, EditSheets.kt:276 |
| %1$d분, %2$d번 | S0 미루기 줄 값. 예: '5분, 3번' | 줄 값 | `edit_snooze_value` (edit:37)<br>쓰는 곳: EditScreen.kt:580 |
| 다시 잠들기 방지 | S0 다시 잠들기 방지 줄, S4 시트 | 줄 이름, 시트 제목 | `edit_wake_check` (edit:27)<br>쓰는 곳: EditScreen.kt:594, EditSheets.kt:250 |
| %1$d분 후, 횟수 %2$d번 | S0 다시 잠들기 방지 줄 값. 예: '3분 후, 횟수 1번' | 줄 값 | `edit_wake_check_value` (edit:39)<br>쓰는 곳: EditSheets.kt:328 |
| %1$d분 후 | 같은 줄, 재확인 없음. 예: '3분 후' | 줄 값 | `edit_wake_check_value_no_recheck` (edit:40)<br>쓰는 곳: EditSheets.kt:330 |
| 사용 안 함 | S0 꺼진 줄의 값, S3/S6/S7/S8 '사용 안 함' 칩, S1 미션 없음 | 줄 값, 선택 칩 | `edit_off_value` (edit:183)<br>쓰는 곳: BackgroundPage.kt:160, DetailPages.kt:129, DetailPages.kt:264, DetailPages.kt:293, EditScreen.kt:631, MissionPickPage.kt:113 |
| 알람 사운드 | S0 소리 카드 | 카드 제목 | `edit_card_sound` (edit:44)<br>쓰는 곳: EditScreen.kt:609 |
| 알람음 | S0 알람음 줄, S5 알람음 화면 | 줄 이름, 화면 제목 | `edit_sound` (edit:45)<br>쓰는 곳: EditScreen.kt:612, SoundPage.kt:94 |
| 볼륨 | S0 볼륨 줄, S6 볼륨 시트 | 줄 이름, 시트 제목 | `edit_volume_title` (edit:148)<br>쓰는 곳: EditScreen.kt:620, EditSheets.kt:116 |
| 알람 화면 배경 | S0 배경 줄, S8 배경 화면 | 화면 제목 | `edit_background` (edit:51)<br>쓰는 곳: BackgroundPage.kt:129 |
| 저장 | S0 아래 | 버튼 | `edit_save` (edit:53)<br>쓰는 곳: EditScreen.kt:259 |
| 지금 울리는 알람이에요. 울림 화면에서 해제해 주세요 | 울리는 중인 알람을 편집하려 할 때 | 토스트 | `edit_ringing_blocked` (edit:54)<br>쓰는 곳: EditScreen.kt:369 |
| 미리 해보기 | (쓰는 곳 없음) | 줄 이름 | `edit_preview` (edit:21)<br>쓰는 곳: 없음 |
| %1$d분 | (쓰는 곳 없음) | 값 | `edit_minutes` (edit:33)<br>쓰는 곳: 없음 |
| 전원 종료 방지 | (쓰는 곳 없음, 설정으로 옮김) | 줄 이름 | `edit_power_off` (edit:41)<br>쓰는 곳: 없음 |
| 해제할 때까지 | (쓰는 곳 없음) | 줄 값 | `edit_power_off_value` (edit:42)<br>쓰는 곳: 없음 |
| 볼륨 및 진동 | (쓰는 곳 없음) | 줄 이름 | `edit_volume` (edit:46)<br>쓰는 곳: 없음 |
| %1$d%, %2$s | (쓰는 곳 없음) 예: '80%, 기본 진동' | 줄 값 | `edit_volume_vibrate` (edit:47)<br>쓰는 곳: 없음 |
| %1$d%, 진동 없음 | (쓰는 곳 없음) 예: '80%, 진동 없음' | 줄 값 | `edit_volume_no_vibrate` (edit:48)<br>쓰는 곳: 없음 |
| %1$s 동안 | (쓰는 곳 없음) 예: '30초 동안' | 줄 값 | `edit_gentle_value` (edit:50)<br>쓰는 곳: 없음 |

### S0 경고 대화상자와 나가기

| 문구 | 어떤 상황에 보이나 | 보이는 방식 | 리소스 키와 위치 |
|---|---|---|---|
| 이대로는 해제 화면이 안 떠요 | S0-2 저장할 때 알림과 다른 앱 위에 표시가 꺼져 있으면 | 대화상자 제목 | `edit_warn_dismiss_title` (edit:171)<br>쓰는 곳: EditScreen.kt:646 |
| 알림과 '다른 앱 위에 표시'가 꺼져 있어 알람이 울려도 소리만 나요. | 같은 대화상자 | 대화상자 본문 | `edit_warn_dismiss_message` (edit:172)<br>쓰는 곳: EditScreen.kt:647 |
| 그래도 저장 | 같은 대화상자 | 버튼 | `edit_warn_dismiss_save` (edit:173)<br>쓰는 곳: EditScreen.kt:648 |
| 권한 켜기 | 같은 대화상자 | 버튼 | `edit_warn_dismiss_fix` (edit:174)<br>쓰는 곳: EditScreen.kt:649 |
| 방해 금지 모드가 소리를 막고 있어요 | S0-3 저장할 때 방해 금지 모드가 알람을 막고 있으면 | 대화상자 제목 | `edit_warn_silenced_title` (edit:175)<br>쓰는 곳: EditScreen.kt:640 |
| 방해 금지 모드에서 '알람'이 꺼져 있어 알람이 소리 없이 울릴 수 있어요. | 같은 대화상자 | 대화상자 본문 | `edit_warn_silenced_message` (edit:176)<br>쓰는 곳: EditScreen.kt:641 |
| 괜찮아요 | 같은 대화상자 | 버튼 | `edit_warn_silenced_save` (edit:177)<br>쓰는 곳: EditScreen.kt:642 |
| 설정 열기 | 같은 대화상자 | 버튼 | `edit_warn_silenced_fix` (edit:178)<br>쓰는 곳: EditScreen.kt:643 |
| 저장하지 않고 나갈까요? | 바꾼 내용이 있는데 뒤로 갈 때 | 대화상자 제목 | `edit_exit_title` (edit:179)<br>쓰는 곳: EditScreen.kt:273 |
| 지금 나가면<br>바꾼 내용이 저장되지 않아요. | 같은 대화상자 | 대화상자 본문 | `edit_exit_message` (edit:180)<br>쓰는 곳: EditScreen.kt:274 |
| 계속하기 | 같은 대화상자 | 버튼 | `edit_exit_keep` (edit:181)<br>쓰는 곳: EditScreen.kt:275 |
| 나가기 | 같은 대화상자 | 버튼 | `edit_exit_leave` (edit:182)<br>쓰는 곳: EditScreen.kt:276 |

### S1 미션 선택과 미션 설정 시트

| 문구 | 어떤 상황에 보이나 | 보이는 방식 | 리소스 키와 위치 |
|---|---|---|---|
| 미션 선택 | S1 미션 선택 화면 | 화면 제목 | `edit_mission` (edit:20)<br>쓰는 곳: MissionPickPage.kt:96 |
| 비처럼 쏟아지는 단어를 캐치해요 | S1-2 Drop Words 카드 | 카드 설명 | `edit_mission_desc_drop_words` (edit:57)<br>쓰는 곳: MissionPickPage.kt:409 |
| 다양한 분야의 랜덤 퀴즈 | S1-2 Pop Quiz 카드 | 카드 설명 | `edit_mission_desc_pop_quiz` (edit:58)<br>쓰는 곳: MissionPickPage.kt:410 |
| 수학 문제를 맞혀요 | S1-2 Quick Math 카드 | 카드 설명 | `edit_mission_desc_quick_math` (edit:59)<br>쓰는 곳: MissionPickPage.kt:411 |
| 스마트폰을 흔들어주세요 | S1-2 Shake Shake 카드 | 카드 설명 | `edit_mission_desc_shake_shake` (edit:60)<br>쓰는 곳: MissionPickPage.kt:412 |
| %1$d단계, 속도 %2$d, %3$d문제 | 고른 Drop Words 카드 설명. 예: '1단계, 속도 1, 10문제' | 카드 설명 | `edit_mission_value_drop_words` (edit:62)<br>쓰는 곳: MissionPickPage.kt:190 |
| %1$d단계, %2$d문제 | 고른 Pop Quiz, Quick Math 카드 설명. 예: '1단계, 3문제' | 카드 설명 | `edit_mission_value_questions` (edit:63)<br>쓰는 곳: MissionPickPage.kt:196, MissionPickPage.kt:205 |
| %1$d단계, %2$d문제, %3$s | 고른 Pop Quiz, 분야가 전체가 아닐 때. 예: '1단계, 3문제, 과학 용어 외 1개' | 카드 설명 | `edit_mission_value_questions_category` (edit:66)<br>쓰는 곳: MissionPickPage.kt:199 |
| %1$d번 | 고른 Shake Shake 카드 설명. 예: '30번' | 카드 설명 | `edit_mission_value_shakes` (edit:64)<br>쓰는 곳: MissionPickPage.kt:206 |
| 난이도 | S1-3~S1-7 미션 설정 시트 | 줄 이름 | `edit_mission_level` (edit:69)<br>쓰는 곳: MissionPickPage.kt:218 |
| %1$d단계 | 같은 줄 값. 예: '3단계' | 줄 값 | `edit_mission_level_value` (edit:70)<br>쓰는 곳: MissionPickPage.kt:246 |
| 속도 | Drop Words 설정 시트 | 줄 이름 | `edit_mission_speed` (edit:71)<br>쓰는 곳: MissionPickPage.kt:258 |
| 바닥까지 %1$d초 | (쓰는 곳 없음) 예: '바닥까지 8초' | 줄 설명 | `edit_mission_speed_sub` (edit:72)<br>쓰는 곳: 없음 |
| 문제 수 | 미션 설정 시트(Drop Words, Pop Quiz, Quick Math) | 줄 이름 | `edit_mission_questions` (edit:73)<br>쓰는 곳: MissionPickPage.kt:416 |
| %1$d문제 | 같은 줄 값. 예: '10문제' | 줄 값 | `edit_mission_questions_value` (edit:74)<br>쓰는 곳: MissionPickPage.kt:421 |
| 흔드는 횟수 | Shake Shake 설정 시트 | 줄 이름 | `edit_mission_shakes` (edit:75)<br>쓰는 곳: MissionPickPage.kt:417 |
| %1$d번 | 같은 줄 값. 예: '30번' | 줄 값 | `edit_mission_shakes_value` (edit:76)<br>쓰는 곳: MissionPickPage.kt:422 |
| 분야 | Pop Quiz 설정 시트 분야 줄 | 줄 이름 | `edit_mission_category` (edit:78)<br>쓰는 곳: MissionPickPage.kt:342 |
| 전체 | 분야 줄 값, 칩 첫 칸 | 줄 값, 선택 칩 | `edit_mission_category_all` (edit:79)<br>쓰는 곳: MissionPickPage.kt:349, MissionPickPage.kt:363 |
| %1$d개 골랐어요 | 분야를 몇 개 골랐을 때. 예: '2개 골랐어요' | 줄 값 | `edit_mission_category_count` (edit:80)<br>쓰는 곳: MissionPickPage.kt:351 |
| 분야를 하나 이상 골라 주세요 | 분야를 모두 빼려 할 때 | 토스트 | `edit_mission_category_min` (edit:81)<br>쓰는 곳: MissionPickPage.kt:388 |
| %1$s 낮추기 | 값 왼쪽 빼기 버튼(미션 시트, 미루기 시트). 예: '난이도 낮추기' | 화면 읽기용 | `edit_mission_decrease` (edit:83)<br>쓰는 곳: EditSheets.kt:293, EditSheets.kt:315, MissionPickPage.kt:252, MissionPickPage.kt:267, MissionPickPage.kt:281 |
| %1$s 높이기 | 값 오른쪽 더하기 버튼. 예: '난이도 높이기' | 화면 읽기용 | `edit_mission_increase` (edit:84)<br>쓰는 곳: EditSheets.kt:294, EditSheets.kt:316, MissionPickPage.kt:253, MissionPickPage.kt:268, MissionPickPage.kt:282 |
| 미리보기 | 미션 설정 시트 아래 | 버튼 | `edit_mission_preview` (edit:85)<br>쓰는 곳: MissionPickPage.kt:299 |
| 선택 완료 | 미션 설정 시트 아래, 새로 고를 때 | 버튼 | `edit_mission_select` (edit:86)<br>쓰는 곳: MissionPickPage.kt:313 |
| 선택 해제 | 미션 설정 시트 아래, 이미 고른 미션일 때 | 버튼 | `edit_mission_deselect` (edit:90)<br>쓰는 곳: MissionPickPage.kt:306 |
| 시작하기 | 미션 확인용 앱의 설정 시트 아래 | 버튼 | `edit_mission_start` (edit:88)<br>쓰는 곳: MissionPickPage.kt:291 |
| 미션 | 미션 확인용 앱 첫 화면 | 화면 제목 | `mission_try_title` (edit:89)<br>쓰는 곳: MissionTryPage.kt:127 |
| 1) 예: 한글을 만든 왕은?<br>2) 예: 물이 끓는 온도는?<br>3) 예: 광합성에 필요한 기체는?<br>4) 예: 물가가 계속 오르는 현상은?<br>5) 예: 임진왜란이 일어난 해는?<br>6) 예: 경기 침체 속 물가 상승은?<br>7) 예: 30년 전쟁을 끝낸 조약은? | 미션 설정 시트 예제 그림의 Pop Quiz 문제(단계 1~7 순서) | 예제 그림 속 글 | `edit_mission_examples_pop_quiz` (edit:100)<br>쓰는 곳: MissionExample.kt:164 |
| 1) 세종대왕\|태조\|정조\|영조<br>2) 100°C\|90°C\|80°C\|120°C<br>3) 이산화탄소\|산소\|질소\|수소<br>4) 인플레이션\|디플레이션\|리세션\|디폴트<br>5) 1592년\|1636년\|1392년\|1910년<br>6) 스태그플레이션\|인플레이션\|디플레이션\|리세션<br>7) 베스트팔렌\|베르사유\|파리\|빈 | 같은 예제 그림의 보기 4개(단계별, 맨 앞이 정답) | 예제 그림 속 글 | `edit_mission_example_quiz_options` (edit:110)<br>쓰는 곳: MissionExample.kt:165 |
| 1) 사과\|나무\|바다<br>2) 운동화\|자동차\|냉장고<br>3) 닭볶음\|깻잎전\|볶음밥<br>4) 일석이조\|우공이산\|동문서답<br>5) 고혈압\|소멸시효\|당뇨병<br>6) 읊조리다\|넓적다리\|굵은소금<br>7) 탄산수소나트륨\|수산화나트륨\|과산화수소 | 미션 설정 시트 예제 그림에 떨어지는 단어(단계별 3개) | 예제 그림 속 글 | `edit_mission_example_drop_words` (edit:120)<br>쓰는 곳: MissionExample.kt:223 |
| 1) 예: 7 + 5<br>2) 예: 47 + 38<br>3) 예: 24 × 7<br>4) 예: (24 × 7) + 58<br>5) 예: 38 × 26<br>6) 예: (38 × 26) + 457<br>7) 예: (167 × 37) + 1872 | 미션 설정 시트 예제 그림의 Quick Math 문제(단계 1~7) | 예제 그림 속 글 | `edit_mission_examples_quick_math` (edit:129)<br>쓰는 곳: MissionExample.kt:103 |
| 1) 예: 사과, 나무<br>2) 예: 운동화, 자동차<br>3) 예: 닭볶음, 깻잎전<br>4) 예: 일석이조, 우공이산<br>5) 예: 고혈압, 소멸시효<br>6) 예: 읊조리다, 넓적다리<br>7) 예: 탄산수소나트륨 | (쓰는 곳 없음) | 예제 글 | `edit_mission_examples_drop_words` (edit:91)<br>쓰는 곳: 없음 |

### S3 다시 울림

| 문구 | 어떤 상황에 보이나 | 보이는 방식 | 리소스 키와 위치 |
|---|---|---|---|
| 해제하지 않고 두면 저절로 다시 울려요 | S3 다시 울림 시트 제목 아래 | 시트 설명 | `edit_ring_again_sheet_desc` (edit:152)<br>쓰는 곳: EditSheets.kt:212 |
| 간격 | S3 시트와 세부 화면의 간격 줄 | 줄 이름 | `edit_ring_again_interval_title` (edit:140)<br>쓰는 곳: DetailPages.kt:128, EditSheets.kt:216 |
| %1$d분 | 간격 칩. 예: '5분' | 선택 칩 | `edit_ring_again_interval_option` (edit:141)<br>쓰는 곳: DetailPages.kt:132, EditSheets.kt:219 |
| 횟수 | S3 시트와 세부 화면의 횟수 줄 | 줄 이름 | `edit_ring_again_count_title` (edit:142)<br>쓰는 곳: DetailPages.kt:139, EditSheets.kt:223 |
| %1$d번 | S3 시트 횟수 칩. 예: '3번' | 선택 칩 | `edit_ring_again_count_sheet` (edit:153)<br>쓰는 곳: EditSheets.kt:230 |
| 무제한 | S3 시트 횟수 칩 마지막 | 선택 칩 | `edit_ring_again_unlimited` (edit:154)<br>쓰는 곳: EditSheets.kt:228 |
| %1$d번 | S3 세부 화면 횟수 칩. 예: '3번' | 선택 칩 | `edit_ring_again_count_option` (edit:143)<br>쓰는 곳: DetailPages.kt:147 |
| 해제할 때까지 | S3 세부 화면 횟수 칩 마지막 | 선택 칩 | `edit_ring_again_count_until_off` (edit:144)<br>쓰는 곳: DetailPages.kt:145 |
| 다시 울릴 때도 미션을 풀어야 해제돼요 | S3 세부 화면 아래 | 도움말 | `edit_ring_again_help` (edit:145)<br>쓰는 곳: DetailPages.kt:125 |

### S4 다시 잠들기 방지와 미루기 시트

| 문구 | 어떤 상황에 보이나 | 보이는 방식 | 리소스 키와 위치 |
|---|---|---|---|
| 알람 해제 후 재수면 여부를 재확인해요 | S4 시트 제목 아래 | 시트 설명 | `edit_wake_check_sheet_desc` (edit:29)<br>쓰는 곳: EditSheets.kt:251 |
| 간격 | S4 시트 첫 줄, 미루기 시트 첫 줄 | 줄 이름 | `edit_wake_check_ask` (edit:30)<br>쓰는 곳: EditSheets.kt:255, EditSheets.kt:278 |
| %1$d분 후 | S4와 미루기 시트 간격 값. 예: '3분 후' | 줄 값 | `edit_minutes_after` (edit:32)<br>쓰는 곳: EditSheets.kt:258, EditSheets.kt:281 |
| 횟수 | S4와 미루기 시트 둘째 줄 | 줄 이름 | `edit_recheck` (edit:31)<br>쓰는 곳: EditSheets.kt:284, EditSheets.kt:306 |
| %1$d번 | 같은 줄 값. 예: '1번' | 줄 값 | `edit_times` (edit:34)<br>쓰는 곳: EditSheets.kt:287, EditSheets.kt:309 |
| 완료 | S3~S6 시트 아래 | 버튼 | `edit_done` (edit:151)<br>쓰는 곳: EditSheets.kt:68 |

### S5~S7 소리

| 문구 | 어떤 상황에 보이나 | 보이는 방식 | 리소스 키와 위치 |
|---|---|---|---|
| 진동 | S6 볼륨 시트 진동 줄, 진동 세부 화면 | 줄 이름, 화면 제목 | `edit_vibration_title` (edit:156)<br>쓰는 곳: DetailPages.kt:262, EditSheets.kt:128, EditSheets.kt:157 |
| 진동 모양 | 진동 세부 화면 | 카드 제목 | `edit_vibration_pattern_title` (edit:184)<br>쓰는 곳: DetailPages.kt:263 |
| 기본 진동 | 진동 모양 칩 | 선택 칩 | `edit_vibration_basic` (edit:157)<br>쓰는 곳: DetailPages.kt:307 |
| 짧게 여러 번 | 진동 모양 칩 | 선택 칩 | `edit_vibration_short` (edit:158)<br>쓰는 곳: DetailPages.kt:308 |
| 길게 한 번 | 진동 모양 칩 | 선택 칩 | `edit_vibration_long` (edit:159)<br>쓰는 곳: DetailPages.kt:309 |
| %1$d% | S6 볼륨 막대 값. 예: '80%' | 값, 화면 읽기용 | `edit_volume_percent` (edit:155)<br>쓰는 곳: DetailPages.kt:165, EditSheets.kt:156 |
| 볼륨 천천히 올리기 | S6 시트의 S7 줄, S7 세부 화면 | 줄 이름, 화면 제목 | `edit_gentle` (edit:49)<br>쓰는 곳: DetailPages.kt:288, EditSheets.kt:135, EditSheets.kt:158 |
| 올라가는 시간 | S7 세부 화면 | 카드 제목 | `edit_gentle_title` (edit:162)<br>쓰는 곳: DetailPages.kt:292 |
| 안 함 | S6 시트의 S7 값, 0초일 때 | 줄 값 | `edit_gentle_none` (edit:149)<br>쓰는 곳: EditSheets.kt:139 |
| %1$d초 | S6 시트의 S7 값. 예: '30초' | 줄 값 | `edit_seconds` (edit:150)<br>쓰는 곳: EditSheets.kt:139 |

### S8 알람 화면 배경

| 문구 | 어떤 상황에 보이나 | 보이는 방식 | 리소스 키와 위치 |
|---|---|---|---|
| 내 사진 추가 | S8 오른쪽 위 사진 아이콘 | 화면 읽기용 이름 | `edit_background_add_photo` (edit:165)<br>쓰는 곳: BackgroundPage.kt:135 |
| 오른쪽 위 사진 버튼으로 갤러리 사진을 더할 수 있어요 | S8 내 사진이 하나도 없을 때 | 안내 글 | `edit_background_photo_empty` (edit:166)<br>쓰는 곳: BackgroundPage.kt:179 |
| 오전 6:30 | S8 미리보기 그림 속 시계 | 미리보기 속 글 | `edit_background_preview_time` (edit:167)<br>쓰는 곳: BackgroundPage.kt:269 |

## 5. 알람 울림 (D)

| 문구 | 어떤 상황에 보이나 | 보이는 방식 | 리소스 키와 위치 |
|---|---|---|---|
| 미션 시작 | D0 미션이 있는 알람 | 큰 버튼 | `ring_start_mission` (ring:4)<br>쓰는 곳: RingScreens.kt:162 |
| 알람 해제 | D0 미션 없는 알람 | 큰 버튼 | `ring_dismiss` (ring:5)<br>쓰는 곳: RingScreens.kt:162 |
| %1$d분 뒤 다시 | D0 미루기를 켠 알람. 예: '5분 뒤 다시' | 글자 버튼 | `ring_snooze` (ring:7)<br>쓰는 곳: RingScreens.kt:215 |

## 6. 미션 (M)

| 문구 | 어떤 상황에 보이나 | 보이는 방식 | 리소스 키와 위치 |
|---|---|---|---|
| 남은 시간 %1$d초 | M1~M4 위쪽 남은 시간 막대. 예: '남은 시간 42초' | 화면 읽기용 | `mission_timer_description` (mission:4)<br>쓰는 곳: MissionFrame.kt:200, MissionFrame.kt:313 |
| 다음 미션 %1$d / %2$d | 미션이 여러 개일 때 다음 미션으로 넘어가는 화면. 예: '다음 미션 2 / 3' | 가운데 글 | `mission_next` (mission:18)<br>쓰는 곳: MissionHostScreen.kt:154 |
| 정답 %1$d / %2$d | M1~M3 맞힌 수 표시. 예: '정답 3 / 5' | 상태 글 | `mission_correct` (mission:7)<br>쓰는 곳: MissionHostScreen.kt:226, MissionHostScreen.kt:227, MissionHostScreen.kt:228 |
| 지우기 | M1 글자 자판, M3 숫자 자판의 지우기 키 | 화면 읽기용 | `mission_math_delete` (mission:8)<br>쓰는 곳: DropWordsContent.kt:275, QuickMathScreen.kt:177 |
| 소수점 | M3 숫자 자판의 소수점 키 | 화면 읽기용 | `mission_math_dot` (mission:9)<br>쓰는 곳: QuickMathScreen.kt:176 |
| 확인 | M1, M3 입력 확인 키 | 화면 읽기용 | `mission_math_confirm` (mission:10)<br>쓰는 곳: MissionParts.kt:185, QuickMathScreen.kt:178 |
| 입력한 답 %1$s | M1, M3 입력 칸. 예: '입력한 답 12' | 화면 읽기용 | `mission_math_input_description` (mission:13)<br>쓰는 곳: MissionParts.kt:140 |
| 틀렸어요 | M2-1 Pop Quiz 오답 | 큰 글 | `mission_wrong_title` (mission:11)<br>쓰는 곳: PopQuizContent.kt:170 |
| 괜찮아요, 새 문제가 나와요 | M2-1 오답 큰 글 아래 | 보조 글 | `mission_wrong_sub` (mission:12)<br>쓰는 곳: PopQuizContent.kt:176 |
| 스마트폰을 흔들어주세요 | M4 Shake Shake 가운데 | 안내 글 | `mission_shake_guide` (mission:25)<br>쓰는 곳: ShakeShakeContent.kt:102 |
| 쏟아지는 단어를 입력해요 | M1 Drop Words 입력 칸이 비어 있을 때 | 입력 칸 힌트 | `mission_drop_placeholder` (mission:26)<br>쓰는 곳: DropWordsContent.kt:160 |
| 미리보기 닫기 | 미리보기로 미션을 할 때 오른쪽 위 닫기 | 화면 읽기용 이름 | `mission_preview_close` (mission:16)<br>쓰는 곳: MissionFrame.kt:220 |
| 잘했어요 | 미리보기 미션을 마쳤을 때 | 가운데 글 | `mission_preview_done` (mission:17)<br>쓰는 곳: MissionHostScreen.kt:156 |
| 문장을 그대로 따라 쓰면 알람이 해제돼요 | M5 미션 포기 화면 위쪽 | 안내 글 | `escape_screen_guide` (mission:29)<br>쓰는 곳: EscapeScreen.kt:169 |
| %1$d초 안에 쓰지 않으면 미션으로 돌아가요 | M5 손을 놓고 있을 때. 예: '10초 안에 쓰지 않으면 미션으로 돌아가요' | 안내 글 | `escape_screen_idle` (mission:28)<br>쓰는 곳: EscapeScreen.kt:167 |
| 따라 쓰는 칸 | M5 따라 쓰는 칸 | 화면 읽기용 | `escape_screen_input` (mission:31)<br>쓰는 곳: EscapeScreen.kt:250 |
| 다시 미션 수행하기 | M5 아래 | 버튼 | `escape_screen_retry` (mission:30)<br>쓰는 곳: EscapeScreen.kt:210 |

## 7. 알람이 울린 뒤 (W)

| 문구 | 어떤 상황에 보이나 | 보이는 방식 | 리소스 키와 위치 |
|---|---|---|---|
| 좋은 아침이에요 | W1 해제 완료, 아침 시간대 | 큰 제목 | `ring_wake_up_title_morning` (ring:10)<br>쓰는 곳: RingScreens.kt:261 |
| 잘 일어났어요 | W1 해제 완료, 밤 시간대 | 큰 제목 | `ring_wake_up_title_night` (ring:11)<br>쓰는 곳: RingScreens.kt:261 |
| 물 한 잔 마시고<br>천천히 하루를 시작해요 | W1 날씨를 모를 때, 아침 | 본문(두 줄) | `ring_wake_up_message_morning` (ring:12)<br>쓰는 곳: RingScreens.kt:299 |
| 물 한 잔 마시고<br>천천히 몸을 깨워 보세요 | W1 날씨를 모를 때, 밤 | 본문(두 줄) | `ring_wake_up_message_night` (ring:13)<br>쓰는 곳: RingScreens.kt:299 |
| 밖에 비가 와요<br>나갈 때 우산 챙기세요 | W1 비 올 때 | 본문(두 줄) | `ring_wake_up_weather_rain` (ring:14)<br>쓰는 곳: RingScreens.kt:296 |
| 밖에 눈이 와요<br>길이 미끄러우니 천천히 걸어요 | W1 눈 올 때 | 본문(두 줄) | `ring_wake_up_weather_snow` (ring:15)<br>쓰는 곳: RingScreens.kt:297 |
| 오늘은 하늘이 맑아요<br>창문을 열고 바깥 공기를 느껴 봐요 | W1 맑을 때 | 본문(두 줄) | `ring_wake_up_weather_clear` (ring:16)<br>쓰는 곳: RingScreens.kt:298 |
| 확인 | W1 아래 | 버튼 | `ring_wake_up_ok` (ring:17)<br>쓰는 곳: RingScreens.kt:285 |
| 일어나셨죠? | W2 깨어 있나요 | 큰 제목 | `ring_wake_check_title` (ring:20)<br>쓰는 곳: RingScreens.kt:376 |
| 아직 깨어 계시죠? | W3 정말 깼어요 | 큰 제목 | `ring_recheck_title` (ring:27)<br>쓰는 곳: RingScreens.kt:376 |
| 버튼을 10초간 눌러주세요 | W2, W3 제목 아래 | 안내 글 | `ring_hold_guide` (ring:30)<br>쓰는 곳: RingScreens.kt:382 |
| 끝까지 누르고 있어야 해요 | W2, W3 10초 전에 손을 뗐을 때 | 안내 글(바뀜) | `ring_hold_released_early` (ring:31)<br>쓰는 곳: RingScreens.kt:382 |
| 10초간 누르기 | W2, W3 길게 누르는 버튼(톡백은 두 번 탭) | 화면 읽기용 | `ring_hold_action` (ring:36)<br>쓰는 곳: HoldButton.kt:101 |
| %1$d초 남았어요 | 누르는 동안 버튼 상태. 예: '7초 남았어요' | 화면 읽기용 | `ring_hold_seconds_left` (ring:37)<br>쓰는 곳: HoldButton.kt:104 |
| 일어났어요 | W2, W3 길게 누르는 버튼 | 버튼 글 | `ring_wake_check_awake` (ring:21)<br>쓰는 곳: RingScreens.kt:416 |
| 시간 안에 누르지 않으면 알람이 다시 울려요 | W2, W3 아래 | 안내 글 | `ring_check_ring_again` (ring:34)<br>쓰는 곳: RingScreens.kt:398 |
| 재확인 해제 후에는 이번 알람을 다시 묻지 않아요 | W2, W3 위쪽 닫기 옆 | 안내 글 | `ring_release_note` (ring:32)<br>쓰는 곳: RingScreens.kt:408 |
| 닫기 | W2, W3 위쪽 닫기 아이콘(재확인 해제) | 화면 읽기용 이름 | `ring_release_close` (ring:33)<br>쓰는 곳: RingScreens.kt:338 |
| 확인했어요 | W2, W3를 마쳤을 때 | 가운데 글 | `ring_wake_check_done` (ring:22)<br>쓰는 곳: RingScreens.kt:428 |
| %1$d분 뒤 재수면 여부를 재확인할게요 | W2를 마친 뒤 W3가 남았을 때. 예: '5분 뒤 재수면 여부를 재확인할게요' | 보조 글 | `ring_wake_check_next` (ring:24)<br>쓰는 곳: RingScreens.kt:435 |
| %1$d번 중 %2$d번째 | 실험실 좀비모드 W3 제목 위. 예: '3번 중 2번째' | 보조 글 | `ring_multi_count` (ring:40)<br>쓰는 곳: RingScreens.kt:368 |
| 마지막이에요 | 좀비모드 마지막 W3 제목 위 | 보조 글 | `ring_multi_last` (ring:41)<br>쓰는 곳: RingScreens.kt:366 |
| 잠시 뒤 재수면 여부를 재확인할게요 | 좀비모드 W3를 마쳤고 더 남았을 때 | 보조 글 | `ring_multi_next` (ring:42)<br>쓰는 곳: RingScreens.kt:433 |
| 오늘 재확인을 다 마쳤어요 | 좀비모드 W3를 모두 마쳤을 때 | 보조 글 | `ring_multi_all_done` (ring:43)<br>쓰는 곳: RingScreens.kt:434 |

## 8. 잠들기 탭 (Z, 지금 숨김)

| 문구 | 어떤 상황에 보이나 | 보이는 방식 | 리소스 키와 위치 |
|---|---|---|---|
| 수면 라운지 | Z 잠들기 화면 맨 위 | 화면 제목 | `sleep_title` (sleep:3)<br>쓰는 곳: SleepScreen.kt:103 |
| 오늘 하루는 여기까지, 불 끄고 쉬어요 | Z 제목 아래 | 보조 글 | `sleep_subtitle` (sleep:4)<br>쓰는 곳: SleepScreen.kt:109 |
| %1$d분 | Z 트랙 타일 길이. 예: '30분' | 타일 보조 글 | `sleep_minutes` (sleep:5)<br>쓰는 곳: SleepScreen.kt:254 |
| %1$s, %2$d분 | Z 재생 화면 트랙 이름 아래. 예: '잠에 들기 어려울 때, 30분' | 보조 글 | `sleep_meta` (sleep:6)<br>쓰는 곳: SleepPlayerScreen.kt:162 |
| 닫기 | Z 재생 화면 닫기 아이콘 | 화면 읽기용 이름 | `sleep_close` (sleep:7)<br>쓰는 곳: SleepPlayerScreen.kt:137 |
| 재생 | Z 재생 화면 재생 버튼, 잠들기 재생 알림 버튼 | 버튼, 알림 버튼 | `sleep_play` (alarm:10)<br>`sleep_play` (sleep:8)<br>쓰는 곳: SleepPlaybackService.kt:105, SleepPlayerScreen.kt:250 |
| 일시정지 | Z 재생 화면 일시정지 버튼, 잠들기 재생 알림 버튼 | 버튼, 알림 버튼 | `sleep_pause` (alarm:11)<br>`sleep_pause` (sleep:9)<br>쓰는 곳: SleepPlaybackService.kt:105, SleepPlayerScreen.kt:250 |
| 종료 타이머 | Z 재생 화면, 타이머가 꺼져 있을 때 | 칩 | `sleep_timer_chip_off` (sleep:10)<br>쓰는 곳: SleepPlayerScreen.kt:262 |
| %1$d분 남음 | Z 재생 화면, 타이머가 돌 때. 예: '25분 남음' | 칩 | `sleep_timer_left` (sleep:11)<br>쓰는 곳: SleepPlayerScreen.kt:265 |
| 종료 타이머 | 종료 타이머 시트 | 시트 제목 | `sleep_timer_sheet_title` (sleep:12)<br>쓰는 곳: SleepPlayerScreen.kt:291 |
| 아침 알람까지 | 종료 타이머 시트 첫 줄 | 선택 줄 | `sleep_timer_option_off` (sleep:13)<br>쓰는 곳: SleepPlayerScreen.kt:301 |
| %1$d분 | 종료 타이머 시트. 예: '30분' | 선택 줄 | `sleep_timer_option` (sleep:14)<br>쓰는 곳: SleepPlayerScreen.kt:303 |

## 9. 설정 탭 (T)

### T 설정 첫 화면

| 문구 | 어떤 상황에 보이나 | 보이는 방식 | 리소스 키와 위치 |
|---|---|---|---|
| 설정 | 설정 탭 맨 위 | 화면 제목 | `settings_title` (settings:3)<br>쓰는 곳: SettingsScreen.kt:184 |
| 뒤로 | 설정의 세부 화면들(권한, 방해 금지 안내, 실험실, FAQ, 라이선스, 문의하기) 뒤로 아이콘 | 화면 읽기용 이름 | `settings_back` (settings:4)<br>쓰는 곳: DndGuidePage.kt:99, InfoPages.kt:71, LabPage.kt:198, PermissionPage.kt:305, SupportPage.kt:181, SupportPage.kt:278 |
| 알람이 잘 울리게 | 설정 첫 묶음 | 묶음 제목 | `settings_card_ringing` (settings:159)<br>쓰는 곳: SettingsScreen.kt:194 |
| 권한 점검 | '알람이 잘 울리게' 묶음 첫 줄 | 줄 이름 | `settings_permissions` (settings:5)<br>쓰는 곳: SettingsScreen.kt:380 |
| 확인이 필요해요 | 권한 점검 줄 값, 꺼진 권한이 있을 때 | 줄 값(강조) | `settings_needs_attention` (settings:16)<br>쓰는 곳: SettingsScreen.kt:381 |
| 방해 금지 모드에서 허용하기 | '알람이 잘 울리게' 묶음 스위치 줄 | 스위치 줄 이름 | `settings_dnd_override` (settings:35)<br>쓰는 곳: SettingsScreen.kt:199 |
| 알람이 울리는 동안만 소리를 내요 | 같은 줄 아래 | 스위치 줄 설명 | `settings_dnd_override_desc` (settings:36)<br>쓰는 곳: SettingsScreen.kt:200 |
| 알람 미리 울려 보기 | '알람이 잘 울리게' 묶음 | 줄 이름 | `settings_alarm_try` (settings:6)<br>쓰는 곳: SettingsScreen.kt:204 |
| 1분 뒤 | 같은 줄 오른쪽 | 줄 값 | `settings_alarm_try_value` (settings:7)<br>쓰는 곳: SettingsScreen.kt:205 |
| %1$s에 울려요 | 알람 미리 울려 보기를 눌렀을 때. 예: '06:31에 울려요' | 토스트 | `settings_alarm_try_done` (settings:8)<br>쓰는 곳: SettingsScreen.kt:290 |
| 알람 | 설정 둘째 묶음 | 묶음 제목 | `settings_card_alarm` (settings:160)<br>쓰는 곳: SettingsScreen.kt:211 |
| 한 주 시작 요일 | 알람 묶음 줄, T1-3 시트 | 줄 이름, 시트 제목 | `settings_week_start` (settings:166)<br>쓰는 곳: SettingsScreen.kt:215, SettingsScreen.kt:326 |
| 일요일 | 한 주 시작 요일 값, T1-3 선택지 | 줄 값, 선택 줄 | `settings_week_start_sun` (settings:167)<br>쓰는 곳: SettingsScreen.kt:217, SettingsScreen.kt:332 |
| 월요일 | 한 주 시작 요일 값, T1-3 선택지 | 줄 값, 선택 줄 | `settings_week_start_mon` (settings:168)<br>쓰는 곳: SettingsScreen.kt:217, SettingsScreen.kt:338 |
| 다음 알람 항상 표시 | 알람 묶음 스위치 줄 | 스위치 줄 이름 | `settings_next_alarm` (settings:33)<br>쓰는 곳: SettingsScreen.kt:224 |
| 시스템 알림 패널에 예정된 다음 알람을 보여줘요 | 같은 줄 아래 | 스위치 줄 설명 | `settings_next_alarm_desc` (settings:34)<br>쓰는 곳: SettingsScreen.kt:225 |
| 소리 | 설정 셋째 묶음 | 묶음 제목 | `settings_card_sound` (settings:161)<br>쓰는 곳: SettingsScreen.kt:232 |
| 이어폰, 블루투스로 울리기 | 소리 묶음 스위치 줄 | 스위치 줄 이름 | `settings_bt_output` (settings:169)<br>쓰는 곳: SettingsScreen.kt:235 |
| 연결이 끊기면 폰 스피커로 울려요 | 같은 줄 아래 | 스위치 줄 설명 | `settings_bt_output_desc` (settings:170)<br>쓰는 곳: SettingsScreen.kt:236 |
| 이어폰이나 블루투스로 울릴까요? | T1-4 이어폰, 블루투스로 울리기를 켤 때 | 시트 제목 | `settings_bt_sheet_title` (settings:171)<br>쓰는 곳: SettingsScreen.kt:353 |
| 이어폰을 빼고 잠들면 알람을 못 들을 수 있어요.<br>연결이 끊겨 있으면 폰 스피커로 울려요. | 같은 시트 | 시트 본문 | `settings_bt_sheet_msg` (settings:172)<br>쓰는 곳: SettingsScreen.kt:354 |
| 켜기 | 같은 시트 | 버튼 | `settings_bt_sheet_on` (settings:173)<br>쓰는 곳: SettingsScreen.kt:355 |
| 취소 | 같은 시트 | 버튼 | `settings_bt_sheet_cancel` (settings:174)<br>쓰는 곳: SettingsScreen.kt:357 |
| 미션 중 | 설정 넷째 묶음 | 묶음 제목 | `settings_card_mission` (settings:162)<br>쓰는 곳: SettingsScreen.kt:243 |
| 전원 종료 방지 | 미션 중 묶음 스위치 줄 | 스위치 줄 이름 | `settings_power_guard` (settings:30)<br>쓰는 곳: SettingsScreen.kt:246 |
| 알람이 울리는 동안 전원 메뉴를 막아요 | 같은 줄 아래 | 스위치 줄 설명 | `settings_power_guard_desc` (settings:31)<br>쓰는 곳: SettingsScreen.kt:247 |
| 화면 | 설정 다섯째 묶음 | 묶음 제목 | `settings_card_screen` (settings:163)<br>쓰는 곳: SettingsScreen.kt:254 |
| 배경 애니메이션 | 화면 묶음 스위치 줄 | 스위치 줄 이름 | `settings_weather_motion` (settings:9)<br>쓰는 곳: WeatherSettings.kt:74 |
| 날씨에 따라 앱 화면에도 적용돼요 | 같은 줄 아래 | 스위치 줄 설명 | `settings_weather_motion_value` (settings:10)<br>쓰는 곳: WeatherSettings.kt:75 |
| 위치 권한을 허용해 주세요 | P5 배경 애니메이션을 켤 때 위치 권한이 없으면 | 대화상자 제목 | `ask_location` (settings:11)<br>쓰는 곳: WeatherSettings.kt:92 |
| 허용하지 않으면<br>날씨 애니메이션이 반영되지 않아요. | 같은 대화상자 | 대화상자 본문 | `ask_location_msg` (settings:12)<br>쓰는 곳: WeatherSettings.kt:93 |
| 도움 | 설정 여섯째 묶음 | 묶음 제목 | `settings_card_help` (settings:164)<br>쓰는 곳: SettingsScreen.kt:257 |
| 자주 묻는 질문 | 도움 묶음 줄, T6 화면 | 줄 이름, 화면 제목 | `settings_faq` (settings:109)<br>쓰는 곳: InfoPages.kt:173, SettingsScreen.kt:260 |
| 문의하기 | 도움 묶음 줄 | 줄 이름 | `settings_support` (settings:14)<br>쓰는 곳: SettingsScreen.kt:261 |
| 앱 정보 | 설정 마지막 묶음 | 묶음 제목 | `settings_card_info` (settings:165)<br>쓰는 곳: SettingsScreen.kt:265 |
| 버전 | 앱 정보 묶음 줄(값은 버전 번호) | 줄 이름 | `settings_version` (settings:15)<br>쓰는 곳: SettingsScreen.kt:268 |
| 오픈소스 라이선스 | 앱 정보 묶음 줄, T5 화면 | 줄 이름, 화면 제목 | `settings_licenses` (settings:110)<br>쓰는 곳: InfoPages.kt:92, SettingsScreen.kt:269 |
| 사용한 오픈소스 | T5 오픈소스 라이선스 화면 | 카드 제목 | `licenses_card` (settings:111)<br>쓰는 곳: InfoPages.kt:101 |
| 개인정보 처리방침 | 앱 정보 묶음 줄 | 줄 이름 | `settings_privacy` (settings:175)<br>쓰는 곳: SettingsScreen.kt:272 |

### T2 권한 화면

| 문구 | 어떤 상황에 보이나 | 보이는 방식 | 리소스 키와 위치 |
|---|---|---|---|
| 권한 | T2 권한 화면 | 화면 제목 | `permission_title` (settings:22)<br>쓰는 곳: PermissionPage.kt:303 |
| 필수 권한 | T2 첫 카드 | 카드 제목 | `permission_required` (settings:23)<br>쓰는 곳: PermissionPage.kt:321 |
| 선택 권한 | T2 둘째 카드 | 카드 제목 | `permission_optional` (settings:24)<br>쓰는 곳: PermissionPage.kt:327 |
| 기타 | T2 셋째 카드 | 카드 제목 | `permission_other` (settings:25)<br>쓰는 곳: PermissionPage.kt:331 |
| 알림 | T2 줄, PL2 시트 줄 | 줄 이름 | `permission_notifications` (settings:48)<br>쓰는 곳: PermissionPage.kt:108, PermissionStepSheet.kt:206 |
| 끄면 알람 화면이 안 떠요 | T2 알림 줄 아래 | 줄 설명 | `permission_notifications_desc` (settings:49)<br>쓰는 곳: PermissionPage.kt:109 |
| 전체 화면 알림 | T2 줄, PL2 시트 줄 | 줄 이름 | `permission_full_screen` (settings:50)<br>쓰는 곳: PermissionPage.kt:115, PermissionStepSheet.kt:207 |
| 잠금 화면 위로 알람을 띄워요 | T2 줄 아래 | 줄 설명 | `permission_full_screen_desc` (settings:51)<br>쓰는 곳: PermissionPage.kt:116 |
| 다른 앱 위에 표시 | T2 줄, PL2 시트 줄 | 줄 이름 | `permission_overlay` (settings:52)<br>쓰는 곳: PermissionPage.kt:122, PermissionStepSheet.kt:208 |
| 쓰던 앱 위로 알람을 띄워요 | T2 줄 아래 | 줄 설명 | `permission_overlay_desc` (settings:53)<br>쓰는 곳: PermissionPage.kt:123 |
| 배터리 사용 제한 없음 | T2 줄 | 줄 이름 | `permission_battery` (settings:40)<br>쓰는 곳: PermissionPage.kt:129 |
| 절전 중에도 제시간에 울려요 | T2 줄 아래 | 줄 설명 | `permission_battery_desc` (settings:41)<br>쓰는 곳: PermissionPage.kt:130 |
| 위치 | T2 줄, PL2 시트 줄 | 줄 이름 | `permission_location` (settings:26)<br>쓰는 곳: PermissionPage.kt:136, PermissionStepSheet.kt:209 |
| 날씨에 맞춰 하늘을 바꿔요 | T2 줄 아래 | 줄 설명 | `permission_location_desc` (settings:27)<br>쓰는 곳: PermissionPage.kt:137 |
| 방해 금지 모드에서 허용 | T2 줄 | 줄 이름 | `permission_dnd` (settings:44)<br>쓰는 곳: PermissionPage.kt:143 |
| 방해 금지 모드에서도 울려요 | T2 줄 아래 | 줄 설명 | `permission_dnd_desc` (settings:45)<br>쓰는 곳: PermissionPage.kt:144 |
| 알람 음량 | T2 줄 | 줄 이름 | `permission_volume` (settings:46)<br>쓰는 곳: PermissionPage.kt:150 |
| 무음 모드여도 소리가 나요 | T2 줄 아래 | 줄 설명 | `permission_volume_desc` (settings:47)<br>쓰는 곳: PermissionPage.kt:151 |
| 정확한 알람 | T2 줄(안드로이드 12만) | 줄 이름 | `permission_exact` (settings:38)<br>쓰는 곳: PermissionPage.kt:157 |
| 정한 시각에 딱 맞춰 울려요 | T2 줄 아래 | 줄 설명 | `permission_exact_desc` (settings:39)<br>쓰는 곳: PermissionPage.kt:158 |
| 전원 종료 방지 | T2 줄 | 줄 이름 | `permission_power` (settings:102)<br>쓰는 곳: PermissionPage.kt:164 |
| 알람이 울리는 동안 전원 메뉴를 막아요 | T2 줄 아래 | 줄 설명 | `permission_power_desc` (settings:103)<br>쓰는 곳: PermissionPage.kt:165 |
| 절전 예외 앱에 추가 | T2 기타 카드 줄(삼성 폰) | 줄 이름 | `permission_samsung_sleep` (settings:42)<br>쓰는 곳: PermissionPage.kt:338 |
| 며칠 안 열어도 잠들지 않아요 | 같은 줄 아래 | 줄 설명 | `permission_samsung_sleep_desc` (settings:43)<br>쓰는 곳: PermissionPage.kt:339 |
| 허용됨 | T2, PL2 줄 오른쪽, 켜졌을 때 | 상태 글 | `permission_granted` (settings:29)<br>쓰는 곳: PermissionPage.kt:382, PermissionStepSheet.kt:309 |
| 허용 필요 | T2, PL2, O5 줄 오른쪽, 꺼졌을 때 | 상태 글 | `permission_not_granted` (settings:37)<br>쓰는 곳: FirstRunPermissions.kt:279, PermissionPage.kt:382, PermissionStepSheet.kt:309 |
| 목록에서 '일어나'를 찾아 켜 주세요 | 폰 설정 목록 화면으로 보낼 때 | 토스트 | `permission_find_in_list` (settings:28)<br>쓰는 곳: PermissionPage.kt:239 |

### PL2 나중용 권한 시트

| 문구 | 어떤 상황에 보이나 | 보이는 방식 | 리소스 키와 위치 |
|---|---|---|---|
| 원활한 앱 사용을 위해<br>아래 권한을 허용해 주세요 | PL2-1 필수 권한 단계 | 시트 제목(두 줄) | `pl_required_title` (settings:78)<br>쓰는 곳: PermissionStepSheet.kt:245 |
| 함께 켜 두면 좋은 권한이에요 | PL2-3 선택 권한 단계 | 시트 제목 | `pl_optional_title` (settings:79)<br>쓰는 곳: PermissionStepSheet.kt:245 |
| 허용하지 않으면 화면 없이<br>소리만 날 수 있어요 | PL2 알림 줄 아래 | 줄 설명(두 줄) | `pl_notifications_desc` (settings:80)<br>쓰는 곳: PermissionStepSheet.kt:206 |
| 허용하지 않으면 잠금 화면에<br>알람이 바로 안 떠요 | PL2 전체 화면 알림 줄 아래 | 줄 설명(두 줄) | `pl_full_screen_desc` (settings:81)<br>쓰는 곳: PermissionStepSheet.kt:207 |
| 허용하지 않으면 홈 버튼에<br>알람 화면이 사라져요 | PL2 다른 앱 위에 표시 줄 아래 | 줄 설명(두 줄) | `pl_overlay_desc` (settings:82)<br>쓰는 곳: PermissionStepSheet.kt:208 |
| 허용하지 않으면 하늘이<br>날씨에 맞춰 바뀌지 않아요 | PL2 위치 줄 아래 | 줄 설명(두 줄) | `pl_location_desc` (settings:83)<br>쓰는 곳: PermissionStepSheet.kt:209 |
| 배터리 사용 제한 해제 | PL2 배터리 줄 | 줄 이름 | `pl_battery` (settings:84)<br>쓰는 곳: PermissionStepSheet.kt:210 |
| 절전 모드에서도<br>알람이 제때 울리게 해요 | PL2 배터리 줄 아래 | 줄 설명(두 줄) | `pl_battery_desc` (settings:85)<br>쓰는 곳: PermissionStepSheet.kt:210 |
| ※ | PL2 배터리 메모 앞 표시 | 기호 | `pl_battery_note_mark` (settings:86)<br>쓰는 곳: PermissionStepSheet.kt:350 |
| 허용하지 않아도 알람은 쓸 수 있지만,<br>절전 기능 때문에 알람이 늦게 울릴 수 있어요. | PL2 배터리 줄 아래 메모 | 메모 글 | `pl_battery_note` (settings:87)<br>쓰는 곳: PermissionStepSheet.kt:355 |
| 다음 | PL2 필수 단계 아래 | 버튼 | `pl_next` (settings:88)<br>쓰는 곳: PermissionStepSheet.kt:281 |
| 완료 | PL2 선택 단계 아래, 설정의 시트들 | 버튼 | `pl_done` (settings:89)<br>쓰는 곳: PermissionStepSheet.kt:289, SettingsSheets.kt:41 |
| 다음에 하기 | PL2 아래 | 글자 버튼 | `pl_later` (settings:90)<br>쓰는 곳: PermissionStepSheet.kt:294 |
| '알림'이 | 토스트 앞부분('알림'이) | 토스트 조각 | `pl_subject_notifications` (settings:91)<br>쓰는 곳: PermissionStepSheet.kt:197 |
| '전체 화면 알림'이 | 토스트 앞부분 | 토스트 조각 | `pl_subject_full_screen` (settings:92)<br>쓰는 곳: PermissionStepSheet.kt:198 |
| '다른 앱 위에 표시'가 | 토스트 앞부분 | 토스트 조각 | `pl_subject_overlay` (settings:93)<br>쓰는 곳: PermissionStepSheet.kt:199 |
| '위치'가 | 토스트 앞부분 | 토스트 조각 | `pl_subject_location` (settings:94)<br>쓰는 곳: PermissionStepSheet.kt:200 |
| '배터리 사용 제한 해제'가 | 토스트 앞부분 | 토스트 조각 | `pl_subject_battery` (settings:95)<br>쓰는 곳: PermissionStepSheet.kt:201 |
| %1$s 아직 허용되지 않았어요 | 폰 설정에서 돌아왔는데 아직 꺼져 있을 때. 예: ''알림'이 아직 허용되지 않았어요' | 토스트 | `pl_toast_not_yet` (settings:96)<br>쓰는 곳: PermissionStepSheet.kt:186 |
| %1$s 꺼졌어요 | 켜져 있던 권한이 꺼졌을 때. 예: ''위치'가 꺼졌어요' | 토스트 | `pl_toast_turned_off` (settings:97)<br>쓰는 곳: PermissionStepSheet.kt:188 |
| 설정을 열지 못했어요. 폰 설정에서 직접 허용해 주세요 | 폰 설정 화면을 열지 못했을 때 | 토스트 | `pl_toast_cannot_open` (settings:98)<br>쓰는 곳: PermissionStepSheet.kt:189 |

### P6 전원 종료 방지 시트

| 문구 | 어떤 상황에 보이나 | 보이는 방식 | 리소스 키와 위치 |
|---|---|---|---|
| 알람이 울리는 동안 전원 메뉴를 막으려면<br>접근성 권한이 필요해요 | P6 전원 종료 방지를 켤 때 접근성 권한이 없으면 | 시트 제목(두 줄) | `power_sheet_title` (settings:105)<br>쓰는 곳: PowerGuardSheet.kt:14 |
| 화면 내용은 읽거나 저장하지 않아요.<br>'설치된 앱'에서 '일어나'를 켜 주세요. | 같은 시트 | 시트 본문 | `power_sheet_msg` (settings:106)<br>쓰는 곳: PowerGuardSheet.kt:15 |
| 동의하고 설정 열기 | 같은 시트 | 버튼 | `power_sheet_agree` (settings:107)<br>쓰는 곳: PowerGuardSheet.kt:16 |

### 방해 금지 모드에서 허용하기 안내 화면

| 문구 | 어떤 상황에 보이나 | 보이는 방식 | 리소스 키와 위치 |
|---|---|---|---|
| 방해 금지 모드에서 허용하기 | 방해 금지 안내 화면 | 화면 제목 | `dnd_guide_title` (settings:147)<br>쓰는 곳: DndGuidePage.kt:97 |
| 알람이 울리는 동안만 허용 | 안내 화면 맨 위 스위치 줄 | 스위치 줄 이름 | `dnd_switch_title` (settings:176)<br>쓰는 곳: DndGuidePage.kt:117 |
| 방해 금지 모드여도 알람 소리를 내요 | 같은 줄 아래 | 스위치 줄 설명 | `dnd_switch_desc` (settings:177)<br>쓰는 곳: DndGuidePage.kt:118 |
| 방해 금지 모드나 수면 모드가 켜지면 알람 화면은 떠도 소리와 진동이 막힐 수 있어요. 아래 스위치를 켜거나 폰 설정에서 '알람'을 켜 주세요. | 스위치 아래 | 본문 | `dnd_guide_intro` (settings:148)<br>쓰는 곳: DndGuidePage.kt:126 |
| 방해 금지 모드에서 '알람' 켜기 | 단계 카드 | 카드 제목 | `dnd_guide_step_title` (settings:149)<br>쓰는 곳: DndGuidePage.kt:133 |
| 폰 설정의 방해 금지 모드에서 '알람 및 소리'를 열고 '알람'을 켜요. 이 스위치 하나가 알람 소리를 정해요. | 단계 카드 | 카드 본문 | `dnd_guide_step_body` (settings:150)<br>쓰는 곳: DndGuidePage.kt:140 |
| 알람 | 단계 카드 속 폰 설정 그림 | 그림 속 글 | `dnd_guide_switch_label` (settings:151)<br>쓰는 곳: DndGuidePage.kt:216 |
| 방해 금지 모드에서도 소리와 진동 내기 | 같은 그림 | 그림 속 글 | `dnd_guide_switch_desc` (settings:152)<br>쓰는 곳: DndGuidePage.kt:221 |
| 알아 두세요 | 알아 두세요 카드 | 카드 제목 | `dnd_guide_note_title` (settings:153)<br>쓰는 곳: DndGuidePage.kt:149 |
| 수면 모드나 루틴이 시간 맞춰 켜져도 이 '알람' 스위치를 따라요. | 알아 두세요 카드 첫 줄 | 본문 | `dnd_guide_note_routine` (settings:154)<br>쓰는 곳: DndGuidePage.kt:156 |
| '허용할 앱'에 일어나 앱을 넣어도 이 스위치가 꺼져 있으면 소리가 막혀요. | 알아 두세요 카드 둘째 줄 | 본문 | `dnd_guide_note_apps` (settings:155)<br>쓰는 곳: DndGuidePage.kt:162 |
| 스위치를 켤 때 방해 금지 모드 접근 권한이 없으면 폰 설정이 열려요. | 화면 아래 | 작은 글 | `dnd_guide_footnote` (settings:156)<br>쓰는 곳: DndGuidePage.kt:168 |
| 방해 금지 설정 열기 | 화면 맨 아래 | 버튼 | `dnd_guide_open` (settings:157)<br>쓰는 곳: DndGuidePage.kt:175 |

### T3 실험실 (Crazy Mode)

| 문구 | 어떤 상황에 보이나 | 보이는 방식 | 리소스 키와 위치 |
|---|---|---|---|
| 실험실 | 설정 첫 화면 실험실 카드 위 | 카드 머리글 | `settings_lab` (settings:132)<br>쓰는 곳: LabPage.kt:127 |
| Crazy Mode | 실험실 카드 줄, T3 화면 | 줄 이름, 화면 제목 | `settings_lab_title` (settings:133)<br>쓰는 곳: LabPage.kt:134, LabPage.kt:196 |
| 꺼짐 | 실험실 줄 값, 켠 기능이 없을 때 | 줄 값 | `settings_lab_off` (settings:134)<br>쓰는 곳: LabPage.kt:147, LabPage.kt:374 |
| 잘 못 일어나는 분을 위한 실험 기능이에요. 아직 다듬는 중이라, 써 보고 '문의하기'로 알려 주세요. | T3 맨 위 | 본문 | `settings_lab_intro` (settings:135)<br>쓰는 곳: LabPage.kt:210 |
| 톡백 같은 접근성 기능을 쓰는 중에는 움직이는 버튼이 자동으로 꺼져요. | T3 아래 | 작은 글 | `settings_lab_note` (settings:136)<br>쓰는 곳: LabPage.kt:229 |
| 움직이는 버튼 | 실험실 줄 값, T3 줄, T3-1 시트 | 줄 이름, 시트 제목 | `settings_lab_trick` (settings:137)<br>쓰는 곳: LabPage.kt:144, LabPage.kt:217, LabPage.kt:386 |
| 속도 %1$s | T3 움직이는 버튼 줄 값. 예: '속도 800ms' | 줄 값 | `settings_lab_trick_on` (settings:140)<br>쓰는 곳: LabPage.kt:370 |
| %1$dms | 속도 값. 예: '800ms' | 값 조각 | `settings_lab_speed_ms` (settings:142)<br>쓰는 곳: LabPage.kt:371 |
| 접근성 기능이 켜져 있어 움직이는 버튼이 꺼져요 | T3 움직이는 버튼 줄 값, 접근성 기능 때문에 꺼졌을 때 | 줄 값 | `settings_lab_trick_a11y` (settings/lab_escape:15)<br>쓰는 곳: LabPage.kt:218 |
| 알람이 울리면 버튼이 화면 안을 공처럼 튕겨 다녀요. 울리는 동안 멈추지 않아요. | T3-1 움직이는 버튼 시트 | 시트 설명 | `settings_lab_trick_desc` (settings:141)<br>쓰는 곳: LabPage.kt:387 |
| 끝에서 끝까지 걸리는 시간 | T3-1 속도 입력 칸 | 입력 칸 이름 | `settings_lab_speed_label` (settings:143)<br>쓰는 곳: TrickSpeedInput.kt:54 |
| ms | 속도 입력 칸 오른쪽 | 단위 | `settings_lab_speed_unit` (settings:144)<br>쓰는 곳: TrickSpeedInput.kt:96 |
| 작을수록 빨라요. %1$d~%2$d 사이로 넣어요 | 속도 입력 칸 아래. 예: '작을수록 빨라요. 50~5000 사이로 넣어요' | 도움말 | `settings_lab_speed_desc` (settings:145)<br>쓰는 곳: TrickSpeedInput.kt:102 |
| 미리 해보기 | T3-1 시트 아래 | 버튼 | `settings_lab_preview` (settings:146)<br>쓰는 곳: LabPage.kt:393 |
| 좀비모드 | 실험실 줄 값, T3 줄 | 줄 이름 | `settings_lab_recheck` (settings:138)<br>쓰는 곳: LabPage.kt:145, LabPage.kt:309 |
| 모든 미션을 완료했더라도 원하는 만큼 알람을 다시 반복할 수 있어요 | T3 좀비모드 줄 아래 | 줄 설명 | `settings_lab_recheck_value` (settings:139)<br>쓰는 곳: LabPage.kt:310 |
| 미션 포기 사용하기 | T3 줄 | 스위치 줄 이름 | `settings_lab_escape` (settings/lab_escape:4)<br>쓰는 곳: LabPage.kt:285 |
| 미션을 못 할 때 뜨는 알림이에요 | 같은 줄 아래 | 스위치 줄 설명 | `settings_lab_escape_value` (settings/lab_escape:5)<br>쓰는 곳: LabPage.kt:286 |
| 사용하지 않아도 흔들기 센서가 없는 폰이나 마지막 다시 울림에서는 떠요. | 같은 줄 카드 아래 | 작은 글 | `settings_lab_escape_note` (settings/lab_escape:6)<br>쓰는 곳: LabPage.kt:294 |
| 미션 포기를 사용하지 않을까요? | T3-4 미션 포기를 끌 때 | 시트 제목 | `settings_lab_escape_sheet_title` (settings/lab_escape:7)<br>쓰는 곳: LabPage.kt:325 |
| 사용하지 않으면 몸이 아프거나 흔들 수 없을 때도 미션을 끝내야 알람이 꺼져요. | 같은 시트 | 시트 설명 | `settings_lab_escape_sheet_desc` (settings/lab_escape:8)<br>쓰는 곳: LabPage.kt:326 |
| 사용하지 않아도 이럴 때는 떠요 | 같은 시트 목록 위 | 목록 제목 | `settings_lab_escape_sheet_label` (settings/lab_escape:9)<br>쓰는 곳: LabPage.kt:332 |
| 1. 흔들기 센서가 없는 폰에서 흔들기 미션을 할 때 | 같은 시트 목록 | 목록 줄 | `settings_lab_escape_sheet_item1` (settings/lab_escape:10)<br>쓰는 곳: LabPage.kt:337 |
| 2. 다시 울림이 마지막 바퀴일 때, 첫 미션에서 | 같은 시트 목록 | 목록 줄 | `settings_lab_escape_sheet_item2` (settings/lab_escape:11)<br>쓰는 곳: LabPage.kt:342 |
| 취소 | 같은 시트 | 버튼 | `settings_lab_escape_cancel` (settings/lab_escape:12)<br>쓰는 곳: LabPage.kt:353 |
| 사용 안 하기 | 같은 시트 | 버튼 | `settings_lab_escape_hide` (settings/lab_escape:13)<br>쓰는 곳: LabPage.kt:359 |
| 미리 해보기, 소리는 나지 않아요 | T3-3 움직이는 버튼 미리 해보기 위쪽 | 안내 막대 | `ring_preview_top` (ring:45)<br>쓰는 곳: TrickPreview.kt:118 |
| 접근성 기능이 켜져 있어 움직이는 버튼이 꺼져요 | T3-3 접근성 기능 때문에 버튼이 안 움직일 때 위쪽 막대 아래 | 안내 글 | `ring_preview_trick_off` (ring:47)<br>쓰는 곳: TrickPreview.kt:124 |
| 닫기 | T3-3 닫기 아이콘 | 화면 읽기용 이름 | `ring_preview_close` (ring:48)<br>쓰는 곳: TrickPreview.kt:132 |
| 잘했어요 | T3-3 미리 해보기를 마쳤을 때 | 가운데 글 | `ring_preview_done` (ring:49)<br>쓰는 곳: TrickPreview.kt:157 |
| 곧 설정으로 돌아가요 | T3-3 마친 뒤 아래 | 보조 글 | `ring_preview_back` (ring:50)<br>쓰는 곳: TrickPreview.kt:163 |

### T6 자주 묻는 질문

| 문구 | 어떤 상황에 보이나 | 보이는 방식 | 리소스 키와 위치 |
|---|---|---|---|
| 알람이 울리지 않을 때 | T6 첫 묶음 | 묶음 제목 | `faq_group_not_ringing` (settings:178)<br>쓰는 곳: InfoPages.kt:175 |
| 권한 | T6 둘째 묶음 | 묶음 제목 | `faq_group_permissions` (settings:179)<br>쓰는 곳: InfoPages.kt:176 |
| 기능 | T6 셋째 묶음 | 묶음 제목 | `faq_group_features` (settings:180)<br>쓰는 곳: InfoPages.kt:177 |
| 권한 화면 열기 | 답 아래, 권한 화면으로 가는 질문일 때 | 글자 버튼 | `faq_open_permissions` (settings:181)<br>쓰는 곳: InfoPages.kt:195 |
| 방해 금지 모드에서 허용하기 열기 | 답 아래, 방해 금지 안내로 가는 질문일 때 | 글자 버튼 | `faq_open_dnd` (settings:182)<br>쓰는 곳: InfoPages.kt:196 |
| 답을 못 찾았어요 | T6 맨 아래 | 줄(누르면 문의하기) | `faq_not_found` (settings:183)<br>쓰는 곳: InfoPages.kt:179 |
| 알람이 아예 울리지 않았어요 | T6 '알람이 울리지 않을 때' 묶음, 질문 줄 | 질문 줄 | `faq_a1_q` (settings:184)<br>쓰는 곳: InfoPages.kt:121 |
| 이 순서로 확인해 주세요.<br>1. 설정 '권한 점검'에서 필수 권한이 모두 켜져 있는지<br>2. 배터리 사용이 '제한 없음'인지<br>3. 삼성 폰이면 '절전 예외 앱'에 일어나가 있는지<br>다 켜져 있으면 설정의 '알람 미리 울려 보기'로 1분 뒤에 울려 보세요. | T6 '알람이 울리지 않을 때' 묶음, 질문을 누르면 펼쳐짐 | 펼친 답 | `faq_a1_a` (settings:185)<br>쓰는 곳: InfoPages.kt:121 |
| 알람 화면은 떴는데 소리가 안 났어요 | T6 '알람이 울리지 않을 때' 묶음, 질문 줄 | 질문 줄 | `faq_a2_q` (settings:186)<br>쓰는 곳: InfoPages.kt:122 |
| 방해 금지 모드나 수면 모드가 소리를 막았을 수 있어요. 폰 설정의 방해 금지 모드에서 '알람'을 켜 주세요. 모드 및 루틴으로 켜지는 모드도 같은 스위치를 따라요. 알람 편집의 볼륨과 폰의 알람 볼륨도 확인해 주세요. | T6 '알람이 울리지 않을 때' 묶음, 질문을 누르면 펼쳐짐 | 펼친 답 | `faq_a2_a` (settings:187)<br>쓰는 곳: InfoPages.kt:122 |
| 잠금 화면에 알람 화면이 안 떠요 | T6 '알람이 울리지 않을 때' 묶음, 질문 줄 | 질문 줄 | `faq_a3_q` (settings:188)<br>쓰는 곳: InfoPages.kt:123 |
| 설정 '권한 점검'에서 알림과 전체 화면 알림을 켜 주세요. 꺼져 있으면 화면 대신 위에서 알림으로만 내려와요. | T6 '알람이 울리지 않을 때' 묶음, 질문을 누르면 펼쳐짐 | 펼친 답 | `faq_a3_a` (settings:189)<br>쓰는 곳: InfoPages.kt:123 |
| 다른 앱을 쓰는 중에 알람 화면이 사라져요 | T6 '알람이 울리지 않을 때' 묶음, 질문 줄 | 질문 줄 | `faq_a4_q` (settings:190)<br>쓰는 곳: InfoPages.kt:124 |
| 설정 '권한 점검'에서 '다른 앱 위에 표시'를 켜 주세요. 꺼져 있으면 홈 버튼을 눌렀을 때 알람 화면이 뒤로 숨어요. | T6 '알람이 울리지 않을 때' 묶음, 질문을 누르면 펼쳐짐 | 펼친 답 | `faq_a4_a` (settings:191)<br>쓰는 곳: InfoPages.kt:124 |
| 알람이 몇 분 늦게 울려요 | T6 '알람이 울리지 않을 때' 묶음, 질문 줄 | 질문 줄 | `faq_a5_q` (settings:192)<br>쓰는 곳: InfoPages.kt:125 |
| 절전 기능이 알람을 늦췄을 수 있어요. 설정 '권한 점검'에서 배터리 사용 제한을 해제해 주세요. 안드로이드 12 폰은 '정확한 알람'도 켜 주세요. | T6 '알람이 울리지 않을 때' 묶음, 질문을 누르면 펼쳐짐 | 펼친 답 | `faq_a5_a` (settings:193)<br>쓰는 곳: InfoPages.kt:125 |
| 며칠 앱을 안 열었더니 알람이 안 울렸어요 | T6 '알람이 울리지 않을 때' 묶음, 질문 줄 | 질문 줄 | `faq_a6_q` (settings:194)<br>쓰는 곳: InfoPages.kt:126 |
| 삼성 폰은 오래 안 연 앱을 잠자기 상태로 바꿔요. 설정 '권한 점검'에서 '절전 예외 앱에 추가'를 해 두면 앱을 열지 않아도 울려요. | T6 '알람이 울리지 않을 때' 묶음, 질문을 누르면 펼쳐짐 | 펼친 답 | `faq_a6_a` (settings:195)<br>쓰는 곳: InfoPages.kt:126 |
| 폰이 꺼져 있던 동안 알람이 지나갔어요 | T6 '알람이 울리지 않을 때' 묶음, 질문 줄 | 질문 줄 | `faq_a7_q` (settings:196)<br>쓰는 곳: InfoPages.kt:127 |
| 폰이 꺼져 있으면 알람이 울릴 수 없어요. 폰을 켜면 '알람을 놓쳤어요' 알림으로 알려 드리고, 다음 알람부터는 그대로 울려요. 배터리가 적으면 전날 밤 9시에 충전 안내를 보내 드려요. | T6 '알람이 울리지 않을 때' 묶음, 질문을 누르면 펼쳐짐 | 펼친 답 | `faq_a7_a` (settings:197)<br>쓰는 곳: InfoPages.kt:127 |
| 앱을 강제로 종료했더니 알람이 안 울렸어요 | T6 '알람이 울리지 않을 때' 묶음, 질문 줄 | 질문 줄 | `faq_a8_q` (settings:198)<br>쓰는 곳: InfoPages.kt:128 |
| 폰 설정에서 '강제 종료'를 누르면 안드로이드가 예정된 알람을 모두 지워요. 다음에 앱을 열면 알람을 다시 맞추고 알려 드려요. 최근 앱 목록에서 밀어 닫는 것은 괜찮아요. | T6 '알람이 울리지 않을 때' 묶음, 질문을 누르면 펼쳐짐 | 펼친 답 | `faq_a8_a` (settings:199)<br>쓰는 곳: InfoPages.kt:128 |
| 알람이 울렸는데 바로 꺼졌어요 | T6 '알람이 울리지 않을 때' 묶음, 질문 줄 | 질문 줄 | `faq_a9_q` (settings:200)<br>쓰는 곳: InfoPages.kt:129 |
| 1분 동안 해제하지 않으면 알람이 잠시 멈췄다가 '다시 울림' 간격만큼 쉰 뒤 다시 울려요. 다시 울림이 꺼져 있으면 한 번 울리고 멈춰요. 알람 편집의 '다시 울림'을 확인해 주세요. | T6 '알람이 울리지 않을 때' 묶음, 질문을 누르면 펼쳐짐 | 펼친 답 | `faq_a9_a` (settings:201)<br>쓰는 곳: InfoPages.kt:129 |
| 알림 권한은 왜 필요한가요? | T6 '권한' 묶음, 질문 줄 | 질문 줄 | `faq_b1_q` (settings:202)<br>쓰는 곳: InfoPages.kt:133 |
| 알람이 울릴 때 해제 화면을 띄우는 데 써요. 끄면 소리만 나고 화면이 안 뜰 수 있어요. 미션을 못 할 때 쓰는 '미션 포기'도 알림으로 와요. | T6 '권한' 묶음, 질문을 누르면 펼쳐짐 | 펼친 답 | `faq_b1_a` (settings:203)<br>쓰는 곳: InfoPages.kt:133 |
| 전체 화면 알림은 무엇인가요? | T6 '권한' 묶음, 질문 줄 | 질문 줄 | `faq_b2_q` (settings:204)<br>쓰는 곳: InfoPages.kt:134 |
| 잠금 화면 위로 알람 화면을 바로 띄우는 권한이에요. 안드로이드 14부터 끌 수 있고, 끄면 잠금 화면에서 알림으로만 보여요. | T6 '권한' 묶음, 질문을 누르면 펼쳐짐 | 펼친 답 | `faq_b2_a` (settings:205)<br>쓰는 곳: InfoPages.kt:134 |
| 다른 앱 위에 표시는 왜 필요한가요? | T6 '권한' 묶음, 질문 줄 | 질문 줄 | `faq_b3_q` (settings:206)<br>쓰는 곳: InfoPages.kt:135 |
| 다른 앱을 쓰는 중에도 알람 화면을 맨 앞에 띄워요. 끄면 홈 버튼을 눌렀을 때 알람 화면이 사라질 수 있어요. | T6 '권한' 묶음, 질문을 누르면 펼쳐짐 | 펼친 답 | `faq_b3_a` (settings:207)<br>쓰는 곳: InfoPages.kt:135 |
| 배터리 사용 제한을 해제하면 배터리가 빨리 닳나요? | T6 '권한' 묶음, 질문 줄 | 질문 줄 | `faq_b4_q` (settings:208)<br>쓰는 곳: InfoPages.kt:136 |
| 일어나는 알람 시각과 알람이 울리는 동안에만 움직여서 평소 배터리 사용은 거의 늘지 않아요. 해제하지 않으면 절전 기능 때문에 알람이 늦게 울릴 수 있어요. | T6 '권한' 묶음, 질문을 누르면 펼쳐짐 | 펼친 답 | `faq_b4_a` (settings:209)<br>쓰는 곳: InfoPages.kt:136 |
| 정확한 알람은 무엇인가요? | T6 '권한' 묶음, 질문 줄 | 질문 줄 | `faq_b5_q` (settings:210)<br>쓰는 곳: InfoPages.kt:137 |
| 안드로이드 12 폰에서 정한 시각에 딱 맞춰 울리게 하는 권한이에요. 안드로이드 13 이상은 따로 켤 필요가 없어서 목록에 보이지 않아요. | T6 '권한' 묶음, 질문을 누르면 펼쳐짐 | 펼친 답 | `faq_b5_a` (settings:211)<br>쓰는 곳: InfoPages.kt:137 |
| 방해 금지 모드에서도 울리게 하려면요? | T6 '권한' 묶음, 질문 줄 | 질문 줄 | `faq_b6_q` (settings:212)<br>쓰는 곳: InfoPages.kt:138 |
| 폰 설정의 방해 금지 모드에서 '알람'을 켜 주세요. 앱이 이 설정을 대신 바꿀 수는 없어요. 설정의 '방해 금지 모드에서 허용하기'를 켜면 알람이 울리는 동안만 소리를 낼 수 있어요. | T6 '권한' 묶음, 질문을 누르면 펼쳐짐 | 펼친 답 | `faq_b6_a` (settings:213)<br>쓰는 곳: InfoPages.kt:138 |
| 접근성 권한은 왜 필요한가요? | T6 '권한' 묶음, 질문 줄 | 질문 줄 | `faq_b7_q` (settings:214)<br>쓰는 곳: InfoPages.kt:139 |
| '전원 종료 방지'를 켤 때만 써요. 알람이 울리는 동안 전원 메뉴가 열리면 바로 닫는 데만 쓰고, 화면 내용은 읽거나 저장하지 않아요. 쓰지 않으면 켜지 않아도 돼요. | T6 '권한' 묶음, 질문을 누르면 펼쳐짐 | 펼친 답 | `faq_b7_a` (settings:215)<br>쓰는 곳: InfoPages.kt:139 |
| 위치 권한은 왜 묻나요? | T6 '권한' 묶음, 질문 줄 | 질문 줄 | `faq_b8_q` (settings:216)<br>쓰는 곳: InfoPages.kt:140 |
| 날씨에 맞춰 하늘 배경을 바꾸는 데만 써요. 대략적인 위치만 쓰고, 허용하지 않아도 알람은 그대로 울려요. | T6 '권한' 묶음, 질문을 누르면 펼쳐짐 | 펼친 답 | `faq_b8_a` (settings:217)<br>쓰는 곳: InfoPages.kt:140 |
| 미션을 다 못 끝내면 어떻게 되나요? | T6 '기능' 묶음, 질문 줄 | 질문 줄 | `faq_c1_q` (settings:218)<br>쓰는 곳: InfoPages.kt:144 |
| 미션 시간이 지나면 알람이 다시 울려요. 다시 '미션 시작'을 누르면 첫 미션부터 다시 해요. | T6 '기능' 묶음, 질문을 누르면 펼쳐짐 | 펼친 답 | `faq_c1_a` (settings:219)<br>쓰는 곳: InfoPages.kt:144 |
| 미션을 할 수 없는 상황이면요? | T6 '기능' 묶음, 질문 줄 | 질문 줄 | `faq_c2_q` (settings:220)<br>쓰는 곳: InfoPages.kt:145 |
| 알림 패널의 '미션 포기'를 누르고 나오는 문장을 그대로 따라 쓰면 알람이 해제돼요. | T6 '기능' 묶음, 질문을 누르면 펼쳐짐 | 펼친 답 | `faq_c2_a` (settings:221)<br>쓰는 곳: InfoPages.kt:145 |
| 공휴일에는 알람을 쉬고 싶어요 | T6 '기능' 묶음, 질문 줄 | 질문 줄 | `faq_c3_q` (settings:222)<br>쓰는 곳: InfoPages.kt:146 |
| 반복 알람의 편집 화면에서 '공휴일에는 끄기'를 켜 주세요. 대체공휴일도 쉬어요. | T6 '기능' 묶음, 질문을 누르면 펼쳐짐 | 펼친 답 | `faq_c3_a` (settings:223)<br>쓰는 곳: InfoPages.kt:146 |
| 다음 한 번만 알람을 쉬고 싶어요 | T6 '기능' 묶음, 질문 줄 | 질문 줄 | `faq_c4_q` (settings:224)<br>쓰는 곳: InfoPages.kt:147 |
| 홈에서 반복 알람을 끄면 카드에 '이번만 쉬기'가 나와요. 누르면 아직 오지 않은 다음 알람 하나만 쉬고, 그다음부터는 다시 울려요. | T6 '기능' 묶음, 질문을 누르면 펼쳐짐 | 펼친 답 | `faq_c4_a` (settings:225)<br>쓰는 곳: InfoPages.kt:147 |
| 알람을 끈 뒤 다시 잠들까 봐 걱정돼요 | T6 '기능' 묶음, 질문 줄 | 질문 줄 | `faq_c5_q` (settings:226)<br>쓰는 곳: InfoPages.kt:148 |
| 알람 편집의 '다시 잠들기 방지'를 켜면 알람을 해제하고 정한 시간이 지난 뒤 '일어나셨죠?'를 물어요. 버튼을 10초간 눌러야 끝나요. 완전히 일어났다면 홈이나 알림 패널에서 재확인을 해제할 수 있어요. | T6 '기능' 묶음, 질문을 누르면 펼쳐짐 | 펼친 답 | `faq_c5_a` (settings:227)<br>쓰는 곳: InfoPages.kt:148 |
| 미션 중에 전원을 꺼 버리면요? | T6 '기능' 묶음, 질문 줄 | 질문 줄 | `faq_c6_q` (settings:228)<br>쓰는 곳: InfoPages.kt:149 |
| 설정에서 '전원 종료 방지'를 켜면 알람이 울리는 동안 전원 메뉴가 열려도 바로 닫혀요. 처음 켤 때 접근성 권한을 켜는 방법을 안내해 드려요. | T6 '기능' 묶음, 질문을 누르면 펼쳐짐 | 펼친 답 | `faq_c6_a` (settings:229)<br>쓰는 곳: InfoPages.kt:149 |
| 소리 없이 진동만 울리게 할 수 있나요? | T6 '기능' 묶음, 질문 줄 | 질문 줄 | `faq_c7_q` (settings:230)<br>쓰는 곳: InfoPages.kt:150 |
| 알람 편집의 '볼륨'에서 볼륨을 0으로 내리고 진동을 켜 두면 소리 없이 진동만 울려요. | T6 '기능' 묶음, 질문을 누르면 펼쳐짐 | 펼친 답 | `faq_c7_a` (settings:231)<br>쓰는 곳: InfoPages.kt:150 |

### T7 문의하기

| 문구 | 어떤 상황에 보이나 | 보이는 방식 | 리소스 키와 위치 |
|---|---|---|---|
| 문의하기 | T7 문의하기 화면 | 화면 제목 | `support_title` (settings:232)<br>쓰는 곳: SupportPage.kt:179 |
| 어떤 문의인가요? | T7 첫 구역 | 구역 제목 | `support_type_title` (settings:233)<br>쓰는 곳: SupportPage.kt:195 |
| 오류 신고 | 문의 종류 칩 | 선택 칩 | `support_type_bug` (settings:234)<br>쓰는 곳: SupportPage.kt:434 |
| 궁금한 점 | 문의 종류 칩 | 선택 칩 | `support_type_question` (settings:235)<br>쓰는 곳: SupportPage.kt:435 |
| 제안 | 문의 종류 칩 | 선택 칩 | `support_type_idea` (settings:236)<br>쓰는 곳: SupportPage.kt:436 |
| 자주 그런가요? | 오류 신고일 때 둘째 구역 | 구역 제목 | `support_freq_title` (settings:237)<br>쓰는 곳: SupportPage.kt:202 |
| 매번 | 빈도 칩 | 선택 칩 | `support_freq_always` (settings:238)<br>쓰는 곳: SupportPage.kt:441 |
| 가끔 | 빈도 칩 | 선택 칩 | `support_freq_sometimes` (settings:239)<br>쓰는 곳: SupportPage.kt:442 |
| 한 번 | 빈도 칩 | 선택 칩 | `support_freq_once` (settings:240)<br>쓰는 곳: SupportPage.kt:443 |
| 내용 | 내용 구역 | 구역 제목 | `support_body_title` (settings:241)<br>쓰는 곳: SupportPage.kt:208 |
| 언제, 무엇을 했을 때 어떻게 됐는지 적어 주세요.<br>예: 오전 6:30 알람이 울리지 않았어요 | 내용 칸이 비었을 때, 오류 신고 | 입력 칸 힌트(두 줄) | `support_body_hint_bug` (settings:242)<br>쓰는 곳: SupportPage.kt:450 |
| 궁금한 점을 적어 주세요 | 내용 칸이 비었을 때, 궁금한 점 | 입력 칸 힌트 | `support_body_hint_question` (settings:243)<br>쓰는 곳: SupportPage.kt:448 |
| 있으면 좋을 기능이나 바꾸면 좋을 점을 적어 주세요 | 내용 칸이 비었을 때, 제안 | 입력 칸 힌트 | `support_body_hint_idea` (settings:244)<br>쓰는 곳: SupportPage.kt:449 |
| 10자 이상 적어 주세요 | 내용 칸 아래, 10자가 안 될 때 | 도움말 | `support_body_min` (settings:245)<br>쓰는 곳: SupportPage.kt:211 |
| 사진 붙이기 (최대 3장) | 사진 구역 | 구역 제목 | `support_photo` (settings:246)<br>쓰는 곳: SupportPage.kt:217 |
| 사진 추가 | 사진 더하기 칸 | 화면 읽기용 | `support_photo_add` (settings:247)<br>쓰는 곳: SupportPage.kt:369 |
| 사진 빼기 | 붙인 사진의 빼기 아이콘 | 화면 읽기용 | `support_photo_remove` (settings:248)<br>쓰는 곳: SupportPage.kt:368 |
| 앱 기록 같이 보내기 | 기록 보내기 스위치 줄 | 스위치 줄 이름 | `support_log` (settings:249)<br>쓰는 곳: SupportPage.kt:222 |
| 알람 기록과 권한 상태만 보내요 | 같은 줄 아래 | 스위치 줄 설명 | `support_log_desc` (settings:250)<br>쓰는 곳: SupportPage.kt:223 |
| 보낼 기록 보기 | 같은 줄 아래 링크, T7-3 화면 제목 | 링크 글, 화면 제목 | `support_log_view` (settings:251)<br>쓰는 곳: SupportPage.kt:228, SupportPage.kt:276 |
| 알람이 언제 예약되고 울렸는지, 권한 상태, 폰 정보만 들어 있어요. 알람 이름과 위치는 넣지 않아요. | T7-3 보낼 기록 보기 화면 위 | 본문 | `support_log_note` (settings:252)<br>쓰는 곳: SupportPage.kt:288 |
| 보내는 것: 문의 글, 붙인 사진, 폰 정보, 앱 기록 | 보내기 위 동의 상자 첫 줄 | 동의 내용 | `support_consent_items` (settings:253)<br>쓰는 곳: SupportPage.kt:232 |
| 쓰는 곳: 문의 답변과 오류 고치기 | 동의 상자 둘째 줄 | 동의 내용 | `support_consent_purpose` (settings:254)<br>쓰는 곳: SupportPage.kt:232 |
| 보관: 답변이 끝나고 90일 뒤 지워요 | 동의 상자 셋째 줄 | 동의 내용 | `support_consent_keep` (settings:255)<br>쓰는 곳: SupportPage.kt:232 |
| 위 내용에 동의해요 | 동의 체크박스 옆 | 체크박스 글 | `support_consent_check` (settings:256)<br>쓰는 곳: SupportPage.kt:243 |
| 보내기 | T7 맨 아래 | 버튼 | `support_send` (settings:257)<br>쓰는 곳: SupportPage.kt:249 |
| 메일 앱에서 보내기를 눌러 주세요 | 보내기를 눌러 메일 앱이 열릴 때 | 토스트 | `support_mail_hint` (settings:258)<br>쓰는 곳: SupportPage.kt:126 |
| 메일 앱이 없어 보낼 수 없어요 | 메일 앱이 없을 때 | 토스트 | `support_no_mail` (settings:259)<br>쓰는 곳: SupportPage.kt:134 |
| 쓰던 문의를 지울까요? | 쓰던 내용이 있는데 뒤로 갈 때 | 대화상자 제목 | `support_exit_title` (settings:261)<br>쓰는 곳: SupportPage.kt:143 |
| 지우면 쓴 내용과 붙인 사진이 사라져요 | 같은 대화상자 | 대화상자 본문 | `support_exit_message` (settings:262)<br>쓰는 곳: SupportPage.kt:144 |
| 계속 쓰기 | 같은 대화상자 | 버튼 | `support_exit_keep` (settings:263)<br>쓰는 곳: SupportPage.kt:145 |
| 지우기 | 같은 대화상자 | 버튼 | `support_exit_discard` (settings:264)<br>쓰는 곳: SupportPage.kt:146 |
| [일어나 문의] %1$s, %2$s | 메일 앱에 미리 채워지는 제목. 예: '[일어나 문의] 오류 신고, 매번' | 메일 제목 | `support_mail_subject` (settings:260)<br>쓰는 곳: SupportPage.kt:104 |
| 기기: %1$s<br>안드로이드: %2$s<br>앱 버전: %3$s<br>꺼진 권한: %4$s | 메일 앱에 미리 채워지는 본문(빈 줄 셋 뒤 폰 정보). 예: '기기: (폰 모델명), 안드로이드: 14, 앱 버전: (버전), 꺼진 권한: 알림, 위치' | 메일 본문 | `settings_feedback_body` (settings:18)<br>쓰는 곳: SettingsScreen.kt:439, SupportPage.kt:105 |
| 없음 | 메일 본문 '꺼진 권한' 값, 꺼진 게 없을 때 | 메일 본문 조각 | `settings_feedback_none` (settings:19)<br>쓰는 곳: SettingsScreen.kt:437, SupportPage.kt:106 |
| 일어나 의견 | 문의하기 화면 없이 바로 메일을 열 때의 제목 | 메일 제목 | `settings_feedback_subject` (settings:17)<br>쓰는 곳: SettingsScreen.kt:447 |

## 10. 화면 밖: 알림 패널과 폰 설정

### 알림 통로(채널) 이름: 폰 설정, 앱 알림 목록에 보임

| 문구 | 어떤 상황에 보이나 | 보이는 방식 | 리소스 키와 위치 |
|---|---|---|---|
| 울리는 알람 | 폰 설정 알림 목록 | 채널 이름 | `channel_ringing` (alarm:4)<br>쓰는 곳: RingNotifications.kt:72 |
| 알람이 울리는 동안 화면을 띄워요 | 같은 목록에서 채널을 누르면 | 채널 설명 | `channel_ringing_description` (alarm:5)<br>쓰는 곳: RingNotifications.kt:74 |
| 알람 울리는 중 | 폰 설정 알림 목록 | 채널 이름 | `channel_status` (alarm:20)<br>쓰는 곳: RingNotifications.kt:83 |
| 알람이 울리는 동안 알림 패널에 조용히 보여줘요 | 같은 목록 | 채널 설명 | `channel_status_description` (alarm:21)<br>쓰는 곳: RingNotifications.kt:87 |
| 재확인 기다림 | 폰 설정 알림 목록 | 채널 이름 | `channel_waiting` (alarm:22)<br>쓰는 곳: RingNotifications.kt:93 |
| 다시 울리기 전이나 재수면 여부를 재확인하기 전에 조용히 보여줘요 | 같은 목록 | 채널 설명 | `channel_waiting_description` (alarm:23)<br>쓰는 곳: RingNotifications.kt:96 |
| 알람 비상 알림 | 폰 설정 알림 목록 | 채널 이름 | `channel_fallback` (alarm:6)<br>쓰는 곳: RingNotifications.kt:104 |
| 알람 화면을 띄우지 못했을 때 소리로 깨워요 | 같은 목록 | 채널 설명 | `channel_fallback_description` (alarm:7)<br>쓰는 곳: RingNotifications.kt:105 |
| 놓친 알람 | 폰 설정 알림 목록 | 채널 이름 | `channel_missed` (alarm:13)<br>쓰는 곳: RingNotifications.kt:98 |
| 해제하지 못하고 지나간 알람을 알려요 | 같은 목록 | 채널 설명 | `channel_missed_description` (alarm:14)<br>쓰는 곳: RingNotifications.kt:99 |
| 미션을 못 할 때 | 폰 설정 알림 목록 | 채널 이름 | `channel_escape` (alarm:89)<br>쓰는 곳: RingNotifications.kt:117 |
| 울림 미션을 할 수 없을 때 문장을 따라 써서 알람을 해제하는 알림이에요 | 같은 목록 | 채널 설명 | `channel_escape_description` (alarm:90)<br>쓰는 곳: RingNotifications.kt:118 |
| 다음 알람 | 폰 설정 알림 목록 | 채널 이름 | `channel_next_alarm` (alarm:65)<br>쓰는 곳: NextAlarmNotice.kt:94 |
| 예정된 다음 알람을 알림 패널에 항상 보여줘요 | 같은 목록 | 채널 설명 | `channel_next_alarm_description` (alarm:66)<br>쓰는 곳: NextAlarmNotice.kt:97 |
| 알람 점검 | 폰 설정 알림 목록 | 채널 이름 | `channel_reminder` (alarm:78)<br>쓰는 곳: BatteryReminder.kt:100 |
| 내일 알람이 제때 울리지 못할 수 있을 때 알려요 | 같은 목록 | 채널 설명 | `channel_reminder_description` (alarm:79)<br>쓰는 곳: BatteryReminder.kt:102 |
| 잠들기 재생 | 폰 설정 알림 목록 | 채널 이름 | `channel_sleep` (alarm:8)<br>쓰는 곳: SleepPlaybackService.kt:39 |
| 잠들기 소리를 트는 동안 재생 조절을 보여 줘요 | 같은 목록 | 채널 설명 | `channel_sleep_description` (alarm:9)<br>쓰는 곳: SleepPlaybackService.kt:40 |

### 울리는 중과 기다리는 중 알림

| 문구 | 어떤 상황에 보이나 | 보이는 방식 | 리소스 키와 위치 |
|---|---|---|---|
| 예정된 시간이 됐어요 | 알람이 울리는 동안(울림 화면을 못 띄웠을 때 깨우기 알림 포함) | 알림 제목 | `ring_title` (alarm:17)<br>쓰는 곳: RingNotifications.kt:218, RingNotifications.kt:407 |
| 눌러서 알람을 해제하세요 | 울리는 중 알림, 깨우기 알림 | 알림 본문 | `ring_text_ringing` (alarm:24)<br>쓰는 곳: RingNotifications.kt:408, RingNotifications.kt:528 |
| 미션을 하고 있어요 | 미션을 하는 동안 | 알림 본문 | `ring_text_mission` (alarm:25)<br>쓰는 곳: RingNotifications.kt:529 |
| 잠시 뒤 다시 울려요 | 1분 울리고 멈춰 쉬는 동안(시각을 모를 때) | 알림 본문 | `ring_text_resting` (alarm:26)<br>쓰는 곳: RingNotifications.kt:530 |
| 알람이 잠시 멈췄어요 | 다시 울림으로 쉬는 동안 | 알림 제목 | `resting_title` (alarm:18)<br>쓰는 곳: RingNotifications.kt:260 |
| %1$s에 다시 울려요. 눌러서 지금 해제할 수 있어요 | 다시 울림으로 쉬는 동안. 예: '8:30에 다시 울려요. 눌러서 지금 해제할 수 있어요' | 알림 본문 | `ring_text_resting_at` (alarm:28)<br>쓰는 곳: RingNotifications.kt:269 |
| %1$d분 뒤 알람이 다시 울려요 | 쉬기 시작할 때. 예: '5분 뒤 알람이 다시 울려요' | 토스트(앱 밖에서도 뜸) | `rest_toast` (alarm:31)<br>쓰는 곳: RingNotifications.kt:459 |
| %1$d분 뒤 %2$d번째 알람이 울려요 | 쉬기 시작할 때, 횟수가 정해져 있으면. 예: '5분 뒤 2번째 알람이 울려요' | 토스트 | `rest_toast_numbered` (alarm:30)<br>쓰는 곳: RingNotifications.kt:457 |
| 알람을 미뤘어요 | 미루기로 쉬는 동안 | 알림 제목 | `snoozed_title` (alarm:33)<br>쓰는 곳: RingNotifications.kt:259 |
| %1$s에 다시 울려요. 눌러서 지금 미션을 할 수 있어요 | 같은 알림. 예: '오전 6:35에 다시 울려요. 눌러서 지금 미션을 할 수 있어요' | 알림 본문 | `snoozed_text` (alarm:34)<br>쓰는 곳: RingNotifications.kt:266 |
| 예정된 알람을 해제했어요 | 알람을 해제하고 W2를 기다리는 동안 | 알림 제목 | `waiting_title` (alarm:19)<br>쓰는 곳: RingNotifications.kt:261 |
| %1$d분 뒤 재수면 여부를 재확인할게요 | 같은 알림. 예: '3분 뒤 재수면 여부를 재확인할게요' | 알림 본문 | `ring_text_waiting` (alarm:35)<br>쓰는 곳: RingNotifications.kt:271 |
| 일어나셨죠? | W2가 뜰 때(전체 화면 알림) | 알림 제목 | `wake_check_title` (alarm:37)<br>쓰는 곳: RingNotifications.kt:216 |
| 일어나셨죠? 버튼을 10초간 눌러주세요 | 같은 알림 | 알림 본문 | `ring_text_wake_check` (alarm:38)<br>쓰는 곳: RingNotifications.kt:532 |
| 아직 깨어 계시죠? | W3가 뜰 때 | 알림 제목 | `recheck_title` (alarm:40)<br>쓰는 곳: RingNotifications.kt:217 |
| 버튼을 10초간 눌러주세요 | 같은 알림 | 알림 본문 | `ring_text_recheck` (alarm:41)<br>쓰는 곳: RingNotifications.kt:534 |
| %1$s에 재수면 여부를 재확인할게요 | W3를 기다리는 동안. 예: '오전 6:43에 재수면 여부를 재확인할게요' | 알림 제목 | `recheck_waiting_title` (alarm:44)<br>쓰는 곳: RingNotifications.kt:313 |
| 잠시 뒤 재수면 여부를 재확인할게요 | W3를 기다리는 동안(시각을 모를 때 제목, 단계 글) | 알림 제목, 본문 | `ring_text_recheck_waiting` (alarm:42)<br>쓰는 곳: RingNotifications.kt:314, RingNotifications.kt:531, RingNotifications.kt:533 |
| 완전히 일어나셨으면 재확인을 해제해주세요 | W3를 기다리는 동안 | 알림 본문 | `recheck_waiting_text` (alarm:45)<br>쓰는 곳: RingNotifications.kt:318 |
| 재확인 해제 | W2, W3를 기다리는 알림 아래 | 알림 버튼 | `recheck_release_action` (alarm:46)<br>쓰는 곳: RingNotifications.kt:301 |
| 곧 재수면 여부를 재확인할게요 | 실험실 좀비모드로 다음 W3를 기다리는 동안 | 알림 제목 | `recheck_multi_waiting_title` (alarm:48)<br>쓰는 곳: RingNotifications.kt:340 |
| 남은 재확인 %1$d번. 완전히 일어나셨으면 재확인을 해제해주세요 | 같은 알림. 예: '남은 재확인 2번. 완전히 일어나셨으면 재확인을 해제해주세요' | 알림 본문 | `recheck_multi_waiting_text` (alarm:49)<br>쓰는 곳: RingNotifications.kt:341 |
| 남은 재확인 %1$d번 | (쓰는 곳 없음) 예: '남은 재확인 2번' | 알림 글 | `recheck_multi_left` (alarm:50)<br>쓰는 곳: 없음 |
| 오전 %1$s | 알림 속 시각 앞. 예: '오전 6:35' | 시각 조각 | `clock_am` (alarm:51)<br>쓰는 곳: NextAlarmNotice.kt:145, RingNotifications.kt:355 |
| 오후 %1$s | 알림 속 시각 앞. 예: '오후 2:10' | 시각 조각 | `clock_pm` (alarm:52)<br>쓰는 곳: NextAlarmNotice.kt:145, RingNotifications.kt:355 |

### 미션을 못 할 때(미션 포기) 알림

| 문구 | 어떤 상황에 보이나 | 보이는 방식 | 리소스 키와 위치 |
|---|---|---|---|
| 미션을 못 하겠으면 | 울리는 중 알림이 미션 포기 문으로 바뀔 때 | 알림 제목 | `escape_title` (alarm:91)<br>쓰는 곳: RingNotifications.kt:183 |
| 여기를 누르고 문장을 따라 쓰면 미션 없이 알람이 해제돼요 | 같은 알림(펼치면 아래에 지금 단계 글이 붙음) | 알림 본문 | `escape_text` (alarm:92)<br>쓰는 곳: RingNotifications.kt:166 |
| 알람을 해제했어요 | M5에서 문장을 다 따라 써서 해제했을 때 | 알림 제목 | `escape_done` (alarm:96)<br>쓰는 곳: MissionEscapeNotifier.kt:64 |
| 미션 포기 | (쓰는 곳 없음) | 알림 버튼 | `escape_action` (alarm:94)<br>쓰는 곳: 없음 |
| 조금 달라요. 한 번 더 따라 써 주세요. | (쓰는 곳 없음, 옛 알림 안 답장 칸 문구) | 알림 글 | `escape_wrong` (alarm:93)<br>쓰는 곳: 없음 |
| 띄어쓰기와 마침표까지 똑같이 써 주세요 | (쓰는 곳 없음, 옛 알림 안 답장 칸 문구) | 알림 글 | `escape_hint` (alarm:95)<br>쓰는 곳: 없음 |
| 1) 지금은 미션을 할 수 없어서 알람을 끄지만 내일 아침에는 꼭 제시간에 일어날게요.<br>2) 오늘은 몸이 좋지 않아서 미션 대신 이 문장을 써요. 끄고 나서 다시 눕지 않을게요.<br>3) 옆 사람이 자고 있어서 미션을 못 해요. 알람이 꺼지면 바로 일어나 물 한 잔 마실게요.<br>4) 미션을 건너뛰는 건 오늘 한 번뿐이에요. 이불 밖으로 나와서 창문을 열어 볼게요.<br>5) 이 문장을 끝까지 따라 쓰는 동안 잠이 조금 깼어요. 이제 일어나서 하루를 시작할게요.<br>6) 알람을 이렇게 끄는 대신 다시 잠들지 않기로 약속해요. 지금 바로 불을 켜고 일어날게요.<br>7) 오늘은 사정이 있어서 미션을 못 하지만 일어날 시간인 건 알아요. 천천히 몸을 일으킬게요.<br>8) 지금 이 문장을 쓰고 있다면 이미 눈은 떴어요. 알람을 끄고 나서 세수부터 하러 갈게요.<br>9) 미션 대신 문장을 쓰는 건 쉬운 길이 아니에요. 다음 알람에는 미션으로 끝내 볼게요.<br>10) 알람이 꺼져도 다시 잠들면 소용없어요. 휴대폰을 내려놓고 자리에서 일어나 기지개를 켤게요.<br>11) 손가락을 움직이다 보니 머리도 조금씩 맑아져요. 알람을 끄면 바로 침대에서 내려올게요.<br>12) 오늘 아침은 미션 대신 이 문장으로 대신해요. 끄고 나면 커튼을 걷고 햇빛을 볼게요.<br>13) 잠깐 눈을 감으면 또 잠들 걸 알아요. 그래서 알람을 끄자마자 거실로 걸어 나갈게요.<br>14) 미션을 못 하는 날도 있어요. 그래도 일어나는 건 미루지 않고 지금 바로 몸을 일으킬게요.<br>15) 이 문장을 다 쓰면 알람이 꺼져요. 꺼진 뒤에는 휴대폰을 멀리 두고 물부터 마실게요.<br>16) 아직 졸리지만 하루를 시작할 시간이에요. 알람을 끈 뒤 가볍게 목과 어깨를 풀어 볼게요.<br>17) 오늘은 미션을 건너뛰지만 늦잠은 건너뛰지 않을게요. 일어나서 창문을 활짝 열 거예요.<br>18) 문장을 천천히 따라 쓰면서 정신을 차리고 있어요. 다 쓰고 나면 바로 이불을 갤게요.<br>19) 알람을 끄는 방법은 바뀌어도 일어나는 시간은 그대로예요. 지금 자리에서 일어날게요.<br>20) 다시 누우면 오늘 계획이 밀려요. 알람이 꺼지면 바로 일어나서 아침을 챙겨 먹을게요. | M5 미션 포기 화면에서 따라 쓸 문장 20개 중 하나. 지금은 시험용 문장만 써서 실제로는 안 보임(결정 172) | 따라 쓸 문장 | `escape_sentences` (alarm:98)<br>쓰는 곳: MissionEscapeStore.kt:29, MissionEscapeStore.kt:33 |

### 놓친 알람, 강제 종료, 내일 알람 점검

| 문구 | 어떤 상황에 보이나 | 보이는 방식 | 리소스 키와 위치 |
|---|---|---|---|
| 알람을 놓쳤어요 | 알람을 놓쳤을 때 | 알림 제목 | `missed_title` (alarm:55)<br>쓰는 곳: RingNotifications.kt:486 |
| %1$s 알람을 해제하지 않아서 멈췄어요 | 아무도 해제하지 않아 멈췄을 때. 예: '오전 6:30 알람을 해제하지 않아서 멈췄어요' | 알림 본문 | `missed_text_ignored` (alarm:56)<br>쓰는 곳: RingNotifications.kt:479 |
| %1$s 알람은 폰이 꺼져 있는 동안 지나갔어요 | 폰이 꺼져 있었을 때 | 알림 본문 | `missed_text_powered_off` (alarm:57)<br>쓰는 곳: RingNotifications.kt:480 |
| %1$s 알람은 앱이 닫혀 있는 동안 지나갔어요 | 앱이 닫혀 있었을 때 | 알림 본문 | `missed_text_app_closed` (alarm:58)<br>쓰는 곳: RingNotifications.kt:481 |
| 앱이 강제로 닫혀 알람이 꺼져 있었어요 | N2 앱이 강제 종료된 뒤 다시 열렸을 때 | 알림 제목 | `force_stopped_title` (alarm:60)<br>쓰는 곳: RingNotifications.kt:503 |
| 지금 다시 맞췄어요 | 같은 알림 | 알림 본문 | `force_stopped_text` (alarm:61)<br>쓰는 곳: RingNotifications.kt:504 |
| 내일 알람이 울리지 않을 수 있어요 | N1 밤 9시 내일 알람 점검(배터리 문제) | 알림 제목 | `battery_reminder_title` (alarm:80)<br>쓰는 곳: BatteryReminder.kt:93 |
| 내일 알람이 소리 없이 울릴 수 있어요 | N1 밤 9시, 방해 금지 모드 문제일 때 | 알림 제목 | `tomorrow_title_silent` (alarm:83)<br>쓰는 곳: BatteryReminder.kt:92 |
| 배터리 사용 제한이 켜져 있어요. 눌러서 풀어 주세요. | N1 배터리 사용 제한이 켜졌을 때 | 알림 본문 | `battery_reminder_text` (alarm:81)<br>쓰는 곳: BatteryReminder.kt:124 |
| 배터리 사용이 '제한'으로 되어 있어요 | N1 배터리가 '제한'일 때 | 알림 본문 | `tomorrow_text_restricted` (alarm:84)<br>쓰는 곳: BatteryReminder.kt:117 |
| 방해 금지 모드가 알람을 막고 있어요 | N1 방해 금지 모드가 막을 때 | 알림 본문 | `tomorrow_text_silenced` (alarm:85)<br>쓰는 곳: BatteryReminder.kt:118 |
| 배터리가 %1$d% 남았어요. 자기 전에 충전해 주세요 | N1 배터리가 적을 때. 예: '배터리가 15% 남았어요. 자기 전에 충전해 주세요' | 알림 본문 | `tomorrow_text_low_battery` (alarm:86)<br>쓰는 곳: BatteryReminder.kt:120 |
| %1$s. 그 밖에 %2$d개 더 | N1 문제가 여러 개일 때. 예: '배터리 사용이 '제한'으로 되어 있어요. 그 밖에 1개 더' | 알림 본문 | `tomorrow_more` (alarm:87)<br>쓰는 곳: BatteryReminder.kt:97 |

### N3 다음 알람 항상 표시

| 문구 | 어떤 상황에 보이나 | 보이는 방식 | 리소스 키와 위치 |
|---|---|---|---|
| 다음 알람 %1$s | 설정 '다음 알람 항상 표시'를 켜면 늘 떠 있음. 예: '다음 알람 목요일 오전 6:30' | 알림 제목 | `next_alarm_title` (alarm:68)<br>쓰는 곳: NextAlarmNotice.kt:104 |
| %1$s %2$s | 제목과 본문 속 때. 예: '목요일 오전 6:30' | 알림 글 조각 | `next_alarm_when` (alarm:70)<br>쓰는 곳: NextAlarmNotice.kt:119 |
| %1$d월 %2$d일 | 7일 넘게 남았을 때 요일 대신. 예: '12월 3일' | 알림 글 조각 | `next_alarm_date` (alarm:71)<br>쓰는 곳: NextAlarmNotice.kt:141 |
| %1$s 반복 | 반복 알람. 예: '월 화 수 목 금 반복' | 알림 본문 | `next_alarm_repeat` (alarm:73)<br>쓰는 곳: NextAlarmNotice.kt:131 |
| 한 번 울려요 | 한 번만 울리는 알람 | 알림 본문 | `next_alarm_once` (alarm:74)<br>쓰는 곳: NextAlarmNotice.kt:129 |
| %1$s은 쉬어요. %2$s에 울려요 | 이번만 쉬는 중. 예: '목요일은 쉬어요. 금요일 오전 6:30에 울려요' | 알림 본문 | `next_alarm_rested` (alarm:76)<br>쓰는 곳: NextAlarmNotice.kt:125 |

### 잠들기 재생 알림

| 문구 | 어떤 상황에 보이나 | 보이는 방식 | 리소스 키와 위치 |
|---|---|---|---|
| 끄기 | 잠들기 소리를 트는 동안 알림 | 알림 버튼 | `sleep_stop` (alarm:12)<br>쓰는 곳: SleepPlaybackService.kt:106 |

### 접근성 서비스 (폰 설정, 접근성, 설치된 앱)

| 문구 | 어떤 상황에 보이나 | 보이는 방식 | 리소스 키와 위치 |
|---|---|---|---|
| 일어나 전원 종료 방지 | 폰 설정 접근성 목록 | 서비스 이름 | `power_guard_service_label` (alarm:62)<br>쓰는 곳: AndroidManifest.xml:31 |
| 알람이 울리는 동안 전원 메뉴가 열리면 바로 닫아요. 화면 내용은 읽거나 저장하지 않아요. | 같은 목록에서 누르면 | 서비스 설명 | `power_guard_service_desc` (alarm:63)<br>쓰는 곳: power_guard_service.xml:7 |

## 11. 여러 화면이 같이 쓰는 이름과 형식

### 시각, 날짜, 길이 형식

| 문구 | 어떤 상황에 보이나 | 보이는 방식 | 리소스 키와 위치 |
|---|---|---|---|
| 오전 | 앱 화면 속 시각 앞 | 시각 조각 | `ui_am` (ui:3)<br>쓰는 곳: AlarmText.kt:27, AlarmText.kt:35 |
| 오후 | 앱 화면 속 시각 앞 | 시각 조각 | `ui_pm` (ui:4)<br>쓰는 곳: AlarmText.kt:27, AlarmText.kt:35 |
| %1$s %2$d:%3$02d | 홈 카드, 편집, 토스트 속 시각. 예: '오전 6:30' | 시각 형식 | `ui_time_12h` (ui:6)<br>쓰는 곳: AlarmText.kt:28 |
| 1) 월<br>2) 화<br>3) 수<br>4) 목<br>5) 금<br>6) 토<br>7) 일 | 요일 칸, 카드 요일, 날짜 괄호 속(월요일부터 일요일 순서) | 요일 글자 | `ui_day_short` (ui:9)<br>쓰는 곳: AlarmText.kt:39 |
| %1$d월 %2$d일 (%3$s) | 홈 맨 위 보조 글 등. 예: '11월 16일 (월)' | 날짜 형식 | `ui_date_short` (ui:20)<br>쓰는 곳: AlarmText.kt:44 |
| %1$d월 %2$d일 %3$s요일 | D0 울림 화면 날짜. 예: '11월 16일 월요일' | 날짜 형식 | `ui_date_long` (ui:23)<br>쓰는 곳: AlarmText.kt:53 |
| %1$d분 | 길이 표시. 예: '1분 30초'의 '1분' | 길이 조각 | `ui_duration_minutes` (ui:26)<br>쓰는 곳: AlarmText.kt:64 |
| %1$d초 | 길이 표시. 예: '30초' | 길이 조각 | `ui_duration_seconds` (ui:27)<br>쓰는 곳: AlarmText.kt:65 |
| %1$d:%2$02d | 남은 시간. 예: '0:42' | 시간 형식 | `ui_countdown` (ui:29)<br>쓰는 곳: AlarmText.kt:73 |
| %1$d일 | '몇 일 몇 시간 몇 분 후'의 일. 예: '2일' | 길이 조각 | `ui_until_days` (ui:32)<br>쓰는 곳: AlarmText.kt:79 |
| %1$d시간 | 같은 문장의 시간. 예: '7시간' | 길이 조각 | `ui_until_hours` (ui:33)<br>쓰는 곳: AlarmText.kt:80 |
| %1$d분 | 같은 문장의 분. 예: '20분' | 길이 조각 | `ui_until_minutes` (ui:34)<br>쓰는 곳: AlarmText.kt:81 |
| 한 번 | 홈 카드, 편집 반복 값 | 반복 이름 | `ui_repeat_once` (ui:36)<br>쓰는 곳: AlarmText.kt:88 |
| 매일 | 홈 카드, 편집 반복 값 | 반복 이름 | `ui_repeat_everyday` (ui:37)<br>쓰는 곳: AlarmText.kt:89 |
| 평일 | 홈 카드, 편집 반복 값 | 반복 이름 | `ui_repeat_weekdays` (ui:38)<br>쓰는 곳: AlarmText.kt:90 |
| 주말 | 홈 카드, 편집 반복 값 | 반복 이름 | `ui_repeat_weekend` (ui:39)<br>쓰는 곳: AlarmText.kt:91 |

### 미션 이름과 Pop Quiz 분야

| 문구 | 어떤 상황에 보이나 | 보이는 방식 | 리소스 키와 위치 |
|---|---|---|---|
| Drop Words | 홈 카드, 편집, 미션 선택, 미션 화면 | 미션 이름 | `ui_mission_drop_words` (ui:43)<br>쓰는 곳: AlarmText.kt:99 |
| Pop Quiz | 홈 카드, 편집, 미션 선택, 미션 화면 | 미션 이름 | `ui_mission_pop_quiz` (ui:42)<br>쓰는 곳: AlarmText.kt:98 |
| Quick Math | 홈 카드, 편집, 미션 선택, 미션 화면 | 미션 이름 | `ui_mission_quick_math` (ui:41)<br>쓰는 곳: AlarmText.kt:97 |
| Shake Shake | 홈 카드, 편집, 미션 선택, 미션 화면 | 미션 이름 | `ui_mission_shake_shake` (ui:44)<br>쓰는 곳: AlarmText.kt:100 |
| 없음 | 홈 카드, 편집 미션 값 | 값 | `ui_mission_none` (ui:45)<br>쓰는 곳: AlarmText.kt:106 |
| %1$s 외 %2$d개 | 미션이 여럿일 때. 예: 'Drop Words 외 1개' | 값 | `ui_mission_more` (ui:47)<br>쓰는 곳: AlarmText.kt:109, AlarmText.kt:130 |
| 경제 용어 | M2 위 분야 글자, Pop Quiz 시트 분야 칩 | 분야 이름 | `ui_quiz_category_economy` (ui:67)<br>쓰는 곳: AlarmText.kt:117 |
| 사회 용어 | 같음 | 분야 이름 | `ui_quiz_category_society` (ui:68)<br>쓰는 곳: AlarmText.kt:118 |
| 국사 | 같음 | 분야 이름 | `ui_quiz_category_korean_history` (ui:69)<br>쓰는 곳: AlarmText.kt:119 |
| 세계사 | 같음 | 분야 이름 | `ui_quiz_category_world_history` (ui:70)<br>쓰는 곳: AlarmText.kt:120 |
| 과학 용어 | 같음 | 분야 이름 | `ui_quiz_category_science` (ui:71)<br>쓰는 곳: AlarmText.kt:121 |
| 인물 | 같음 | 분야 이름 | `ui_quiz_category_people` (ui:72)<br>쓰는 곳: AlarmText.kt:122 |
| 사회 이슈 | 같음 | 분야 이름 | `ui_quiz_category_issues` (ui:73)<br>쓰는 곳: AlarmText.kt:123 |

### 알람 화면 배경 이름 (S8, O4-2)

| 문구 | 어떤 상황에 보이나 | 보이는 방식 | 리소스 키와 위치 |
|---|---|---|---|
| 하늘 | S8, O4-2 묶음 칩 | 묶음 이름 | `ui_bg_group_sky` (ui:49)<br>쓰는 곳: AlarmBackgrounds.kt:21 |
| 자연 | 같음 | 묶음 이름 | `ui_bg_group_nature` (ui:50)<br>쓰는 곳: AlarmBackgrounds.kt:22 |
| 단색 | 같음 | 묶음 이름 | `ui_bg_group_solid` (ui:51)<br>쓰는 곳: AlarmBackgrounds.kt:23 |
| 내 사진 | 같음 | 묶음 이름 | `ui_bg_group_photo` (ui:52)<br>쓰는 곳: AlarmBackgrounds.kt:24 |
| 내 사진 | S8 내 사진 타일 | 타일 이름 | `ui_bg_my_photo` (ui:65)<br>쓰는 곳: BackgroundPage.kt:217 |
| 시간대 하늘 | S8, O4-2 배경 타일 이름 | 타일 이름 | `ui_bg_time_sky` (ui:53)<br>쓰는 곳: AlarmBackgrounds.kt:48 |
| 아침 노을 | S8, O4-2 배경 타일 이름 | 타일 이름 | `ui_bg_sunrise` (ui:54)<br>쓰는 곳: AlarmBackgrounds.kt:49 |
| 새벽 남색 | S8, O4-2 배경 타일 이름 | 타일 이름 | `ui_bg_dawn` (ui:55)<br>쓰는 곳: AlarmBackgrounds.kt:50 |
| 맑은 하늘 | S8, O4-2 배경 타일 이름 | 타일 이름 | `ui_bg_clear` (ui:56)<br>쓰는 곳: AlarmBackgrounds.kt:51 |
| 밤하늘 | S8, O4-2 배경 타일 이름 | 타일 이름 | `ui_bg_night` (ui:57)<br>쓰는 곳: AlarmBackgrounds.kt:52 |
| 민트 | S8, O4-2 배경 타일 이름 | 타일 이름 | `ui_bg_mint` (ui:58)<br>쓰는 곳: AlarmBackgrounds.kt:53 |
| 숲 | S8, O4-2 배경 타일 이름 | 타일 이름 | `ui_bg_forest` (ui:59)<br>쓰는 곳: AlarmBackgrounds.kt:54 |
| 라임 | S8, O4-2 배경 타일 이름 | 타일 이름 | `ui_bg_lime` (ui:60)<br>쓰는 곳: AlarmBackgrounds.kt:55 |
| 복숭아 | S8, O4-2 배경 타일 이름 | 타일 이름 | `ui_bg_peach` (ui:61)<br>쓰는 곳: AlarmBackgrounds.kt:56 |
| 연한 회색 | S8, O4-2 배경 타일 이름 | 타일 이름 | `ui_bg_gray` (ui:62)<br>쓰는 곳: AlarmBackgrounds.kt:57 |
| 모래 | S8, O4-2 배경 타일 이름 | 타일 이름 | `ui_bg_sand` (ui:63)<br>쓰는 곳: AlarmBackgrounds.kt:58 |
| 먹색 | S8, O4-2 배경 타일 이름 | 타일 이름 | `ui_bg_ink` (ui:64)<br>쓰는 곳: AlarmBackgrounds.kt:59 |

### 알람음 이름 (S5, O4-3)

| 문구 | 어떤 상황에 보이나 | 보이는 방식 | 리소스 키와 위치 |
|---|---|---|---|
| 기본 | S5, O4-3 묶음 칩 | 묶음 이름 | `ringtone_group_basic` (alarm/sound_names:4)<br>쓰는 곳: SoundNames.kt:13 |
| 잔잔한 | S5, O4-3 묶음 칩 | 묶음 이름 | `ringtone_group_calm` (alarm/sound_names:5)<br>쓰는 곳: SoundNames.kt:14 |
| 경쾌한 | S5, O4-3 묶음 칩 | 묶음 이름 | `ringtone_group_upbeat` (alarm/sound_names:6)<br>쓰는 곳: SoundNames.kt:15 |
| 시끄러운 | S5, O4-3 묶음 칩 | 묶음 이름 | `ringtone_group_loud` (alarm/sound_names:7)<br>쓰는 곳: SoundNames.kt:16 |
| 웅장한 | S5, O4-3 묶음 칩 | 묶음 이름 | `ringtone_group_grand` (alarm/sound_names:8)<br>쓰는 곳: SoundNames.kt:17 |
| 신비로운 | S5, O4-3 묶음 칩 | 묶음 이름 | `ringtone_group_mystic` (alarm/sound_names:9)<br>쓰는 곳: SoundNames.kt:18 |
| 크리스마스 | S5, O4-3 묶음 칩 | 묶음 이름 | `ringtone_group_christmas` (alarm/sound_names:10)<br>쓰는 곳: SoundNames.kt:19 |
| 랜덤 | S5, O4-3 알람음 목록, S0 알람음 줄 값 | 목록 줄 이름 | `ringtone_random` (alarm/sound_names:11)<br>쓰는 곳: SoundNames.kt:22 |
| 기본 알람음 | S5, O4-3 알람음 목록, S0 알람음 줄 값 | 목록 줄 이름 | `ringtone_default` (alarm/sound_names:12)<br>쓰는 곳: SoundNames.kt:23 |
| 고요한 새벽빛 | S5, O4-3 알람음 목록, S0 알람음 줄 값 | 목록 줄 이름 | `ringtone_aube_paisible` (alarm/sound_names:13)<br>쓰는 곳: SoundNames.kt:24 |
| 보드라운 아침 | S5, O4-3 알람음 목록, S0 알람음 줄 값 | 목록 줄 이름 | `ringtone_douceur` (alarm/sound_names:14)<br>쓰는 곳: SoundNames.kt:25 |
| 봄날의 창가 | S5, O4-3 알람음 목록, S0 알람음 줄 값 | 목록 줄 이름 | `ringtone_primavera` (alarm/sound_names:15)<br>쓰는 곳: SoundNames.kt:26 |
| 아침 숲 산책 | S5, O4-3 알람음 목록, S0 알람음 줄 값 | 목록 줄 이름 | `ringtone_petit_bois` (alarm/sound_names:16)<br>쓰는 곳: SoundNames.kt:27 |
| 나뭇잎 이슬 | S5, O4-3 알람음 목록, S0 알람음 줄 값 | 목록 줄 이름 | `ringtone_gouttes_de_bois` (alarm/sound_names:17)<br>쓰는 곳: SoundNames.kt:28 |
| 햇살 인사 | S5, O4-3 알람음 목록, S0 알람음 줄 값 | 목록 줄 이름 | `ringtone_bonjour_soleil` (alarm/sound_names:18)<br>쓰는 곳: SoundNames.kt:29 |
| 굿모닝 카페 | S5, O4-3 알람음 목록, S0 알람음 줄 값 | 목록 줄 이름 | `ringtone_buongiorno` (alarm/sound_names:19)<br>쓰는 곳: SoundNames.kt:30 |
| 콧노래 | S5, O4-3 알람음 목록, S0 알람음 줄 값 | 목록 줄 이름 | `ringtone_joie_de_vivre` (alarm/sound_names:20)<br>쓰는 곳: SoundNames.kt:31 |
| 가벼운 발걸음 | S5, O4-3 알람음 목록, S0 알람음 줄 값 | 목록 줄 이름 | `ringtone_allegro_vivace` (alarm/sound_names:21)<br>쓰는 곳: SoundNames.kt:32 |
| 바쁜 아침 | S5, O4-3 알람음 목록, S0 알람음 줄 값 | 목록 줄 이름 | `ringtone_presto` (alarm/sound_names:22)<br>쓰는 곳: SoundNames.kt:33 |
| 말랑 통통 | S5, O4-3 알람음 목록, S0 알람음 줄 값 | 목록 줄 이름 | `ringtone_petits_rebonds` (alarm/sound_names:23)<br>쓰는 곳: SoundNames.kt:34 |
| 시골길 드라이브 | S5, O4-3 알람음 목록, S0 알람음 줄 값 | 목록 줄 이름 | `ringtone_route_de_campagne` (alarm/sound_names:24)<br>쓰는 곳: SoundNames.kt:35 |
| 구름 낀 햇살 | S5, O4-3 알람음 목록, S0 알람음 줄 값 | 목록 줄 이름 | `ringtone_soleil_mineur` (alarm/sound_names:25)<br>쓰는 곳: SoundNames.kt:36 |
| 정신 번쩍 | S5, O4-3 알람음 목록, S0 알람음 줄 값 | 목록 줄 이름 | `ringtone_fortissimo` (alarm/sound_names:26)<br>쓰는 곳: SoundNames.kt:37 |
| 벌떡 기상 | S5, O4-3 알람음 목록, S0 알람음 줄 값 | 목록 줄 이름 | `ringtone_nuit_blanche` (alarm/sound_names:27)<br>쓰는 곳: SoundNames.kt:38 |
| 북소리 행진 | S5, O4-3 알람음 목록, S0 알람음 줄 값 | 목록 줄 이름 | `ringtone_tambour_battant` (alarm/sound_names:28)<br>쓰는 곳: SoundNames.kt:39 |
| 빨간 경보 | S5, O4-3 알람음 목록, S0 알람음 줄 값 | 목록 줄 이름 | `ringtone_neon_rouge` (alarm/sound_names:29)<br>쓰는 곳: SoundNames.kt:40 |
| 왕의 아침 | S5, O4-3 알람음 목록, S0 알람음 줄 값 | 목록 줄 이름 | `ringtone_maestoso` (alarm/sound_names:30)<br>쓰는 곳: SoundNames.kt:41 |
| 개선 행진 | S5, O4-3 알람음 목록, S0 알람음 줄 값 | 목록 줄 이름 | `ringtone_marche_triomphale` (alarm/sound_names:31)<br>쓰는 곳: SoundNames.kt:42 |
| 장엄한 서막 | S5, O4-3 알람음 목록, S0 알람음 줄 값 | 목록 줄 이름 | `ringtone_grandeur` (alarm/sound_names:32)<br>쓰는 곳: SoundNames.kt:43 |
| 꿈결 | S5, O4-3 알람음 목록, S0 알람음 줄 값 | 목록 줄 이름 | `ringtone_reverie` (alarm/sound_names:33)<br>쓰는 곳: SoundNames.kt:44 |
| 그림자 왈츠 | S5, O4-3 알람음 목록, S0 알람음 줄 값 | 목록 줄 이름 | `ringtone_danse_des_ombres` (alarm/sound_names:34)<br>쓰는 곳: SoundNames.kt:45 |
| 산타의 종소리 | S5, O4-3 알람음 목록, S0 알람음 줄 값 | 목록 줄 이름 | `ringtone_joyeux_noel` (alarm/sound_names:35)<br>쓰는 곳: SoundNames.kt:46 |
| 눈 내리는 밤 | S5, O4-3 알람음 목록, S0 알람음 줄 값 | 목록 줄 이름 | `ringtone_nuit_de_noel` (alarm/sound_names:36)<br>쓰는 곳: SoundNames.kt:47 |

### 잠들기 콘텐츠 이름 (Z, 지금 숨김)

| 문구 | 어떤 상황에 보이나 | 보이는 방식 | 리소스 키와 위치 |
|---|---|---|---|
| 잠에 들기 어려울 때 | Z 묶음 제목, 재생 화면 보조 글 | 묶음 이름 | `sleep_group_hard_to_sleep` (alarm/sound_names:37)<br>쓰는 곳: SoundNames.kt:50 |
| 마음이 답답할 때 | Z 묶음 제목, 재생 화면 보조 글 | 묶음 이름 | `sleep_group_heavy_heart` (alarm/sound_names:38)<br>쓰는 곳: SoundNames.kt:51 |
| 생각이 복잡할 때 | Z 묶음 제목, 재생 화면 보조 글 | 묶음 이름 | `sleep_group_busy_mind` (alarm/sound_names:39)<br>쓰는 곳: SoundNames.kt:52 |
| 아침 5분 명상 | Z 묶음 제목, 재생 화면 보조 글 | 묶음 이름 | `sleep_group_morning_meditation` (alarm/sound_names:40)<br>쓰는 곳: SoundNames.kt:53 |
| 창문을 두드리는 밤비 | Z 트랙 타일, 재생 화면 제목, 잠들기 재생 알림 제목 | 트랙 이름 | `sleep_track_night_rain` (alarm/sound_names:41)<br>쓰는 곳: SoundNames.kt:56 |
| 멀리서 밀려오는 파도 | Z 트랙 타일, 재생 화면 제목, 잠들기 재생 알림 제목 | 트랙 이름 | `sleep_track_waves` (alarm/sound_names:42)<br>쓰는 곳: SoundNames.kt:57 |
| 새벽 숲의 새소리 | Z 트랙 타일, 재생 화면 제목, 잠들기 재생 알림 제목 | 트랙 이름 | `sleep_track_forest_birds` (alarm/sound_names:43)<br>쓰는 곳: SoundNames.kt:58 |
| 타닥이는 모닥불 | Z 트랙 타일, 재생 화면 제목, 잠들기 재생 알림 제목 | 트랙 이름 | `sleep_track_campfire` (alarm/sound_names:44)<br>쓰는 곳: SoundNames.kt:59 |
| 깊은 밤, 고요한 숨 | Z 트랙 타일, 재생 화면 제목, 잠들기 재생 알림 제목 | 트랙 이름 | `sleep_track_deep_night` (alarm/sound_names:45)<br>쓰는 곳: SoundNames.kt:60 |
| 구름 위를 천천히 걷기 | Z 트랙 타일, 재생 화면 제목, 잠들기 재생 알림 제목 | 트랙 이름 | `sleep_track_cloud_walk` (alarm/sound_names:46)<br>쓰는 곳: SoundNames.kt:61 |
| 느리게 쉬는 숨 | Z 트랙 타일, 재생 화면 제목, 잠들기 재생 알림 제목 | 트랙 이름 | `sleep_track_slow_breath` (alarm/sound_names:47)<br>쓰는 곳: SoundNames.kt:62 |
| 별빛 아래 들판 | Z 트랙 타일, 재생 화면 제목, 잠들기 재생 알림 제목 | 트랙 이름 | `sleep_track_starry_field` (alarm/sound_names:48)<br>쓰는 곳: SoundNames.kt:63 |
| 고요한 방 | Z 트랙 타일, 재생 화면 제목, 잠들기 재생 알림 제목 | 트랙 이름 | `sleep_track_quiet_room` (alarm/sound_names:49)<br>쓰는 곳: SoundNames.kt:64 |
| 달빛 산책 | Z 트랙 타일, 재생 화면 제목, 잠들기 재생 알림 제목 | 트랙 이름 | `sleep_track_moon_walk` (alarm/sound_names:50)<br>쓰는 곳: SoundNames.kt:65 |
| 새벽 왈츠 | Z 트랙 타일, 재생 화면 제목, 잠들기 재생 알림 제목 | 트랙 이름 | `sleep_track_dawn_waltz` (alarm/sound_names:51)<br>쓰는 곳: SoundNames.kt:66 |
| 빗방울 소나타 | Z 트랙 타일, 재생 화면 제목, 잠들기 재생 알림 제목 | 트랙 이름 | `sleep_track_rain_sonata` (alarm/sound_names:52)<br>쓰는 곳: SoundNames.kt:67 |
| 새벽 2시 | Z 트랙 타일, 재생 화면 제목, 잠들기 재생 알림 제목 | 트랙 이름 | `sleep_track_two_am` (alarm/sound_names:53)<br>쓰는 곳: SoundNames.kt:68 |
| 골목 카페 | Z 트랙 타일, 재생 화면 제목, 잠들기 재생 알림 제목 | 트랙 이름 | `sleep_track_alley_cafe` (alarm/sound_names:54)<br>쓰는 곳: SoundNames.kt:69 |
| 밤 버스 | Z 트랙 타일, 재생 화면 제목, 잠들기 재생 알림 제목 | 트랙 이름 | `sleep_track_night_bus` (alarm/sound_names:55)<br>쓰는 곳: SoundNames.kt:70 |
| 느린 오후 | Z 트랙 타일, 재생 화면 제목, 잠들기 재생 알림 제목 | 트랙 이름 | `sleep_track_slow_afternoon` (alarm/sound_names:56)<br>쓰는 곳: SoundNames.kt:71 |

## 12. 코드에 직접 적힌 한국어

리소스 파일이 아니라 Kotlin 코드에 바로 적혀 있는 글이에요. [개발 28]의 TSK-273 점검(`docs/handover/dev-28.md`) 뒤 남은 것을 다시 찾았어요(`rg "[가-힣]"`, 시험 코드와 주석 뺌).

### 사용자에게 보이는 것

| 문구 | 어떤 상황에 보이나 | 보이는 방식 | 위치 |
|---|---|---|---|
| 테스트 문장 12345, @@$$ | M5 미션 포기 화면에서 따라 쓸 문장. 시험하는 동안은 이 문장 하나만 나와요(결정 172). 출시 전에 `escape_sentences` 20개로 바꿔야 해요 | 따라 쓸 문장 | `MissionEscape.kt:12` (`TEST_SENTENCE`), `MissionEscapeStore.kt:35`, `EscapeViewModel.kt:49` |
| 월, 화, 수, 목, 금, 토, 일 / 월요일 ~ 일요일 | N3 다음 알람 알림 속 요일. 예: '월 화 수 목 금 반복', '목요일 오전 6:30'. 리소스가 아니라 자바의 한국어 요일 이름(`Locale.KOREAN`)을 써요 | 알림 글 조각 | `NextAlarmNotice.kt:132`(짧은 요일), `NextAlarmNotice.kt:139`(긴 요일) |
| 가나다라마바사아자차카타파하 | M1 Drop Words 글자 자판에 채울 글자가 모자랄 때 채우는 글자 | 자판 글자 | `SyllableKeypad.kt:83` (`FALLBACK`) |

### 사용자에게 보이지 않는 것 (참고)

| 글 | 무엇 | 위치 |
|---|---|---|
| 예: | 미션 시트 예제 문구 앞 '예: '를 떼는 코드. 영어 문구를 넣으면 같이 바꿔야 해요 | `MissionExample.kt:94` |
| 간격은 … 중 하나예요, 횟수는 0 이상이에요 | 개발용 오류 메시지 | `Alarm.kt:16`, `Alarm.kt:17` |
| 예문이 없어요, 단어가 없어요, 문제가 없어요, 문제를 만들지 못했어요, reduce에서 처리했어요 | 개발용 오류 메시지 | `MissionEscape.kt:35`, `DropWords.kt:85`, `PopQuiz.kt:96`, `QuickMath.kt:62`, `RingStateMachine.kt:93` |

## 13. 디버그 전용

- `app/src/debug`에는 문구 파일이 없고, 코드(`AlarmTestCases.kt`, `DebugCommandReceiver.kt`)에도 화면에 나오는 한국어가 없어요.
- 확인용 앱 두 개의 이름('일어나 미션', '일어나 온보딩')은 1절 맨 위에 같이 적었어요. 출시 앱에는 들어가지 않아요.
- 미션 확인용 앱에서만 보이는 글: `mission_try_title`('미션'), `edit_mission_start`('시작하기')는 4절 S1에 있어요.

## 14. 미션 콘텐츠 (참고)

화면 문구가 아니라 미션에 나오는 한국어 내용이라 줄마다 적지 않았어요. 바꿀 때는 이 파일을 봐요.

| 무엇 | 양 | 위치 |
|---|---|---|
| Pop Quiz 문제, 정답, 오답 | 350문제 (1~7단계마다 50문제). 분야: 과학 용어 64, 경제 용어 56, 국사 55, 사회 용어 55, 세계사 45, 인물 38, 사회 이슈 37 | `feature/mission/src/main/res/raw/mission_content.json` (`popQuiz`) |
| Drop Words 떨어지는 단어 | 1~7단계 단어 목록 | 같은 파일 (`dropWords`) |
| Quick Math 문제 | 코드가 숫자로 만들어서 글이 없어요 | `QuickMath.kt` |
| 미션 포기 따라 쓸 문장 | 20개 (10절 `escape_sentences`) + 시험용 1개 (12절) | `core/alarm` strings.xml |

## 15. 점검 메모

고치지 않고 적기만 했어요. 고칠 때는 기획에서 새 문구를 정한 뒤 개발 방에 넘겨요.

### 15-1. 문구 기준(`docs/plan-11-wording.md`)에 안 맞는 것

| # | 리소스 | 지금 문구 | 안 맞는 점 |
|---|---|---|---|
| 1 | `faq_c5_q` | 알람을 끈 뒤 다시 잠들까 봐 걱정돼요 | 울리는 알람을 멈추는 것은 '해제'예요. '알람을 해제한 뒤'가 기준에 맞아요 |
| 2 | `settings_lab_escape_sheet_desc` | …미션을 끝내야 알람이 꺼져요. | '해제돼요'가 기준이에요 |
| 3 | `escape_sentences` 20개 중 13개(1, 2, 3, 6, 8, 10, 11, 12, 13, 15, 16, 19, 20번) | 예: 알람을 끄지만 / 알람이 꺼지면 | 따라 쓰는 문장이라 말맛을 살린 것일 수 있지만 기준으로는 '해제'예요. 10번과 15번에는 '휴대폰'이 나와요(다른 화면은 '폰') |
| 4 | `channel_sleep_description` | …재생 조절을 보여 줘요 | 다른 채널 설명은 모두 '보여줘요'로 붙여 써요 |
| 5 | 띄어쓰기 섞임 | '켜 주세요, 써 주세요, 허용해 주세요, 풀어 주세요, 해제해 주세요(home/edit 토스트)' 와 '해제해주세요, 눌러주세요, 흔들어주세요' | 사용자가 준 문구는 붙여 썼고, 옛 문구는 띄어 썼어요. 한쪽으로 맞출지 정해야 해요 |
| 6 | `first_run_perm_alarm` | 알람 | O5에서 '알림'과 '전체 화면 알림' 권한을 묶은 줄 이름이에요. 기준상 폰 권한 이름은 '알림'이라 헷갈릴 수 있어요 |
| 7 | '해제'가 두 뜻 | `pl_battery`, `first_run_perm_battery` '배터리 사용 제한 해제', `faq_a5_a` '배터리 사용 제한을 해제해 주세요', `pl_subject_battery` | 기준에서 '해제'는 울리는 알람을 멈추는 말인데 배터리 제한에도 써요 |

### 15-2. 같은 뜻인데 다르게 쓴 것

| # | 뜻 | 다르게 쓴 문구 (리소스) |
|---|---|---|
| 1 | 배터리 제한을 없애기 | '배터리 사용 제한 없음'(`permission_battery`, T2), '배터리 사용 제한 해제'(`pl_battery`, `first_run_perm_battery`), '풀기'(`banner_release`), '눌러서 풀어 주세요'(`battery_reminder_text`), ''제한 없음'인지'(`faq_a1_a`) |
| 2 | 방해 금지 모드에서 울리게 하기 | '방해 금지 모드에서 허용하기'(`settings_dnd_override`, `dnd_guide_title`), '방해 금지 모드에서 허용'(`permission_dnd`, T2), '알람이 울리는 동안만 허용'(`dnd_switch_title`), ''알람' 허용을 켜 주세요'(`banner_silenced_desc`) |
| 3 | 전원 종료 방지 설명 | '알람이 울리는 동안 전원 메뉴를 막아요'(`settings_power_guard_desc`, `permission_power_desc`), '…전원 메뉴가 열리면 바로 닫아요'(`first_run_perm_power_desc`, `power_guard_service_desc`, `faq_b7_a`), '…전원 메뉴를 막으려면'(`power_sheet_title`) |
| 4 | 미션 포기 | 알림 채널 '미션을 못 할 때'(`channel_escape`), 알림 제목 '미션을 못 하겠으면'(`escape_title`), 실험실 '미션 포기 사용하기'(`settings_lab_escape`), M5 버튼 '다시 미션 수행하기'. FAQ(`faq_b1_a`, `faq_c2_a`)는 "알림 패널의 '미션 포기'를 누르고"라고 하는데, 알림에는 '미션 포기'라는 버튼이 없어요(`escape_action`은 쓰는 곳 없음). 알림 자체를 누르는 방식이라 FAQ 설명이 실제와 달라요 |
| 5 | 다시 울림 횟수 끝 | 시트 '무제한'(`edit_ring_again_unlimited`), S0 줄 값 '%1$d분, 무제한'(`edit_ring_again_until_off`), 세부 화면 '해제할 때까지'(`edit_ring_again_count_until_off`) |
| 6 | 미리 해 보기 | 미션 시트 '미리보기'(`edit_mission_preview`), 실험실 '미리 해보기'(`settings_lab_preview`, `ring_preview_top`) |
| 7 | 남은 재확인 수 | '재확인이 %1$d번 남았어요'(`home_recheck_title_count`), '남은 재확인 %1$d번'(`recheck_multi_waiting_text`, `recheck_multi_left`) |
| 8 | 곧 재확인 | '곧 재수면 여부를 재확인할게요'(홈 띠, 좀비모드 알림), '잠시 뒤 재수면 여부를 재확인할게요'(`ring_text_recheck_waiting`, `ring_multi_next`) |
| 9 | 권한 화면 이름 | 설정 줄과 FAQ는 '권한 점검'(`settings_permissions`, `faq_*_a`), 화면 제목은 '권한'(`permission_title`) |
| 10 | 확인 필요 표시 | 탭 배지 '확인 필요'(`tab_settings_alert`), 설정 줄 값 '확인이 필요해요'(`settings_needs_attention`) |
| 11 | 알람을 쉬기 | '이번만 쉬기'(H8), 반복 카드 '공휴일에는 끄기'(`edit_skip_holidays`, FAQ는 '공휴일에는 알람을 쉬고 싶어요') |
| 12 | 시각 형식 | 대부분 '오전 6:31', 설정 '알람 미리 울려 보기' 토스트만 '06:31에 울려요'(`settings_alarm_try_done`에 24시간 형식이 들어가요). N3 알림 요일은 리소스가 아닌 자바 요일 이름(12절) |
| 13 | 같은 화면 묶음의 영어 이름 | 미션 이름 'Drop Words, Pop Quiz, Quick Math, Shake Shake', 실험실 'Crazy Mode', 속도 단위 'ms'. 일부러 정한 이름이지만 영어로 남은 곳은 이것뿐이에요 |

### 15-3. 쓰는 곳이 없는 리소스 (15개)

| 리소스 | 문구 | 메모 |
|---|---|---|
| `recheck_multi_left` (alarm:50) | 남은 재확인 %1$d번 | 좀비모드 알림이 `recheck_multi_waiting_text`로 바뀜 |
| `escape_wrong` (alarm:93) | 조금 달라요. 한 번 더 따라 써 주세요. | 옛 알림 안 답장 칸(결정 171로 없앰) |
| `escape_action` (alarm:94) | 미션 포기 | 같음. FAQ가 아직 이 이름을 말함(15-2의 4) |
| `escape_hint` (alarm:95) | 띄어쓰기와 마침표까지 똑같이 써 주세요 | 같음 |
| `edit_preview` (edit:21) | 미리 해보기 | |
| `edit_minutes` (edit:33) | %1$d분 | |
| `edit_power_off` (edit:41) | 전원 종료 방지 | 설정으로 옮김 |
| `edit_power_off_value` (edit:42) | 해제할 때까지 | 같음 |
| `edit_volume` (edit:46) | 볼륨 및 진동 | S0 줄 이름이 '볼륨'(`edit_volume_title`)으로 바뀜 |
| `edit_volume_vibrate` (edit:47) | %1$d%, %2$s | 같음 |
| `edit_volume_no_vibrate` (edit:48) | %1$d%, 진동 없음 | 같음 |
| `edit_gentle_value` (edit:50) | %1$s 동안 | |
| `edit_mission_speed_sub` (edit:72) | 바닥까지 %1$d초 | |
| `edit_mission_examples_drop_words` (edit:91) | 예: 사과, 나무 외 6개 | 예제가 그림(`edit_mission_example_drop_words`)으로 바뀜 |
| `onboarding_skip` (settings:114) | 건너뛰기 | 첫 실행은 `first_run_skip`을 씀 |

그 밖에 키는 쓰이지만 실제로는 안 보이는 것: `escape_sentences`(시험용 문장만 씀, 12절), 잠들기 탭(Z) 문구 전체(탭이 숨겨져 있음), `tab_placeholder`(탭이 다 연결돼 있으면 안 보임).

### 15-4. 다른 키인데 글이 똑같은 것

한 글을 바꿀 때 같이 바꿀지 볼 목록이에요. 버튼처럼 짧은 공용 말('닫기', '취소', '완료', '%1$d분' 등)은 따로 두는 게 보통이라, 긴 글 위주로 보면 돼요.

| 글 | 키 |
|---|---|
| 일어나 | `app_name`, `onboarding_splash_desc` |
| 알람 | `dnd_guide_switch_label`, `first_run_perm_alarm`, `settings_card_alarm`, `tab_alarm` |
| 설정 | `settings_title`, `tab_settings` |
| 닫기 | `first_run_close`, `ring_preview_close`, `ring_release_close`, `silenced_ring_close`, `sleep_close` |
| 설정 열기 | `edit_warn_silenced_fix`, `silenced_ring_open` |
| 끄기 | `home_action_off`, `sleep_stop` |
| %1$d분 뒤 재수면 여부를 재확인할게요 | `ring_text_waiting`, `ring_wake_check_next` |
| 일어나셨죠? | `ring_wake_check_title`, `wake_check_title` |
| 아직 깨어 계시죠? | `recheck_title`, `ring_recheck_title` |
| 버튼을 10초간 눌러주세요 | `ring_hold_guide`, `ring_text_recheck` |
| 잠시 뒤 재수면 여부를 재확인할게요 | `ring_multi_next`, `ring_text_recheck_waiting` |
| 완전히 일어나셨으면 재확인을 해제해주세요 | `home_recheck_desc`, `home_recheck_desc_one`, `home_wake_check_soon_desc`, `recheck_waiting_text` |
| 곧 재수면 여부를 재확인할게요 | `home_recheck_title`, `home_wake_check_soon_band_now`, `recheck_multi_waiting_title` |
| %1$s %2$s | `home_next_subline`, `next_alarm_when` |
| %1$s. 그 밖에 %2$d개 더 | `banner_more`, `tomorrow_more` |
| %1$d분 | `edit_minutes`, `edit_ring_again_interval_option`, `sleep_minutes`, `sleep_timer_option`, `ui_duration_minutes`, `ui_until_minutes` |
| %1$d초 | `edit_seconds`, `ui_duration_seconds` |
| 한 번 | `support_freq_once`, `ui_repeat_once` |
| 없음 | `settings_feedback_none`, `ui_mission_none` |
| 내 사진 | `ui_bg_group_photo`, `ui_bg_my_photo` |
| 알람 생성 | `edit_title_new`, `home_empty_button` |
| 뒤로 | `edit_back`, `settings_back` |
| 미리 해보기 | `edit_preview`, `settings_lab_preview` |
| %1$d분, %2$d번 | `edit_ring_again_value`, `edit_snooze_value` |
| 간격 | `edit_ring_again_interval_title`, `edit_wake_check_ask` |
| 횟수 | `edit_recheck`, `edit_ring_again_count_title` |
| %1$d분 후 | `edit_minutes_after`, `edit_wake_check_value_no_recheck` |
| %1$d번 | `edit_mission_shakes_value`, `edit_mission_value_shakes`, `edit_ring_again_count_option`, `edit_ring_again_count_sheet`, `edit_times` |
| 전원 종료 방지 | `edit_power_off`, `first_run_perm_power`, `permission_power`, `settings_power_guard` |
| 해제할 때까지 | `edit_power_off_value`, `edit_ring_again_count_until_off` |
| 스마트폰을 흔들어주세요 | `edit_mission_desc_shake_shake`, `mission_shake_guide` |
| 시작하기 | `edit_mission_start`, `first_run_start` |
| 완료 | `edit_done`, `first_run_done`, `pl_done` |
| 다음 | `first_run_next`, `pl_next` |
| 건너뛰기 | `first_run_skip`, `onboarding_skip` |
| 이번만 쉬기 | `home_action_rest`, `home_rest_offer` |
| 취소 | `home_select_cancel`, `settings_bt_sheet_cancel`, `settings_lab_escape_cancel` |
| 켜기 | `banner_turn_on`, `home_action_on`, `settings_bt_sheet_on` |
| 해제 | `home_recheck_action`, `home_recheck_release` |
| 지우기 | `mission_math_delete`, `support_exit_discard` |
| 확인 | `mission_math_confirm`, `ring_wake_up_ok` |
| 잘했어요 | `mission_preview_done`, `ring_preview_done` |
| 접근성 기능이 켜져 있어 움직이는 버튼이 꺼져요 | `ring_preview_trick_off`, `settings_lab_trick_a11y` |
| 문의하기 | `settings_support`, `support_title` |
| 권한 | `faq_group_permissions`, `permission_title` |
| 알람이 울리는 동안 전원 메뉴를 막아요 | `permission_power_desc`, `settings_power_guard_desc` |
| 방해 금지 모드에서 허용하기 | `dnd_guide_title`, `settings_dnd_override` |
| 다른 앱 위에 표시 | `first_run_perm_overlay`, `permission_overlay` |
| 배터리 사용 제한 해제 | `first_run_perm_battery`, `pl_battery` |
| 종료 타이머 | `sleep_timer_chip_off`, `sleep_timer_sheet_title` |

### 15-5. 그 밖에

1. **같은 키 이름이 두 모듈에 있어요**: `sleep_play`('재생'), `sleep_pause`('일시정지')가 `core/alarm`과 `feature/sleep`에 똑같이 있어요. 글이 같아서 지금은 문제가 없지만, 한쪽만 고치면 어느 글이 나올지 빌드 순서에 따라 달라져요. 고칠 때 둘 다 고치거나 하나로 합쳐야 해요.
2. **주석 속 옛 문구**: `edit_until`, `home_next_edit` 위 주석에 옛 문구('알림이 울립니다')가 남아 있어요. 화면에는 안 나와요.
3. **영어판 준비 때 같이 볼 곳**: 12절의 요일 이름(`Locale.KOREAN`), '예: ' 떼기, 미션 콘텐츠(14절)는 문구 파일만 바꿔서는 안 바뀌어요.
