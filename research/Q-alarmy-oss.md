# Q. 알라미가 쓰는 오픈소스 중 우리가 쓸 만한 것 (TSK-272)

- 조사: [리서치 6], 2026-11-27. 알라미 26.43.0.
- 방법: 폰에 깔린 알라미 설치 파일을 PC로 복사해, 앱 설정 '정보 > 오픈소스' 화면이 읽는 목록 파일(`res/raw/third_party_license_metadata`, 구글 오픈소스 고지 플러그인 형식)을 그대로 읽었어요. 앱을 실행하거나 고치지 않았고, 폰 설정도 바꾸지 않았어요. 설치 파일 복사본은 문서 밖 임시 폴더에만 있어요.
- 목록은 **680줄**이에요(같은 이름이 여러 번 나오는 묶음 포함). 라이선스는 같은 파일의 본문에서 확인했어요.

## 1. 결론 먼저

| 판단 | 내용 |
|---|---|
| 알라미 목록의 대부분 | 우리가 이미 쓰는 것(AndroidX, Compose, Hilt, Room, DataStore, 코루틴, Timber)이거나 광고, 분석 SDK(약 40%)예요. 광고, 분석은 우리 앱 방향(무료 기본 + 유료 기능, 광고 계획 없음)과 맞지 않아 뺐어요 |
| 바로 쓸 만한 것 | **Lottie**(움직이는 그림), **Accompanist Permissions** 대신 지금 방식 유지, **Play Billing**(구독), **Play 인앱 리뷰**, **Play 오픈소스 고지 플러그인** |
| 나중 후보 | **Media3 ExoPlayer**(벨소리 재생을 바꿀 때), **CameraX + ML Kit 바코드/사물 인식**(사진, 바코드 미션을 만들 때), **Firebase Crashlytics, Messaging**(출시 뒤 오류 수집, 문의 답변 알림) |
| 쓰지 않을 것 | 광고 SDK 전부, Zendesk(유료 고객센터, TSK-267 문서 참고), Braze, Amplitude, Datadog, AppMetrica(분석, 마케팅) |

## 2. 알라미 목록을 쓰임새로 나눈 표

| 묶음 | 알라미가 쓰는 것(대표) | 라이선스 | 우리 상태 |
|---|---|---|---|
| 화면 기본 | AndroidX(Activity, Core, Lifecycle, Navigation, Fragment), Compose(UI, Foundation, Material3, Animation), ConstraintLayout | Apache 2.0 | 이미 씀(Compose 위주) |
| 저장 | Room, DataStore(Preferences), SQLite | Apache 2.0 | 이미 씀 |
| 의존성 주입 | Hilt, Dagger, javax.inject | Apache 2.0 | 이미 씀 |
| 비동기 | Kotlin 코루틴, kotlinx-serialization, kotlinx-datetime | Apache 2.0 | 코루틴, serialization 씀. datetime은 java.time으로 충분 |
| 로그 | Timber | Apache 2.0 | 이미 씀 |
| 백그라운드 일 | WorkManager(+ Hilt 연결) | Apache 2.0 | 안 씀. 알람은 AlarmManager가 맞아요. 로그 올리기 재시도 같은 '언젠가 하면 되는 일'에만 후보 |
| 움직이는 그림 | **Lottie, Lottie Compose** | Apache 2.0 | 안 씀. 아래 3절 |
| 소리, 영상 | Media3(ExoPlayer, UI), 옛 ExoPlayer, mp4parser | Apache 2.0 | 안 씀(MediaPlayer). 아래 3절 |
| 카메라, 인식 | CameraX(Core, Lifecycle, View, Video, ML Kit Vision), ML Kit 바코드 인식, 이미지 라벨링, TensorFlow Lite(LiteRT), MediaPipe | CameraX는 Apache 2.0. **ML Kit은 오픈소스가 아니라 구글 ML Kit 이용약관** | 안 씀. 카메라 미션 보류 중 |
| 이미지 | Glide(BSD), Coil(Apache 2.0), Picasso, AndroidSVG | 관대한 라이선스 | 안 씀. 앱 안 그림만 쓰면 필요 없음 |
| 네트워크 | OkHttp, Retrofit, Gson, Ktor, Volley, Cronet | Apache 2.0 | 안 씀(날씨는 HttpURLConnection 한 번). 서버 붙일 때 후보 |
| 구독, 스토어 | **Play Billing(billing, billing-ktx)**, Play Core(인앱 리뷰 review, 기능 배포), Install Referrer | **Android SDK 이용약관**(오픈소스 아님, 무료) | 안 씀. 아래 3절 |
| 로그인 | play-services-auth, Firebase | Android SDK 약관 / Apache 2.0 | 안 씀. 결정: 로그인 없이 구독(TSK-263 문서) |
| 오류, 알림 | Firebase Crashlytics, Messaging(FCM), Remote Config, Installations | Apache 2.0(SDK) | 안 씀. 출시 뒤 후보 |
| 위치 | play-services-location | Android SDK 약관 | 안 씀(LocationManager로 한 번만 가져옴). 그대로 둠 |
| 고객센터 | **Zendesk**(Messaging, Support, Guide SDK) | **Zendesk 유료 계약 조건**(오픈소스 아님) | 안 씀. TSK-267 문서 |
| 분석, 마케팅 | Amplitude(MIT), Braze, Datadog RUM, AppMetrica, Snowplow, Airbridge | 섞임 | 안 씀(개인정보 부담, 방향과 다름) |
| 광고 | AdMob, AppLovin, Unity, Vungle, Mintegral, Moloco, Pangle, InMobi, Yandex, BigoAds, Chartboost, Fyber, IronSource, Meta, 동의창 Didomi, AdShield 등 20여 개 | 각 회사 약관 | 안 씀 |
| 기타 작은 것 | Balloon(말풍선 툴팁), Sandwich(네트워크 응답 감싸기), Chucker(개발용 네트워크 보기), Kronos(인터넷 시간 맞추기), Ticker(숫자 굴러가는 글자), Snapper(목록 스냅), Accompanist(Permissions, Pager), Orbit MVI, DivKit, Zipline, Adhan(기도 시각 계산, MIT) | 대부분 Apache 2.0 | 아래 3절에서 몇 개만 후보 |

## 3. 우리가 쓸 후보 (추천 순)

| 순서 | 이름 | 용도 | 라이선스 | 우리 쓸모 | 언제 |
|---|---|---|---|---|---|
| 1 | **Play 오픈소스 고지 플러그인**(`com.google.android.gms:oss-licenses-plugin` + `play-services-oss-licenses`) | 빌드 때 우리가 쓰는 라이브러리의 라이선스 목록을 자동으로 모아 화면으로 보여 줌 | Apache 2.0 | 설정에 '오픈소스 라이선스' 줄이 이미 있어요. 손으로 적으면 빠지기 쉬워요. 알라미도 이 방식이에요. Compose 화면이 필요하면 **AboutLibraries**(Apache 2.0, 알라미 목록엔 없음)도 같은 일을 해요 | 출시 전(TSK-157 앞) |
| 2 | **Play Billing Library**(billing-ktx) | 구글 플레이 구독, 결제, 구독 상태 확인 | Android SDK 약관 | 유료 기능(Crazy Mode, 움직이는 버튼, 좀비모드, 결정 188) 잠금 해제. 로그인 없이 구글 계정으로 구독 확인(TSK-263 문서 5절) | 구독 화면(TSK-235) 할 때 |
| 3 | **Play 인앱 리뷰**(`com.google.android.play:review-ktx`) | 앱을 나가지 않고 별점 창 띄우기 | Android SDK 약관 | 알라미는 미션 끝, 기록 쌓인 뒤 평가를 물어요(P 문서 2절 '평가 묻기'). 우리는 '알람 7번 잘 껐을 때'처럼 좋은 순간 한 번만 | 출시 직후 |
| 4 | **Lottie Compose** | 디자이너가 After Effects로 만든 움직임을 그대로 재생 | Apache 2.0 | 온보딩, 권한 안내 움직임(OnboardingMotion.kt 등)을 지금은 코드로 하나하나 짜요. 디자이너(사용자)가 만든 움직임을 파일로 바로 넣을 수 있어요. 앱 크기 약 0.3MB 늘어요 | 새 움직임을 만들 때 시험 |
| 5 | **Media3 ExoPlayer** | 소리 재생(반복 이음새 없음, 서서히 키우기 쉬움) | Apache 2.0 | 지금 MediaPlayer로 충분해요. 30초 루프 벨소리의 이음새가 끊겨 들리거나, 벨소리를 내려받아 쓰게 되면 바꿔요. 앱 크기 1~2MB 늘어요 | 문제 생기면 |
| 6 | **Firebase Crashlytics** | 앱이 죽은 기록을 모아 보기 | SDK Apache 2.0, 서비스 무료 | 비공개 테스트 중 '알람이 안 울렸다'의 원인이 앱 죽음인지 알 수 있어요. 공용 Firebase 프로젝트 규칙(README) 먼저. 개인정보 처리방침에 적어야 해요 | 비공개 테스트 중 검토 |
| 7 | **CameraX + ML Kit 바코드 인식** | 사진, 바코드 찍기 미션 | CameraX Apache 2.0, ML Kit 구글 약관(무료, 기기 안 처리) | 알라미 대표 미션이에요. 미션 아이디어(보류)와 같이 | 보류(결정 193) |
| 8 | **Kronos** | 인터넷 시간으로 폰 시계 오차 확인 | Apache 2.0 | 폰 시계가 틀어졌을 때 알려 주기. 효과에 비해 인터넷 권한 쓰임이 늘어 **안 쓰는 쪽 추천** | 안 함 |

- 쓰지 않을 것: Balloon(우리 디자인 시스템 시트, 토스트로 충분), Accompanist Permissions(구글이 더 이상 키우지 않음, 지금 권한 화면이 이미 있음), Glide, Coil(사진 불러오기가 없음).

## 4. 라이선스 지키는 법 (가져다 쓸 때)

| 라이선스 | 해야 할 일 |
|---|---|
| Apache 2.0, MIT, BSD | 앱 안 '오픈소스 라이선스' 화면에 이름과 라이선스 문구를 남기면 돼요. 1번 플러그인이 자동으로 해요 |
| Android SDK 약관(Play Billing, Play Core, play-services) | 오픈소스 고지는 의무가 아니지만, 플러그인이 같이 목록에 넣어 줘요. 약관을 따르면 무료 |
| ML Kit 약관 | 구글 약관 동의. 개인정보 처리방침에 '기기 안에서 처리' 적기 |
| GPL(알라미 목록엔 없음) | 섞으면 우리 소스 공개 의무. 계속 B 문서 원칙대로 보기만 해요 |

## 5. 확인 못 한 것
- 각 라이브러리의 버전 번호는 이 목록 파일에 없어서 적지 않았어요.
- 알라미가 이 라이브러리를 화면 어디에 쓰는지는 이름으로 짐작한 것이에요(예: Lottie는 움직이는 그림, Balloon은 툴팁).
