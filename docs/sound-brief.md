# 벨소리, 수면음악 제작 브리핑 (ElevenLabs, 11-17)

ElevenLabs로 morning-up의 벨소리와 수면음악을 만들 때 쓰는 기준 문서예요. 코드는 바꾸지 않았어요.

## 0. 결론 (추천안)

- **벨소리는 4묶음 13곡을 새로 뽑아요**: 잔잔한 3, 경쾌한 4, 자연 3, 강하게 3. 지금 있는 5곡과 합치면 기본음 포함 19곡이에요.
- **수면음악은 앱에 이미 정해 둔 4묶음 16곡을 그대로 채워요**: 자연의 소리, 앰비언트, 피아노 연주곡, 로파이 각 4곡. 제목과 분위기가 이미 정해져 있어서 파일만 넣으면 바로 연결돼요.
- 가사는 **경쾌한, 강하게 묶음에만** 넣어요. 잔잔한, 자연, 수면음악은 모두 가사 없이(instrumental) 만들어요.
- 수면 '자연의 소리' 4곡(빗소리, 파도, 새소리, 모닥불)은 Music이 아니라 **ElevenLabs Sound Effects(효과음, 반복 켜기)** 로 뽑는 게 더 자연스러워요.

이유: 새 묶음을 만들지 않고 앱에 이미 있는 묶음 이름(알람음 S5: 기본, 잔잔한, 경쾌한, 자연 / 잠들기 Z1: 4묶음 16곡)에 맞췄어요. 다른 알람 앱도 대부분 묶음을 3~5개로 적게 두고, '시끄럽게 깨우는 소리' 묶음을 따로 둬요. 그래서 우리 앱에 없던 '강하게' 하나만 더했어요.

## 1. 다른 앱은 어떻게 나누나 (짧은 조사)

| 앱 | 벨소리 나누는 방식 | 수면 소리 나누는 방식 |
|---|---|---|
| 갤럭시 기본 알람 | 묶음 없이 한 목록 + 내 파일, 음악 앱 연결 | 없음 |
| iOS 시계 | 묶음 없이 한 목록 + '클래식' 폴더, 노래 고르기 | 없음 |
| Alarmy | 5묶음, 100곡 넘음. 'Loud(시끄러운)', 'Classic(전통 알람)'과 잔잔한 쪽(새소리, 명상, 어쿠스틱 기타 등) | 별도 수면 소리 있음 |
| Sleep Cycle | 묶음 이름 없이 '부드러운 소리와 음악' 목록 + 내 음악 | 수면 소리 목록 |
| Calm | 알람 기능은 작음 | 자연 소리(Soundscapes), 수면 음악, 수면 동화로 나눔 |

정리하면 벨소리는 **부드러운 쪽 / 신나는 쪽 / 자연 / 확실히 깨우는 쪽** 네 갈래가 흔하고, 수면은 **자연 소리 / 음악**으로 크게 나눠요. 갤럭시, iOS, Calm 내용은 앱을 써 본 기준이고, Alarmy와 Sleep Cycle은 아래 출처를 봤어요.

출처: [Alarmy 블로그, 깊이 자는 사람용 알람음](https://alar.my/en/blog/alarm-ringtones-for-heavy-sleepers), [Alarmy 블로그, 알람음 고르기](https://alar.my/en/blog/how-to-choose-alarm-sound), [Sleep Cycle 프리미엄 안내](https://support.sleepcycle.com/hc/en-us/articles/206704909), [ElevenLabs Music 프롬프트 가이드](https://elevenlabs.io/docs/overview/capabilities/music/best-practices)

## 2. 지금 앱에 있는 소리

| 묶음 | 곡 | 비고 |
|---|---|---|
| 기본 | 기본 알람음, Morning, Morning Loop | 파일 모두 30초, 192kbps mp3 |
| 잔잔한 | Love Serenade, Spring Season | |
| 경쾌한 | Good Morning | |
| 수면 | 없음 (16곡 이름, 길이, 색만 정해져 있음) | `core/model/.../SleepTracks.kt` |

겹치지 않게 하려고 새 곡은 'Morning'처럼 아침 인사 느낌의 제목과 비슷한 분위기를 피하고, 악기를 다르게 골랐어요(아래 표).

## 3. 벨소리 카테고리 (깨우기)

| 묶음 | 쓰임 | 분위기 | 템포(BPM) | 악기 | 가사 | 새로 뽑을 곡 수 |
|---|---|---|---|---|---|---|
| 잔잔한 | 일찍 일어나는 날, 옆 사람 배려 | 따뜻하고 포근함 | 70~90 | 피아노, 글로켄슈필, 어쿠스틱 기타, 첼레스타 | 없음 | 3 (기존 2 + 3 = 5) |
| 경쾌한 | 평일 출근, 기분 좋게 시작 | 밝고 통통 튐 | 110~124 | 우쿨렐레, 핑거스냅, 박수, 신스 벨, 가벼운 드럼 | 있음 (짧게) | 4 (기존 1 + 4 = 5) |
| 자연 | 부드럽게 깨우기와 짝 | 숲, 개울, 상쾌함 | 80~100 | 칼림바, 마림바, 플루트 + 새소리, 물소리 | 없음 | 3 |
| 강하게 (새 묶음) | 잘 못 일어나는 사람, 마지막 알람 | 힘차고 급함 | 128~140 | 펀치 드럼, 브라스, 신스 리드, 베이스 | 있음 (외침처럼 짧게) | 3 |

합계 13곡. 이름 짓기는 영어 제목(기존과 같은 방식)을 추천해요.

### 3-1. 잔잔한 (가사 없음)

```
Gentle morning wake-up ringtone, instrumental only, 30 seconds. Warm felt piano melody with soft glockenspiel accents, 80 BPM, major key. Starts immediately with the main melody on the first beat, no silence. Gradually builds from soft to medium volume by adding light strings. Ends on the same chord it starts with so it loops seamlessly.
```

```
Calm acoustic guitar fingerpicking ringtone, instrumental only, 30 seconds, 76 BPM. Bright celesta melody on top, cozy and sunny morning feeling. Melody begins at 0 seconds. Slow crescendo across the track. Seamless loop, ending flows back into the beginning.
```

### 3-2. 경쾌한 (가사 있음)

```
Upbeat feel-good pop ringtone, 30 seconds, 118 BPM. Ukulele, finger snaps, hand claps and a light kick drum. Cheerful female vocal, lyrics begin at 0 seconds, short catchy hook repeated. Builds energy toward the end. Ends on the downbeat so it loops seamlessly.
Lyrics:
Rise and shine, it's a brand new day
Open your eyes, let's get on our way
Rise and shine, the sun is calling you
```

```
Bright K-pop style morning ringtone, 30 seconds, 120 BPM. Synth bells, plucky bass, clean drums. Playful Korean female vocal starts right away, simple repeated chorus. Energy rises each repeat. Seamless loop.
Lyrics (Korean):
일어나요 아침이야
창문 열면 햇살 가득
오늘도 좋은 하루
일어나요 지금 바로
```

가사 줄 예시(골라 쓰기):

| 영어 | 한국어 |
|---|---|
| Good day, good day, wake up and play | 눈을 떠요 반짝반짝 |
| Coffee's on, the sky is blue | 기지개 쭉 하나 둘 셋 |
| Up, up, up, here we go | 오늘은 왠지 좋은 예감 |
| Hello morning, hello you | 굿모닝 굿모닝 일어나 |

### 3-3. 자연 (가사 없음)

```
Fresh forest morning ringtone, instrumental only, 30 seconds, 90 BPM. Kalimba melody with soft marimba, gentle birdsong and a light flowing stream in the background. Melody starts at 0 seconds. Slowly gets brighter and louder. Seamless loop.
```

```
Airy nature ringtone, instrumental only, 30 seconds, 84 BPM. Wooden flute and plucked harp over morning birds chirping. Peaceful but awake feeling. Starts immediately, gradual crescendo, seamless loop.
```

### 3-4. 강하게 (가사 있음, 짧게 외치듯)

```
High-energy wake-up alarm track, 30 seconds, 136 BPM. Punchy drums, bold brass stabs, driving synth bass, bright synth lead. Loud and urgent from the very first beat, no intro. Short shouted group chant, lyrics begin at 0 seconds. Keeps building intensity. Seamless loop.
Lyrics:
Wake up! Get up!
No more sleeping, time to go
Wake up! Get up!
```

```
Energetic electronic alarm, 30 seconds, 130 BPM. Hard-hitting beat, rising synth arpeggio, siren-like lead melody (musical, not harsh). Korean male group chant starts immediately. Intensity rises throughout. Seamless loop.
Lyrics (Korean):
일어나 일어나
지금 당장 일어나
늦었어 뛰어 뛰어
```

## 4. 수면음악 카테고리 (잠들기)

앱 Z1에 이미 정해 둔 4묶음 16곡을 그대로 채워요. 모두 가사 없음이에요.

| 묶음 | 쓰임 | 분위기 | 템포(BPM) | 악기, 소리 | 가사 | 곡 수 | 만드는 곳 |
|---|---|---|---|---|---|---|---|
| 자연의 소리 | 생각 끄기, 소음 가리기 | 고르고 끊김 없음 | 박자 없음 | 비, 파도, 새, 모닥불 | 없음 | 4 | Sound Effects (반복 켜기) |
| 앰비언트 | 깊게 쉬기 | 넓고 떠 있는 느낌 | 50~60 (박자 거의 없음) | 신스 패드, 드론, 부드러운 벨 | 없음 | 4 | Music |
| 피아노 연주곡 | 감성, 하루 정리 | 조용하고 여림 | 55~70 | 펠트 피아노, 낮은 현악 | 없음 | 4 | Music |
| 로파이 | 뒤척이는 밤 | 따뜻하고 나른함 | 60~75 | 로즈 피아노, 테이프 잡음, 부드러운 드럼 | 없음 | 4 | Music |

합계 16곡.

### 4-1. 곡별 프롬프트

공통 꼬리말(Music 프롬프트 끝에 붙여요):

```
Instrumental only, 30 seconds. No sudden loud sounds, no drum fills, very soft dynamics, constant volume. Seamless loop: the ending flows smoothly back into the beginning.
```

**자연의 소리 (Sound Effects, Loop 켜고 30초)**

| 곡 (id) | 프롬프트 |
|---|---|
| 창문을 두드리는 밤비 (night_rain) | `Steady gentle night rain tapping on a window glass, soft and even, distant light rain on the roof, no thunder` |
| 멀리서 밀려오는 파도 (waves) | `Slow distant ocean waves rolling onto a sandy beach at night, calm and even, no seagulls, no wind gusts` |
| 새벽 숲의 새소리 (forest_birds) | `Quiet forest before dawn, a few soft distant birds, light breeze through leaves, very calm, no loud calls` |
| 타닥이는 모닥불 (campfire) | `Soft crackling campfire, gentle wood pops, quiet night crickets far away, warm and steady` |

**앰비언트 (Music)**

| 곡 (id) | 프롬프트 앞부분 |
|---|---|
| 깊은 밤, 고요한 숨 (deep_night) | `Deep ambient sleep music, 54 BPM, warm low synth drone with slow evolving pads, very dark and peaceful.` |
| 구름 위를 천천히 걷기 (cloud_walk) | `Dreamy airy ambient, 58 BPM, soft shimmering pads and faint music box notes, floating weightless feeling.` |
| 느리게 쉬는 숨 (slow_breath) | `Breathing ambient for sleep, 50 BPM, pads that swell in for 4 seconds and fade out for 6 seconds, like slow breathing.` |
| 별빛 아래 들판 (starry_field) | `Night sky ambient, 56 BPM, wide soft pads with tiny twinkling bell sparkles, calm open field feeling.` |

**피아노 연주곡 (Music)**

| 곡 (id) | 프롬프트 앞부분 |
|---|---|
| 고요한 방 (quiet_room) | `Solo felt piano lullaby, 60 BPM, simple slow melody, lots of space between notes, intimate quiet room sound.` |
| 달빛 산책 (moon_walk) | `Soft piano with low cello, 64 BPM, gentle walking melody, moonlit night mood, minor to major warmth.` |
| 새벽 왈츠 (dawn_waltz) | `Slow gentle piano waltz in 3/4, 66 BPM, tender and nostalgic, very soft touch.` |
| 빗방울 소나타 (rain_sonata) | `Soft piano with light rain ambience in the background, 62 BPM, delicate repeating notes like raindrops.` |

**로파이 (Music)**

| 곡 (id) | 프롬프트 앞부분 |
|---|---|
| 새벽 2시 (two_am) | `Lo-fi sleep beat, 62 BPM, mellow Rhodes chords, very soft brushed drums, warm tape hiss, late night mood.` |
| 골목 카페 (alley_cafe) | `Lo-fi jazz hop, 70 BPM, muted jazz guitar, soft upright bass, quiet cafe ambience, cozy and sleepy.` |
| 밤 버스 (night_bus) | `Lo-fi chill, 66 BPM, soft synth keys, gentle low hum like a night bus ride, light vinyl crackle, very soft kick.` |
| 느린 오후 (slow_afternoon) | `Lo-fi acoustic, 68 BPM, soft nylon guitar and mellow keys, lazy warm afternoon feeling, gentle tape wobble.` |

참고: 앱 목록은 곡 길이를 20~60분으로 보여 줘요. 30초 파일은 앱이 반복 재생해서 그 길이를 채워요. 피아노와 로파이는 30초 반복이 귀에 띌 수 있으니, 크레딧이 남으면 이 두 묶음만 60~120초로 다시 뽑는 것도 좋아요.

## 5. 제작 팁

### 벨소리

| 팁 | 프롬프트에 쓰는 말 |
|---|---|
| 첫 1초 안에 소리가 나야 해요. 앞에 조용한 구간이 있으면 '안 울린다'고 느껴요 | `starts immediately`, `no intro`, `melody begins at 0 seconds` |
| 점점 커지게 만들어요. 앱의 '부드럽게 깨우기'와도 잘 맞아요 | `gradually builds`, `slow crescendo` |
| 끝과 시작이 이어져야 해요. 알람은 30초를 계속 반복해요 | `seamless loop`, `ends on the same chord it starts with` |
| 끝에 페이드아웃(서서히 작아짐)을 넣지 않아요. 반복될 때 소리가 끊긴 것처럼 들려요 | `no fade out` |
| 가사는 2~4줄, 같은 구절 반복이 좋아요. 30초에 긴 가사는 잘려요 | 가사를 직접 넣고 `short catchy hook repeated` |
| 폰 스피커에서 들리게 중고음이 또렷한 악기를 써요. 낮은 베이스만 있으면 작게 들려요 | `bright`, `clear melody` |
| 뽑은 뒤 폰 스피커로 꼭 들어 보고, 첫 1초와 이음새(30초→0초)를 확인해요 | |

### 수면음악

| 팁 | 프롬프트에 쓰는 말 |
|---|---|
| 갑자기 커지는 소리, 드럼 필인, 큰 새소리, 천둥은 빼요 | `no sudden loud sounds`, `no thunder` |
| 템포는 50~70으로 느리게, 음량은 처음부터 끝까지 고르게 | `constant volume`, `very soft dynamics` |
| 가사와 목소리는 넣지 않아요. 말소리는 머리를 깨워요 | `instrumental only` |
| 끝과 시작이 이어지게. 수면은 수십 분 반복돼서 이음새가 더 잘 들려요 | `seamless loop` |
| 너무 높은 음, 날카로운 소리는 피해요 | `warm`, `soft`, `low` |
| 같은 프롬프트로 2~3번 뽑아서 가장 고른 것을 골라요 | |

## 6. 파일 넘길 때

- 형식: mp3, 30초 (지금 벨소리와 같게).
- 파일 이름: 벨소리는 영어 제목(`Sunny Steps.mp3`처럼), 수면은 위 표의 id(`night_rain.mp3`)로 두면 개발 세션이 바로 연결할 수 있어요.
- 둘 곳: 벨소리는 `sounds/ringtones/`, 수면은 `sounds/sleep/`.
- 알람음 화면(S5) 묶음은 지금 코드에 기본, 잔잔한, 경쾌한만 있어요. 자연, 강하게 묶음을 더하는 건 파일을 넣을 때 개발 세션이 함께 해요. 피그마 S5 탭(기본, 자연, 잔잔한, 경쾌한, 클래식, 동요, 내 파일) 중 클래식, 동요는 이번에 곡이 없어서 비어 있으면 앱이 자동으로 숨겨요. '강하게' 탭 이름은 사용자 확인 후 정해요.
