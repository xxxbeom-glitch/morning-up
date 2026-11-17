# 알람음 파일 목록

원본은 `sounds/ringtones/`에 그대로 두고, 앱에서 읽을 수 있게 영어 소문자와 밑줄 이름으로 바꿔 `core/alarm/src/main/res/raw/`에 복사했어요(2026-11-17).

| 원래 이름 (`sounds/ringtones/`) | 바꾼 이름 (`res/raw/`) | 앱 안 id | S5 묶음 | 화면 이름 |
|---|---|---|---|---|
| Morning.mp3 | ringtone_morning.mp3 | `morning` | 기본 | Morning |
| Morning_Loop_2026-11-17T031033.mp3 | ringtone_morning_loop.mp3 | `morning_loop` | 기본 | Morning Loop |
| Love Serenade.mp3 | ringtone_love_serenade.mp3 | `love_serenade` | 잔잔한 | Love Serenade |
| Spring Season.mp3 | ringtone_spring_season.mp3 | `spring_season` | 잔잔한 | Spring Season |
| Good Morning.mp3 | ringtone_good_morning.mp3 | `good_morning` | 경쾌한 | Good Morning |
| (원래 있던 기본음) | alarm_default.wav | `default` | 기본 | 기본 알람음 |

## 소리를 더하거나 바꾸는 법
1. 파일을 `core/alarm/src/main/res/raw/`에 `ringtone_이름.mp3`(영어 소문자, 숫자, 밑줄만)로 넣어요.
2. `core/model/.../Ringtones.kt`에 묶음과 화면 이름을 한 줄 더해요.
3. `core/alarm/.../ring/RingtoneFiles.kt`에 id와 파일을 한 줄 더해요. 빠뜨리면 테스트(`RingtoneFilesTest`)가 알려 줘요.

묶음 나누기(기본, 잔잔한, 경쾌한)는 곡 이름만 보고 임시로 정했어요. 자연, 클래식, 동요, 내 파일 묶음은 소리가 생기면 보여요.
