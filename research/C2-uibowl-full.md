# C2. 유아이볼 전체 조사 (알라미 전체 + 비슷한 앱)

조사일: 2026-11-15 / 출처: 유아이볼(uibowl.io) 유료 계정 / 이미지 목록: `C2-uibowl-images.json` (58장, 화면 유형 순서)

## 짧은 결론

- 알라미는 미션 13종을 **"영상 + 한 줄 설명 + 난이도 슬라이더 + 횟수 휠 + 미리보기"** 라는 같은 틀로 설정해요. 이 틀은 쓸 만해요. 우리도 미션마다 같은 설정 틀을 쓰면 돼요.
- 알라미 화면의 거의 모든 곳에 **광고 배너, 할인 배너, 프로 자물쇠, 포인트**가 붙어 있어요. 이걸 빼기만 해도 화면이 확실히 깔끔해져요. 차별점이 바로 여기 있어요.
- 우리 MVP 미션 중 **타이핑, 두더지잡기는 유아이볼에 직접 사례가 없어요**(검색 결과 0건). 대신 퀴즈 앱(페이코, 신한 SOL, 토스)과 미니게임 화면(플리더스, 크플, 두잇)에서 진행 표시, 규칙 소개, 점수 표시를 가져오면 돼요.
- 다른 알람 앱에서 가져올 것: 오르비의 **공휴일 알람 끄기**, pillow의 **길게 눌러 멈추기**, 깨비즈의 **캐릭터 말투 강도(LV1~3)**.
- 본 양: 알라미 64패턴(22페이지 전부), 다른 앱 검색 약 30그룹.

## 1. 알라미 전체 미션 목록

| 미션 | 하는 일 | 난이도 조절 | 무료/유료 | 메모 |
|---|---|---|---|---|
| 따라쓰기(미션 문장 찾기) | 문장을 그대로 입력 | 문장 40개 중 고르기, 횟수 | 일부 무료 | 우리 "움직이는 문장 타이핑"과 가장 가까움 |
| 수학문제 | 덧셈 등 계산 | 7단계(매우 쉬움~지옥 모드), 횟수 | 무료 | 난이도 이름이 재밌음 |
| 부호 맞추기 | 3?8?5=6 같은 빈칸에 부호 넣기 | 5단계, 횟수 | 프로 | |
| 색깔 타일 찾기 | 3x3 타일 중 색 맞추기 | 5단계, 라운드 수 | 무료 | 격자 터치 게임 참고 |
| 흔들기 | 폰을 N번 흔들기 | 5단계, 횟수(30번 등) | 무료 | 우리 흔들기 기본형과 같음 |
| 스쿼트 | 카메라로 스쿼트 횟수 세기 | 횟수(20번) | 프로 | |
| 걷기 | N걸음 걷기 | 걸음 수 | 프로 | |
| 우리집 물건 찾기 | 정해진 물건을 카메라로 찍기 | 물건 19개 중 고르기 | 카메라 권한 필요 | |
| 사진찍기 | 미리 찍어 둔 장소를 다시 찍기 | 없음 | 무료 | |
| QR/바코드 | 미리 등록한 바코드 스캔 | 없음 | 무료 | |
| 꽝없는 복권 받기 | 화면을 N번 탭 | 쉬움 30번 등 | 무료 | 포인트와 연결됨 |
| 오늘의 운세 | 타로 카드 뽑기 | 없음 | 무료 | 미션이라기보다 보상 |
| 키우기(펫 "라미") | 잠든 캐릭터 깨우기 | 없음 | 무료 | 아침 탭 바로가기 |

미션 밖 기능: 다시 잠들기 방지(프로, HOT 배지), 전원 끄기 방지(벌금 $1.00 셀프 벌칙), 미션 제한 시간(기본 20초), 알람 미루기(5분 3회), 부드럽게 깨우기(30초), 시간/날씨 읽어주기.

## 2. 알라미 화면 구조

| 영역 | 화면 | 구성 | 판단 |
|---|---|---|---|
| 하단 탭 5개 | 알람 / 수면 / 아침 / 리포트 / 설정 | 모든 탭 아래 광고 배너 | 탭 수는 우리가 3~4개로 줄일 것 |
| 알람 탭 | 알람 목록 | "N시간 N분 후에 울려요", 큰 시각 카드, 토글, 빨간 + 버튼 | 구조 좋음, 배너 빼기 |
| 알람 만들기 | 이름, 휠, 요일, 미션 슬롯 5칸, 다시 잠들기 방지, 고급 옵션, 저장 | 한 화면 긴 스크롤 | 구조 좋음, 자물쇠 빼기 |
| 미션 설정 | 영상, 설명, 난이도, 횟수, 미리보기 | 13종 공통 틀 | 그대로 참고 |
| 울림 | 잠금화면 큰 숫자, 밀어서 중단 | iOS 라이브 액티비티 | 안드로이드는 전체 화면 알림으로 |
| 아침 탭 | 날씨 사진, 바로가기 4개(펫, 기분, 운세, 포인트) | 가운데 큰 광고 | 피할 것 |
| 수면 탭 | 측정 시작, 코골이, 수면 리포트, 수면 사운드 | 마이크 권한 | 2차 이후 |
| 리포트 탭 | 기상/수면/습관 리포트, 달력, 빈 화면 | 지표 설명 바텀시트 | 좋음 |
| 설정 | 로그인, 포인트, 프로, 전원 끄기 방지, 테마, 사운드 출력, 앱 알림 | "모든 광고 제거하기" | 항목만 참고 |
| 온보딩 4단계 | 1 알람 시각 → 2 배경 → 3 소리와 볼륨 → 4 미션 | 진행바, 빨간 다음 버튼 | 흐름 좋음 |
| 결제 | 페이월, 학생 구독, 포인트 이용권, 복권 | 할인율, 1위 월계관 | 피할 것 |

## 3. 화면 유형별 정리

### 3-1. 알람 목록(홈)

| 레퍼런스 | 좋은 점 | 피할 점 |
|---|---|---|
| [알라미 알람 목록](https://uibowl.io/name/%EC%95%8C%EB%9D%BC%EB%AF%B8?patterns=%EB%A9%94%EC%9D%B8&patternName=%EC%95%8C%EB%9E%8C) | 맨 위 "N시간 N분 후에 울려요"가 가장 궁금한 정보를 바로 줌. 요일은 작게, 시각은 크게 | PRO 칩, 할인 배너, 광고 배너가 목록보다 먼저 보임 |
| [오르비 알람 목록](https://uibowl.io/name/%EC%98%A4%EB%A5%B4%EB%B9%84%20%EC%95%8C%EB%9E%8C?patterns=%EB%A9%94%EC%9D%B8&patternName=%EC%95%8C%EB%9E%8C%20%EB%AA%A9%EB%A1%9D) | 꺼진 알람을 흐리게 처리, 다크 배경에 노랑 토글 하나만 강조 | 특별히 없음 |
| [메디코치 알람](https://uibowl.io/name/%EB%A9%94%EB%94%94%EC%BD%94%EC%B9%98?patterns=%ED%8E%B8%EC%A7%91%C2%B7%EC%88%98%EC%A0%95%ED%95%98%EA%B8%B0&patternName=%EC%95%8C%EB%9E%8C) | 밝은 카드, 버튼 2개(편집, 추가)로 단순 | 미션 정보가 없어 우리와는 거리가 있음 |

morning-up이 할 것: 맨 위에 다음 알람까지 남은 시간, 카드에는 큰 시각 + 요일 + 미션 아이콘 + 토글. 꺼진 알람은 흐리게. 배너 자리는 비워 둬요.

### 3-2. 알람 만들기, 편집

| 레퍼런스 | 좋은 점 | 피할 점 |
|---|---|---|
| [알라미 알람 생성](https://uibowl.io/name/%EC%95%8C%EB%9D%BC%EB%AF%B8?patterns=%EC%83%9D%EC%84%B1%ED%95%98%EA%B8%B0&patternName=%EC%95%8C%EB%9E%8C) | 이름(이모지 가능), 휠, 요일 칩, 미션 슬롯 5칸, 빨간 저장 버튼 고정 | 슬롯 5칸 중 4칸이 자물쇠, 다시 잠들기 방지에도 자물쇠 |
| [알라미 알람 미루기 등 고급 옵션](https://uibowl.io/name/%EC%95%8C%EB%9D%BC%EB%AF%B8?patterns=%EB%A9%94%EC%9D%B8&patternName=%EC%95%8C%EB%9E%8C%20%EB%AF%B8%EB%A3%A8%EA%B8%B0) | 볼륨, 부드럽게 깨우기, 미루기 횟수를 아래쪽에 모음 | 깜짝 사운드에 프로 자물쇠 |
| [알라미 알람 배경 선택](https://uibowl.io/name/%EC%95%8C%EB%9D%BC%EB%AF%B8?patterns=%EC%83%9D%EC%84%B1%ED%95%98%EA%B8%B0&patternName=%EC%95%8C%EB%9E%8C%20%EB%B0%B0%EA%B2%BD%20%EC%84%A0%ED%83%9D) | 울림 화면 배경을 가로 카드로 고르기 | 밈 이미지 위주라 품질이 들쭉날쭉 |
| [오르비 새 알람 추가](https://uibowl.io/name/%EC%98%A4%EB%A5%B4%EB%B9%84%20%EC%95%8C%EB%9E%8C?patterns=%EC%83%9D%EC%84%B1%ED%95%98%EA%B8%B0&patternName=%EC%83%88%20%EC%95%8C%EB%9E%8C%20%EC%B6%94%EA%B0%80) | 제목 자리에 "23시간 41분 후에 울려요", 평일/주말 빠른 선택, **공휴일 알람 끄기** | 없음 |
| [깨비즈 알람 생성](https://uibowl.io/name/%EA%B9%A8%EB%B9%84%EC%A6%88?patterns=%EC%83%9D%EC%84%B1%ED%95%98%EA%B8%B0&patternName=%EC%95%8C%EB%9E%8C%20%EC%83%9D%EC%84%B1) | 캐릭터 말풍선, LV1~3 슬라이더로 말투 강도 조절 | 툴팁이 많아 화면이 바쁨 |
| [pillow 알람 등록](https://uibowl.io/name/pillow?patterns=%EC%8B%A0%EC%B2%AD%ED%95%98%EA%B8%B0&patternName=%EC%95%8C%EB%9E%8C%EB%93%B1%EB%A1%9D) | 카드 한 장에 시각과 "스마트 기상" 배지 | 영어 앱이라 문구는 참고 못 함 |

morning-up이 할 것: 알라미 순서(시각 → 요일 → 미션 → 다시 잠들기 방지 → 소리)를 따르되 자물쇠는 없애요. 제목에 남은 시간, 평일/주말 빠른 선택, 공휴일 끄기 토글을 넣어요. 미션 슬롯은 모두 열어 두고, 각 슬롯에서 바로 "미리 해보기"로 이어지게 해요.

### 3-3. 알람 울림, 해제

| 레퍼런스 | 좋은 점 | 피할 점 |
|---|---|---|
| [pillow 알람 울림](https://uibowl.io/name/pillow?patterns=%EC%83%81%EC%84%B8%EC%A0%95%EB%B3%B4%20%28PDP%29&patternName=%EC%95%8C%EB%9E%8C) | 검정 배경, 얇고 큰 숫자, "길게 눌러 멈추기"로 잠결 실수 방지, 주황 스누즈 알약 | 없음 |
| [깨비즈 알람 해제](https://uibowl.io/name/%EA%B9%A8%EB%B9%84%EC%A6%88?patterns=%ED%8E%B8%EC%A7%91%C2%B7%EC%88%98%EC%A0%95%ED%95%98%EA%B8%B0&patternName=%EC%95%8C%EB%9E%8C%20%ED%95%B4%EC%A0%9C) | 주황 화면과 캐릭터 잔소리로 성격이 확실함 | 반말 잔소리는 호불호가 갈림 |
| [알라미 알람 끄기](https://uibowl.io/name/%EC%95%8C%EB%9D%BC%EB%AF%B8?patterns=%EB%A9%94%EC%9D%B8&patternName=%EC%95%8C%EB%9E%8C%20%EB%81%84%EA%B8%B0) | 잠금화면에서 바로 보이는 큰 숫자 | iOS 전용 화면이라 안드로이드와 다름 |

morning-up이 할 것: 울림 화면은 큰 시각 + 미션 시작 버튼 하나. 미루기는 작게. 버튼은 "길게 눌러" 방식으로 잠결 실수를 막아요.

### 3-4. 미션 고르기, 설정

| 레퍼런스 | 좋은 점 | 피할 점 |
|---|---|---|
| [알라미 온보딩 미션 선택](https://uibowl.io/name/%EC%95%8C%EB%9D%BC%EB%AF%B8?patterns=%EC%98%A8%EB%B3%B4%EB%94%A9&patternName=%EC%95%8C%EB%9E%8C%20%EB%AF%B8%EC%85%98) | 미션마다 손에 든 폰 실사 영상 + 한 줄 설명 바텀시트 | 없음 |
| [알라미 따라쓰기 설정](https://uibowl.io/name/%EC%95%8C%EB%9D%BC%EB%AF%B8?patterns=%EA%B2%8C%EC%9D%B4%EB%AF%B8%ED%94%BC%EC%BC%80%EC%9D%B4%EC%85%98&patternName=%E1%84%86%E1%85%B5%E1%84%89%E1%85%A7%E1%86%AB%20%E1%84%86%E1%85%AE%E1%86%AB%E1%84%8C%E1%85%A1%E1%86%BC%20%E1%84%8E%E1%85%A1%E1%86%BD%E1%84%80%E1%85%B5) | 예제 미리보기, 문장 고르기, 횟수 휠, 미리보기 버튼 | 예시 문장을 그대로 쓰면 안 됨 |
| [알라미 흔들기 설정](https://uibowl.io/name/%EC%95%8C%EB%9D%BC%EB%AF%B8?patterns=%EA%B2%8C%EC%9D%B4%EB%AF%B8%ED%94%BC%EC%BC%80%EC%9D%B4%EC%85%98&patternName=%E1%84%86%E1%85%B5%E1%84%89%E1%85%A7%E1%86%AB-%E1%84%92%E1%85%B3%E1%86%AB%E1%84%83%E1%85%B3%E1%86%AF%E1%84%80%E1%85%B5) | 영상, 5단 난이도, 횟수 휠 3가지로 끝 | 없음 |
| [알라미 수학문제 설정](https://uibowl.io/name/%EC%95%8C%EB%9D%BC%EB%AF%B8?patterns=%EA%B2%8C%EC%9D%B4%EB%AF%B8%ED%94%BC%EC%BC%80%EC%9D%B4%EC%85%98&patternName=%E1%84%86%E1%85%B5%E1%84%89%E1%85%A7%E1%86%AB-%E1%84%89%E1%85%AE%E1%84%92%E1%85%A1%E1%86%A8%E1%84%86%E1%85%AE%E1%86%AB%E1%84%8C%E1%85%A6%20%E1%84%91%E1%85%AE%E1%86%AF%E1%84%80%E1%85%B5) | 난이도 이름에 "지옥 모드"까지 있어 재미 요소 | 7단계는 너무 많음 |
| [알라미 색깔 타일 설정](https://uibowl.io/name/%EC%95%8C%EB%9D%BC%EB%AF%B8?patterns=%EA%B2%8C%EC%9D%B4%EB%AF%B8%ED%94%BC%EC%BC%80%EC%9D%B4%EC%85%98&patternName=%E1%84%86%E1%85%B5%E1%84%89%E1%85%A7%E1%86%AB-%EC%83%89%EA%B9%94%20%ED%83%80%EC%9D%BC%20%EC%B0%BE%EA%B8%B0) | 3x3 격자 예제, 라운드 수 | 없음 |
| [알라미 물건 찾기 설정](https://uibowl.io/name/%EC%95%8C%EB%9D%BC%EB%AF%B8?patterns=%EA%B2%8C%EC%9D%B4%EB%AF%B8%ED%94%BC%EC%BC%80%EC%9D%B4%EC%85%98&imgId=cmmbxg374000il504gxomdwni) | "카메라 권한 필수" 배지를 미리 붙임 | 없음 |
| [알라미 스쿼트 설정](https://uibowl.io/name/%EC%95%8C%EB%9D%BC%EB%AF%B8?patterns=%EA%B2%8C%EC%9D%B4%EB%AF%B8%ED%94%BC%EC%BC%80%EC%9D%B4%EC%85%98&patternName=%EB%AF%B8%EC%85%98-%EC%8A%A4%EC%BF%BC%ED%8A%B8) | 영상 설명 | 저장 버튼 자리에 "프로 시작하기" |
| [네이버페이 미니게임 목록](https://uibowl.io/name/%EB%84%A4%EC%9D%B4%EB%B2%84%ED%8E%98%EC%9D%B4?patterns=%EA%B2%8C%EC%9D%B4%EB%AF%B8%ED%94%BC%EC%BC%80%EC%9D%B4%EC%85%98&patternName=%EB%AF%B8%EB%8B%88%EA%B2%8C%EC%9E%84) | 2열 썸네일 카드 + 한 줄 설명 | 없음 |

morning-up이 할 것: 미션 고르기는 2열 카드(그림 + 이름 + 한 줄 규칙). 카드를 누르면 공통 설정 틀(짧은 움직이는 미리보기, 난이도 3단계, 횟수, 미리 해보기). 난이도는 3단계로 줄이고 이름에 재미를 줘요.

### 3-5. 미션 진행(퀴즈, 미니게임)

| 레퍼런스 | 좋은 점 | 피할 점 |
|---|---|---|
| [페이코 퀴즈](https://uibowl.io/name/%ED%8E%98%EC%9D%B4%EC%BD%94?patterns=%EA%B2%8C%EC%9D%B4%EB%AF%B8%ED%94%BC%EC%BC%80%EC%9D%B4%EC%85%98&patternName=%ED%80%B4%EC%A6%88) | "퀴즈 1/5" 칩, 남은 시간, 큰 질문, 둥근 보기 4개 | 없음 |
| [신한 SOL 퀴즈](https://uibowl.io/name/%EC%8B%A0%ED%95%9C%20%EC%8A%88%ED%8D%BCSOL?patterns=%EA%B2%8C%EC%9D%B4%EB%AF%B8%ED%94%BC%EC%BC%80%EC%9D%B4%EC%85%98&patternName=%EC%95%BC%EA%B5%AC-%ED%80%B4%EC%A6%88) | 선택한 보기에 테두리 강조, 힌트 칩 | 포인트 문구 |
| [토스 행운퀴즈](https://uibowl.io/name/%ED%86%A0%EC%8A%A4?patterns=%EA%B2%8C%EC%9D%B4%EB%AF%B8%ED%94%BC%EC%BC%80%EC%9D%B4%EC%85%98&patternName=%ED%96%89%EC%9A%B4%ED%80%B4%EC%A6%88) | 정답/오답 결과 화면을 분명히 나눔 | 없음 |
| [제타 퀴즈](https://uibowl.io/name/%EC%A0%9C%ED%83%80?patterns=%EA%B2%8C%EC%9D%B4%EB%AF%B8%ED%94%BC%EC%BC%80%EC%9D%B4%EC%85%98&patternName=%ED%80%B4%EC%A6%88) | 큰 카드 2개, 넉넉한 터치 영역 | 2지선다라 우리와 다름 |
| [플리더스 미니게임](https://uibowl.io/name/%ED%94%8C%EB%A6%AC%EB%8D%94%EC%8A%A4?patterns=%EA%B2%8C%EC%9D%B4%EB%AF%B8%ED%94%BC%EC%BC%80%EC%9D%B4%EC%85%98&patternName=%EB%AF%B8%EB%8B%88%EA%B2%8C%EC%9E%84%ED%94%8C%EB%A0%88%EC%9D%B4) | 제목, 한 줄 규칙, 큰 그림, 시작 버튼 | 랭킹은 MVP에 불필요 |
| [크플 만보기 미니게임](https://uibowl.io/name/%ED%81%AC%ED%94%8C?patterns=%EA%B2%8C%EC%9D%B4%EB%AF%B8%ED%94%BC%EC%BC%80%EC%9D%B4%EC%85%98&patternName=%EB%A7%8C%EB%B3%B4%EA%B8%B0%20%EB%AF%B8%EB%8B%88%EA%B2%8C%EC%9E%84) | 규칙 3줄, TIP 칩, 시작 버튼 | 아침에는 글이 너무 많음 |
| [두잇 미니게임](https://uibowl.io/name/%EB%91%90%EC%9E%87%20-%20%EC%82%90%EC%95%BD%EC%9D%B4%20%ED%82%A4%EC%9A%B0%EA%B8%B0?patterns=%EB%A9%94%EC%9D%B8&patternName=%EB%AF%B8%EB%8B%88%EA%B2%8C%EC%9E%84) | 점수, 규칙 버튼, 가운데 MISSION 배지 | 없음 |
| [팔도감 미니게임](https://uibowl.io/name/%ED%8C%94%EB%8F%84%EA%B0%90?patterns=%EA%B2%8C%EC%9D%B4%EB%AF%B8%ED%94%BC%EC%BC%80%EC%9D%B4%EC%85%98&patternName=%EB%AF%B8%EB%8B%88%EA%B2%8C%EC%9E%84) | "1부터 순서대로 숫자 누르기" 같은 반응 게임 | 도전권 개수 제한 |

morning-up이 할 것: 미션 진행 화면 위쪽은 공통 머리(진행 "2/3", 남은 시간, 소리 줄임 표시). 시작 전에는 한 줄 규칙 카드만 1초 보여주고 바로 시작해요. 아침에 긴 설명은 읽지 않아요.

### 3-6. 다시 잠들기 방지, 앱 꺼짐 대응

| 레퍼런스 | 좋은 점 | 피할 점 |
|---|---|---|
| [알라미 다시 잠들기 방지](https://uibowl.io/name/%EC%95%8C%EB%9D%BC%EB%AF%B8?patterns=%EB%A9%94%EC%9D%B8&patternName=%EB%8B%A4%EC%8B%9C%20%EC%9E%A0%EB%93%A4%EA%B8%B0%20%EB%B0%A9%EC%A7%80) | "기상 인증 못 하면 다시 울려요" + "2초 / 일어났어요!" 그림 + 사용해보기 | 유료 기능(HOT 배지) |
| [알라미 앱 꺼짐 알림](https://uibowl.io/name/%EC%95%8C%EB%9D%BC%EB%AF%B8?patterns=%ED%91%B8%EC%8B%9C%EC%95%8C%EB%A6%BC) | "앱이 꺼지면 기본 알람 소리만 울려요" 하고 다시 켜게 함 | 없음 |
| [알라미 전원 끄기 방지](https://uibowl.io/name/%EC%95%8C%EB%9D%BC%EB%AF%B8?patterns=%EC%84%A4%EC%A0%95&patternName=%EC%A0%84%EC%9B%90%20%EB%81%84%EA%B8%B0%20%EB%B0%A9%EC%A7%80) | 없음 | $1.00 벌금을 카드로 걸게 함. 우리는 이 기능 자체를 뺌 |

morning-up이 할 것: 다시 잠들기 방지는 무료로 켜 두고, 설명 바텀시트와 "사용해보기"를 같은 방식으로 둬요. 앱이 꺼졌을 때 알림도 넣어요.

### 3-7. 온보딩, 권한

| 레퍼런스 | 좋은 점 | 피할 점 |
|---|---|---|
| [알라미 온보딩 1/4 알람 시각](https://uibowl.io/name/%EC%95%8C%EB%9D%BC%EB%AF%B8?patterns=%EC%95%8C%EB%A6%BC&patternName=%EC%95%8C%EB%9E%8C%20%EC%84%A4%EC%A0%95%20%ED%97%88%EC%9A%A9) | 첫 질문이 "알람을 언제 울릴까요?" | 없음 |
| [알라미 알람 권한 안내](https://uibowl.io/name/%EC%95%8C%EB%9D%BC%EB%AF%B8?patterns=%EC%98%A8%EB%B3%B4%EB%94%A9&patternName=%EC%95%8C%EB%9E%8C%20%EC%84%A4%EC%A0%95) | 시스템 권한 창 전에 이유 설명 | 없음 |
| [알라미 마이크 권한](https://uibowl.io/name/%EC%95%8C%EB%9D%BC%EB%AF%B8?patterns=%EC%84%A4%EC%A0%95&patternName=%EB%A7%88%EC%9D%B4%ED%81%AC%20%EA%B6%8C%ED%95%9C) | 권한별 체크 리스트, "다른 목적으로 쓰지 않아요" | 없음 |
| [알라미 온보딩(스플래시, 1위 홍보)](https://uibowl.io/name/%EC%95%8C%EB%9D%BC%EB%AF%B8?patterns=%EC%98%A8%EB%B3%B4%EB%94%A9) | 남색 + 그라데이션 로고 색 | "전 세계 1위, 97개국, 1억 다운로드" 자랑, 광고 추적 허용 사전 안내 |

morning-up이 할 것: 온보딩은 알람 시각 → 미션 고르기 → 미리 해보기 → 권한(알림, 정확한 알람, 전체 화면 알림, 배터리 최적화 제외) 순서. 권한마다 체크 리스트로 왜 필요한지 한 줄씩 적어요.

### 3-8. 리포트, 기록, 빈 화면

| 레퍼런스 | 좋은 점 | 피할 점 |
|---|---|---|
| [알라미 알람 기록](https://uibowl.io/name/%EC%95%8C%EB%9D%BC%EB%AF%B8?patterns=%EB%82%B4%EC%97%AD&patternName=%EC%95%8C%EB%9E%8C%20%EA%B8%B0%EB%A1%9D) | 달력 + 기상 시각 + 기상까지 걸린 시간 2칸 | 하단 광고 |
| [알라미 리포트 빈 화면](https://uibowl.io/name/%EC%95%8C%EB%9D%BC%EB%AF%B8?patterns=%ED%86%B5%EA%B3%84%C2%B7%EB%A6%AC%ED%8F%AC%ED%8A%B8&patternName=%EB%B9%88%20%ED%99%94%EB%A9%B4) | "알람 기록이 없어요" + 알람 만들기 버튼 | 없음 |
| [알라미 습관 리포트](https://uibowl.io/name/%EC%95%8C%EB%9D%BC%EB%AF%B8?patterns=%ED%86%B5%EA%B3%84%C2%B7%EB%A6%AC%ED%8F%AC%ED%8A%B8) | 요일 점 + 연속 기록 불꽃 | 없음 |
| [알라미 지표 설명](https://uibowl.io/name/%EC%95%8C%EB%9D%BC%EB%AF%B8?patterns=%EC%98%A8%EB%B3%B4%EB%94%A9&patternName=%EB%A6%AC%ED%8F%AC%ED%8A%B8) | 지표 뜻을 그림 + 한 문장 바텀시트로 | 없음 |
| [OtterLife 빈 데이터](https://uibowl.io/name/OtterLife?patterns=%ED%86%B5%EA%B3%84%C2%B7%EB%A6%AC%ED%8F%AC%ED%8A%B8&patternName=%EC%88%98%EB%A9%B4) | 점선 캐릭터 + 비어 있는 이유 + 해결책 버튼 | 없음 |
| [삼쩜삼 수면 기록](https://uibowl.io/name/%EC%82%BC%EC%A9%9C%EC%82%BC?patterns=%EA%B2%8C%EC%9D%B4%EB%AF%B8%ED%94%BC%EC%BC%80%EC%9D%B4%EC%85%98&patternName=%EC%88%98%EB%A9%B4) | "굿모닝" 인사 + 6칸 버튼으로 한 번에 기록 | 포인트 보상 |

morning-up이 할 것: 미션 끝난 직후 "기상 완료" 화면에 오늘 기상 시각, 걸린 시간, 연속 기록을 보여줘요. 기록이 없으면 알람 만들기 버튼으로 이어줘요.

### 3-9. 수면 (MVP 밖, 참고만)

| 레퍼런스 | 좋은 점 | 피할 점 |
|---|---|---|
| [알라미 수면 메인](https://uibowl.io/name/%EC%95%8C%EB%9D%BC%EB%AF%B8?patterns=%ED%86%B5%EA%B3%84%C2%B7%EB%A6%AC%ED%8F%AC%ED%8A%B8&patternName=%EC%88%98%EB%A9%B4) | "나도 모르는 내 잠버릇은?" 같은 호기심 문구 | 마이크 권한 필요 |
| [알라미 수면 측정 튜토리얼](https://uibowl.io/name/%EC%95%8C%EB%9D%BC%EB%AF%B8?patterns=%ED%8A%9C%ED%86%A0%EB%A6%AC%EC%96%BC&patternName=%EC%88%98%EB%A9%B4%20%EC%B8%A1%EC%A0%95) | 큰 시계 + 1/3 코치마크 | 없음 |
| [워크온 수면 통계](https://uibowl.io/name/%EC%9B%8C%ED%81%AC%EC%98%A8?patterns=%ED%86%B5%EA%B3%84%C2%B7%EB%A6%AC%ED%8F%AC%ED%8A%B8&patternName=%EC%88%98%EB%A9%B4) | 권장 7~9시간 띠 | 없음 |

morning-up이 할 것: MVP에는 넣지 않아요. 코치마크 방식은 "미션 미리 해보기" 첫 안내에 써요.

### 3-10. 설정

| 레퍼런스 | 좋은 점 | 피할 점 |
|---|---|---|
| [알라미 설정](https://uibowl.io/name/%EC%95%8C%EB%9D%BC%EB%AF%B8?patterns=%EB%A9%94%EB%89%B4) | 알람 고급설정, 테마, 사운드 출력, 앱 알림 묶음 | 로그인, 포인트, 프로, "모든 광고 제거하기" |
| [알라미 미션 제한 시간](https://uibowl.io/name/%EC%95%8C%EB%9D%BC%EB%AF%B8?patterns=%EC%84%A4%EC%A0%95&patternName=%EB%AF%B8%EC%85%98%20%EC%A0%9C%ED%95%9C%20%EC%8B%9C%EA%B0%84) | "설정 시간 안에 미션을 끝내야 꺼져요", 기본 20초 | 없음 |

morning-up이 할 것: 설정은 알람(미션 제한 시간, 비상 해제 시간), 화면(테마), 소리, 알림, 권한 점검 5묶음이면 충분해요.

## 4. Alarmy에서 따라 하면 안 되는 것

| 무엇 | 어디서 | 왜 |
|---|---|---|
| 하단 광고 배너 | 거의 모든 탭 | 눈이 덜 떠진 아침에 실수 터치를 부름 |
| 할인 배너, PRO 칩 | 알람 목록 맨 위 | 가장 중요한 다음 알람 정보를 밀어냄 |
| 미션 슬롯 자물쇠 | 알람 만들기 | 미션 묶기는 우리 무료 차별점 |
| 저장 자리의 "프로 시작하기" | 스쿼트, 걷기 설정 | 버튼 자리를 결제로 바꾸는 압박 |
| 리뷰 팝업 | 첫 사용 직후 홈 | 아직 아침을 한 번도 겪지 않았는데 평가를 묻는 것 |
| 아침 탭 가운데 광고 카드 | 아침 탭 | 기상 직후 첫 화면을 광고로 막음 |
| "5초 광고 보고 복권 긁기" | 포인트 | 광고 시청을 보상처럼 포장 |
| 할인율, 취소선, 1위 월계관 | 페이월 | 숫자로 압박하는 결제 화면 |
| "전 세계 1위" 자랑 온보딩 | 온보딩 | 쓰기 전부터 자랑만 함 |
| 광고 추적 허용 사전 안내 | 온보딩 | 광고 목적 권한 |
| 전원 끄기 방지 $1 벌금 | 설정 | 돈을 거는 벌칙, 우리는 기능 자체를 뺌 |
| 영어 소리 이름("Wake up you lazy") | 알람 소리 | 한국어 앱에서 품질이 떨어져 보임 |
| 따라쓰기 예시 문장 | 따라쓰기 | 문장은 직접 만들어야 함 |

## 5. 우리 MVP 미션별 참고할 화면

### 움직이는 문장 타이핑
- 유아이볼에 직접 사례 없음(타이핑 검색 0건).
- 설정: [알라미 따라쓰기](https://uibowl.io/name/%EC%95%8C%EB%9D%BC%EB%AF%B8?patterns=%EA%B2%8C%EC%9D%B4%EB%AF%B8%ED%94%BC%EC%BC%80%EC%9D%B4%EC%85%98&patternName=%E1%84%86%E1%85%B5%E1%84%89%E1%85%A7%E1%86%AB%20%E1%84%86%E1%85%AE%E1%86%AB%E1%84%8C%E1%85%A1%E1%86%BC%20%E1%84%8E%E1%85%A1%E1%86%BD%E1%84%80%E1%85%B5)의 예제 미리보기와 횟수 휠. 우리는 문장 고르기 대신 "흔들림 / 순간이동 / 글자마다 따로" 3단계 미리보기를 움직이게 보여줘요.
- 진행: [페이코 퀴즈](https://uibowl.io/name/%ED%8E%98%EC%9D%B4%EC%BD%94?patterns=%EA%B2%8C%EC%9D%B4%EB%AF%B8%ED%94%BC%EC%BC%80%EC%9D%B4%EC%85%98&patternName=%ED%80%B4%EC%A6%88)의 "1/5" 칩과 남은 시간을 머리에 두고, 문장 영역은 크게, 입력창은 키보드 바로 위에 붙여요.
- 결과: [토스 행운퀴즈](https://uibowl.io/name/%ED%86%A0%EC%8A%A4?patterns=%EA%B2%8C%EC%9D%B4%EB%AF%B8%ED%94%BC%EC%BC%80%EC%9D%B4%EC%85%98&patternName=%ED%96%89%EC%9A%B4%ED%80%B4%EC%A6%88)처럼 맞음과 틀림을 다른 색 화면으로 분명히 나눠요.

### 헷갈리는 한글 4지선다
- 가장 가까운 화면: [페이코 퀴즈](https://uibowl.io/name/%ED%8E%98%EC%9D%B4%EC%BD%94?patterns=%EA%B2%8C%EC%9D%B4%EB%AF%B8%ED%94%BC%EC%BC%80%EC%9D%B4%EC%85%98&patternName=%ED%80%B4%EC%A6%88) (진행 칩, 큰 질문, 둥근 보기 4개).
- 보기 선택 표시: [신한 SOL 퀴즈](https://uibowl.io/name/%EC%8B%A0%ED%95%9C%20%EC%8A%88%ED%8D%BCSOL?patterns=%EA%B2%8C%EC%9D%B4%EB%AF%B8%ED%94%BC%EC%BC%80%EC%9D%B4%EC%85%98&patternName=%EC%95%BC%EA%B5%AC-%ED%80%B4%EC%A6%88)의 테두리 강조.
- 터치 영역: [제타 퀴즈](https://uibowl.io/name/%EC%A0%9C%ED%83%80?patterns=%EA%B2%8C%EC%9D%B4%EB%AF%B8%ED%94%BC%EC%BC%80%EC%9D%B4%EC%85%98&patternName=%ED%80%B4%EC%A6%88)처럼 보기 하나를 크게. 보기 글자는 크게 해서 "사과/샤과"의 차이가 보여야 해요.
- 연속 3문제 규칙은 진행 칩을 점 3개로 바꿔서 보여주고, 틀리면 점이 비워지는 모습으로 "처음부터"를 알려줘요.

### 두더지잡기
- 유아이볼에 직접 사례 없음(GAME 플랫폼 검색 0건).
- 격자: [알라미 색깔 타일](https://uibowl.io/name/%EC%95%8C%EB%9D%BC%EB%AF%B8?patterns=%EA%B2%8C%EC%9D%B4%EB%AF%B8%ED%94%BC%EC%BC%80%EC%9D%B4%EC%85%98&patternName=%E1%84%86%E1%85%B5%E1%84%89%E1%85%A7%E1%86%AB-%EC%83%89%EA%B9%94%20%ED%83%80%EC%9D%BC%20%EC%B0%BE%EA%B8%B0)의 3x3. 잠결 손가락을 생각해 칸을 크게 둬요.
- 플레이 중 머리: [두잇 미니게임](https://uibowl.io/name/%EB%91%90%EC%9E%87%20-%20%EC%82%90%EC%95%BD%EC%9D%B4%20%ED%82%A4%EC%9A%B0%EA%B8%B0?patterns=%EB%A9%94%EC%9D%B8&patternName=%EB%AF%B8%EB%8B%88%EA%B2%8C%EC%9E%84)의 점수 + 목표 배지.
- 시작 카드: [플리더스](https://uibowl.io/name/%ED%94%8C%EB%A6%AC%EB%8D%94%EC%8A%A4?patterns=%EA%B2%8C%EC%9D%B4%EB%AF%B8%ED%94%BC%EC%BC%80%EC%9D%B4%EC%85%98&patternName=%EB%AF%B8%EB%8B%88%EA%B2%8C%EC%9E%84%ED%94%8C%EB%A0%88%EC%9D%B4)의 제목 + 한 줄 규칙 + 큰 그림.
- 탭 피드백: [알라미 꽝없는 복권](https://uibowl.io/name/%EC%95%8C%EB%9D%BC%EB%AF%B8?patterns=%EA%B2%8C%EC%9D%B4%EB%AF%B8%ED%94%BC%EC%BC%80%EC%9D%B4%EC%85%98&patternName=%E1%84%86%E1%85%B5%E1%84%89%E1%85%A7%E1%86%AB-%E1%84%81%E1%85%AA%E1%86%BC%E1%84%8B%E1%85%A5%E1%86%B9%E1%84%82%E1%85%B3%E1%86%AB%20%E1%84%87%E1%85%A9%E1%86%A8%E1%84%80%E1%85%AF%E1%86%AB%20%E1%84%87%E1%85%A1%E1%86%AE%E1%84%80%E1%85%B5)의 "N CLEAR" 카운트.
- 아이디어 추가: [팔도감](https://uibowl.io/name/%ED%8C%94%EB%8F%84%EA%B0%90?patterns=%EA%B2%8C%EC%9D%B4%EB%AF%B8%ED%94%BC%EC%BC%80%EC%9D%B4%EC%85%98&patternName=%EB%AF%B8%EB%8B%88%EA%B2%8C%EC%9E%84)의 "1부터 순서대로 숫자 누르기"는 다음 미션 후보로 좋아요.

### 흔들기
- [알라미 흔들기 설정](https://uibowl.io/name/%EC%95%8C%EB%9D%BC%EB%AF%B8?patterns=%EA%B2%8C%EC%9D%B4%EB%AF%B8%ED%94%BC%EC%BC%80%EC%9D%B4%EC%85%98&patternName=%E1%84%86%E1%85%B5%E1%84%89%E1%85%A7%E1%86%AB-%E1%84%92%E1%85%B3%E1%86%AB%E1%84%83%E1%85%B3%E1%86%AF%E1%84%80%E1%85%B5)와 [온보딩 흔들기 설명](https://uibowl.io/name/%EC%95%8C%EB%9D%BC%EB%AF%B8?patterns=%EC%98%A8%EB%B3%B4%EB%94%A9&patternName=%EC%95%8C%EB%9E%8C%20%EB%AF%B8%EC%85%98)이 거의 전부예요. 진행 화면은 유아이볼에 없어요.
- 우리는 남은 횟수를 큰 숫자와 원형 게이지로 보여주고, 한 번 흔들 때마다 진동과 숫자 튐을 줘요.

## 6. 링크 목록

알라미
- [미션 문장 찾기(따라쓰기)](https://uibowl.io/name/%EC%95%8C%EB%9D%BC%EB%AF%B8?patterns=%EA%B2%8C%EC%9D%B4%EB%AF%B8%ED%94%BC%EC%BC%80%EC%9D%B4%EC%85%98&patternName=%E1%84%86%E1%85%B5%E1%84%89%E1%85%A7%E1%86%AB%20%E1%84%86%E1%85%AE%E1%86%AB%E1%84%8C%E1%85%A1%E1%86%BC%20%E1%84%8E%E1%85%A1%E1%86%BD%E1%84%80%E1%85%B5)
- [스쿼트](https://uibowl.io/name/%EC%95%8C%EB%9D%BC%EB%AF%B8?patterns=%EA%B2%8C%EC%9D%B4%EB%AF%B8%ED%94%BC%EC%BC%80%EC%9D%B4%EC%85%98&patternName=%EB%AF%B8%EC%85%98-%EC%8A%A4%EC%BF%BC%ED%8A%B8)
- [걷기](https://uibowl.io/name/%EC%95%8C%EB%9D%BC%EB%AF%B8?patterns=%EA%B2%8C%EC%9D%B4%EB%AF%B8%ED%94%BC%EC%BC%80%EC%9D%B4%EC%85%98&patternName=%EB%AF%B8%EC%85%98-%EA%B1%B7%EA%B8%B0)
- [오늘의 운세](https://uibowl.io/name/%EC%95%8C%EB%9D%BC%EB%AF%B8?patterns=%EA%B2%8C%EC%9D%B4%EB%AF%B8%ED%94%BC%EC%BC%80%EC%9D%B4%EC%85%98&patternName=%E1%84%86%E1%85%B5%E1%84%89%E1%85%A7%E1%86%AB-%E1%84%8B%E1%85%A9%E1%84%82%E1%85%B3%E1%86%AF%E1%84%8B%E1%85%B4%20%E1%84%8B%E1%85%AE%E1%86%AB%E1%84%89%E1%85%A6)
- [키우기](https://uibowl.io/name/%EC%95%8C%EB%9D%BC%EB%AF%B8?patterns=%EA%B2%8C%EC%9D%B4%EB%AF%B8%ED%94%BC%EC%BC%80%EC%9D%B4%EC%85%98&patternName=%ED%82%A4%EC%9A%B0%EA%B8%B0)
- [사진찍기 미션](https://uibowl.io/name/%EC%95%8C%EB%9D%BC%EB%AF%B8?patterns=%EC%B4%AC%EC%98%81%ED%95%98%EA%B8%B0&patternName=%EB%AF%B8%EC%85%98)
- [바코드 미션](https://uibowl.io/name/%EC%95%8C%EB%9D%BC%EB%AF%B8?patterns=%EC%B4%AC%EC%98%81%ED%95%98%EA%B8%B0&patternName=%EB%AF%B8%EC%85%98-%EB%B0%94%EC%BD%94%EB%93%9C)
- [수면 측정 로딩](https://uibowl.io/name/%EC%95%8C%EB%9D%BC%EB%AF%B8?patterns=%EA%B8%B0%ED%83%80&patternName=%E1%84%85%E1%85%A9%E1%84%83%E1%85%B5%E1%86%BC-%E1%84%89%E1%85%AE%E1%84%86%E1%85%A7%E1%86%AB%20%E1%84%8E%E1%85%B3%E1%86%A8%E1%84%8C%E1%85%A5%E1%86%BC)
- [사운드](https://uibowl.io/name/%EC%95%8C%EB%9D%BC%EB%AF%B8?patterns=%EB%85%B9%EC%9D%8C%ED%95%98%EA%B8%B0&patternName=%EC%82%AC%EC%9A%B4%EB%93%9C)
- [알람 소리](https://uibowl.io/name/%EC%95%8C%EB%9D%BC%EB%AF%B8?patterns=%EC%84%A4%EC%A0%95&patternName=%EC%95%8C%EB%9E%8C%20%EC%86%8C%EB%A6%AC)
- [오늘의 한마디(문자 저장)](https://uibowl.io/name/%EC%95%8C%EB%9D%BC%EB%AF%B8?patterns=%EC%83%9D%EC%84%B1%ED%95%98%EA%B8%B0&patternName=%EB%AC%B8%EC%9E%90%20%EC%A0%80%EC%9E%A5)
- [멤버십 스크롤](https://uibowl.io/name/%EC%95%8C%EB%9D%BC%EB%AF%B8?patterns=%EB%A9%A4%EB%B2%84%EC%8B%AD%C2%B7%EC%9A%94%EA%B8%88%EC%A0%9C&patternName=%EC%8A%A4%ED%81%AC%EB%A1%A4)
- [스토어 스크린샷](https://uibowl.io/name/%EC%95%8C%EB%9D%BC%EB%AF%B8?patterns=iPhone%20%EC%8A%A4%ED%81%AC%EB%A6%B0%EC%83%B7)
- 그 밖의 알라미 링크는 위 3장과 5장 표에 있어요.

다른 앱
- 오르비 알람: [목록](https://uibowl.io/name/%EC%98%A4%EB%A5%B4%EB%B9%84%20%EC%95%8C%EB%9E%8C?patterns=%EB%A9%94%EC%9D%B8&patternName=%EC%95%8C%EB%9E%8C%20%EB%AA%A9%EB%A1%9D), [새 알람 추가](https://uibowl.io/name/%EC%98%A4%EB%A5%B4%EB%B9%84%20%EC%95%8C%EB%9E%8C?patterns=%EC%83%9D%EC%84%B1%ED%95%98%EA%B8%B0&patternName=%EC%83%88%20%EC%95%8C%EB%9E%8C%20%EC%B6%94%EA%B0%80)
- 깨비즈: [알람 생성](https://uibowl.io/name/%EA%B9%A8%EB%B9%84%EC%A6%88?patterns=%EC%83%9D%EC%84%B1%ED%95%98%EA%B8%B0&patternName=%EC%95%8C%EB%9E%8C%20%EC%83%9D%EC%84%B1), [알람 해제](https://uibowl.io/name/%EA%B9%A8%EB%B9%84%EC%A6%88?patterns=%ED%8E%B8%EC%A7%91%C2%B7%EC%88%98%EC%A0%95%ED%95%98%EA%B8%B0&patternName=%EC%95%8C%EB%9E%8C%20%ED%95%B4%EC%A0%9C)
- pillow: [알람 울림](https://uibowl.io/name/pillow?patterns=%EC%83%81%EC%84%B8%EC%A0%95%EB%B3%B4%20%28PDP%29&patternName=%EC%95%8C%EB%9E%8C), [알람 등록](https://uibowl.io/name/pillow?patterns=%EC%8B%A0%EC%B2%AD%ED%95%98%EA%B8%B0&patternName=%EC%95%8C%EB%9E%8C%EB%93%B1%EB%A1%9D)
- 메디코치: [알람](https://uibowl.io/name/%EB%A9%94%EB%94%94%EC%BD%94%EC%B9%98?patterns=%ED%8E%B8%EC%A7%91%C2%B7%EC%88%98%EC%A0%95%ED%95%98%EA%B8%B0&patternName=%EC%95%8C%EB%9E%8C)
- 퀴즈: [페이코](https://uibowl.io/name/%ED%8E%98%EC%9D%B4%EC%BD%94?patterns=%EA%B2%8C%EC%9D%B4%EB%AF%B8%ED%94%BC%EC%BC%80%EC%9D%B4%EC%85%98&patternName=%ED%80%B4%EC%A6%88), [신한 SOL](https://uibowl.io/name/%EC%8B%A0%ED%95%9C%20%EC%8A%88%ED%8D%BCSOL?patterns=%EA%B2%8C%EC%9D%B4%EB%AF%B8%ED%94%BC%EC%BC%80%EC%9D%B4%EC%85%98&patternName=%EC%95%BC%EA%B5%AC-%ED%80%B4%EC%A6%88), [토스](https://uibowl.io/name/%ED%86%A0%EC%8A%A4?patterns=%EA%B2%8C%EC%9D%B4%EB%AF%B8%ED%94%BC%EC%BC%80%EC%9D%B4%EC%85%98&patternName=%ED%96%89%EC%9A%B4%ED%80%B4%EC%A6%88), [제타](https://uibowl.io/name/%EC%A0%9C%ED%83%80?patterns=%EA%B2%8C%EC%9D%B4%EB%AF%B8%ED%94%BC%EC%BC%80%EC%9D%B4%EC%85%98&patternName=%ED%80%B4%EC%A6%88)
- 미니게임: [플리더스](https://uibowl.io/name/%ED%94%8C%EB%A6%AC%EB%8D%94%EC%8A%A4?patterns=%EA%B2%8C%EC%9D%B4%EB%AF%B8%ED%94%BC%EC%BC%80%EC%9D%B4%EC%85%98&patternName=%EB%AF%B8%EB%8B%88%EA%B2%8C%EC%9E%84%ED%94%8C%EB%A0%88%EC%9D%B4), [크플](https://uibowl.io/name/%ED%81%AC%ED%94%8C?patterns=%EA%B2%8C%EC%9D%B4%EB%AF%B8%ED%94%BC%EC%BC%80%EC%9D%B4%EC%85%98&patternName=%EB%A7%8C%EB%B3%B4%EA%B8%B0%20%EB%AF%B8%EB%8B%88%EA%B2%8C%EC%9E%84), [두잇](https://uibowl.io/name/%EB%91%90%EC%9E%87%20-%20%EC%82%90%EC%95%BD%EC%9D%B4%20%ED%82%A4%EC%9A%B0%EA%B8%B0?patterns=%EB%A9%94%EC%9D%B8&patternName=%EB%AF%B8%EB%8B%88%EA%B2%8C%EC%9E%84), [팔도감](https://uibowl.io/name/%ED%8C%94%EB%8F%84%EA%B0%90?patterns=%EA%B2%8C%EC%9D%B4%EB%AF%B8%ED%94%BC%EC%BC%80%EC%9D%B4%EC%85%98&patternName=%EB%AF%B8%EB%8B%88%EA%B2%8C%EC%9E%84), [네이버페이](https://uibowl.io/name/%EB%84%A4%EC%9D%B4%EB%B2%84%ED%8E%98%EC%9D%B4?patterns=%EA%B2%8C%EC%9D%B4%EB%AF%B8%ED%94%BC%EC%BC%80%EC%9D%B4%EC%85%98&patternName=%EB%AF%B8%EB%8B%88%EA%B2%8C%EC%9E%84)
- 기록, 수면: [삼쩜삼](https://uibowl.io/name/%EC%82%BC%EC%A9%9C%EC%82%BC?patterns=%EA%B2%8C%EC%9D%B4%EB%AF%B8%ED%94%BC%EC%BC%80%EC%9D%B4%EC%85%98&patternName=%EC%88%98%EB%A9%B4), [OtterLife](https://uibowl.io/name/OtterLife?patterns=%ED%86%B5%EA%B3%84%C2%B7%EB%A6%AC%ED%8F%AC%ED%8A%B8&patternName=%EC%88%98%EB%A9%B4), [워크온](https://uibowl.io/name/%EC%9B%8C%ED%81%AC%EC%98%A8?patterns=%ED%86%B5%EA%B3%84%C2%B7%EB%A6%AC%ED%8F%AC%ED%8A%B8&patternName=%EC%88%98%EB%A9%B4)

## 부록: 찾았지만 결과가 없거나 관련 없던 검색

| 검색 | 결과 |
|---|---|
| 타이핑(패턴) | 0건 |
| GAME 두더지잡기, 인게임 타이머 | 0건 |
| GAME 결과화면 | RPG 전투 결과만 나와 중단 |
| 챌린지 인증 | 걷기 앱 채팅방, 게시판뿐이라 중단 |
| 화면 글자 "일어났", "굿모닝" | 웹소설 랭킹, 고객센터, 주유소 지도 등 무관한 화면뿐이라 중단 |
| 화면 글자 "알람", "기상", 부품 검색(시간 선택, 토글, 빈 화면) | 시간 관계로 하지 않음(알라미, 오르비로 이미 충분) |
