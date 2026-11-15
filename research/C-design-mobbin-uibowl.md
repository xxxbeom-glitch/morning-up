# C. 디자인 레퍼런스 조사 (Mobbin, 유아이볼)

- 작성일: 2026-11-15
- 범위: 알람, 기상, 수면, 습관 앱의 화면 레퍼런스
- 출처: Mobbin MCP(iOS 화면만 검색됨), 유아이볼 MCP(무료 등급이라 검색당 3건까지)
- 이미지는 링크로만 남겼어요. 우리 시안에 이미지를 붙이거나 따라 그리지 않아요.

> **법적 주의**: 알라미(Alarmy)의 미션 이름, 아이콘, 레이아웃, 예시 문장은 가져오지 않아요. 알라미는 "무엇을 피할지" 확인하는 용도로만 봐요.

---

## 1. 결론 요약: 공통 패턴 5개

| # | 패턴 | 어디서 봤나 | 우리에게 주는 뜻 |
|---|---|---|---|
| 1 | **시간이 화면의 주인공**이에요. 알람 목록, 설정, 울림 화면 모두 숫자가 가장 크고, 나머지는 작게 물러나 있어요. | Me+, Life Reset, TIDE, Fitbit, pillow | 시간 숫자 크기와 굵기를 디자인 시스템의 첫 토큰으로 정해요. |
| 2 | **반복 요일은 동그란 칩 7개 한 줄**이 사실상 표준이에요. 켜진 날만 채운 색, 꺼진 날은 테두리나 어두운 색이에요. | Life Reset, Vocabulary, Lifesum | 새로 발명하지 말고 표준을 따르되, 색과 모양은 우리 것으로 정해요. |
| 3 | **울림 화면은 버튼 2개**(다시 울림 / 끄기)만 남기고, 끄기를 더 어렵게(길게 누르기 등) 또는 더 아래에 둬요. | TIDE, pillow(유아이볼), WHOOP | 우리는 "끄기" 대신 "미션 시작"을 주 버튼으로 두고, 다시 울림은 작게 둬요. |
| 4 | **권한은 시스템 팝업 전에 우리 화면으로 먼저 이유를 설명**해요(프리 프롬프트). 알림 예시 카드를 보여주면 이해가 빨라요. | Brilliant, ChatGPT, Cal AI, 패스오더, 도미노 | 알람 앱은 권한이 많아서(알림, 정확한 알람, 배터리 최적화 제외, 다른 앱 위에 표시) 권한마다 한 화면씩 이유를 보여줘요. |
| 5 | **해제 뒤에는 짧은 축하 + 연속 기록(스트릭)**을 보여줘요. 큰 숫자, 이번 주 요일 체크, 버튼 하나. | Brilliant, BitePal, 5 Minute Journal, Flo, 삼쩜삼 | 기상 확인 화면에 "연속 n일"과 이번 주 7칸 체크를 넣고, 버튼은 하나만 둬요. |

---

## 2. 화면 유형별 레퍼런스

### 2-1. 알람 목록

| 앱 | 링크 | 좋은 점 | 피할 점 |
|---|---|---|---|
| WHOOP | [Mobbin](https://mobbin.com/screens/7af269fb-2a3b-40f9-8730-8ae018912a96) | 요일(작게) → 시간(크게) → 알람 종류 배지 순서가 명확해요. 펼치면 편집, 삭제가 나와서 목록이 깔끔해요. | 대문자 작은 글씨가 많아 한글로 바꾸면 위계가 약해져요. 펼침 방식은 알람이 많을 때 느려요. |
| Me+ | [Mobbin](https://mobbin.com/screens/271bcaf6-57d2-4d0d-9233-51731dd4004f) | 카드 하나에 이름, 시간, 반복, 토글이 다 들어가요. 제목 자리에 "예정된 알람 없음" 같은 상태 문구를 써요. | 꺼진 알람인데 카드 색이 진해서 켜짐과 꺼짐 구분이 약해요. |
| 메디코치 (유아이볼) | [유아이볼](https://uibowl.io/name/%EB%A9%94%EB%94%94%EC%BD%94%EC%B9%98?patterns=%ED%8E%B8%EC%A7%91%C2%B7%EC%88%98%EC%A0%95%ED%95%98%EA%B8%B0&patternName=%EC%95%8C%EB%9E%8C) | 시간 + 토글만 있는 아주 단순한 한 줄 목록이에요. 아래에 "편집 / 새 알람 추가" 버튼 2개가 고정돼 있어요. | 반복 요일, 미션 정보가 목록에 안 보여서 우리 앱에는 정보가 부족해요. |

### 2-2. 시간 선택

| 앱 | 링크 | 좋은 점 | 피할 점 |
|---|---|---|---|
| WHOOP | [Mobbin](https://mobbin.com/screens/9bf17146-28cb-4b00-ad26-3e9f55c5b4cc) | 바텀시트 안 휠 피커 아래에 "8:30~9:30 사이에 진동" 같은 결과 설명을 한 줄로 보여줘요. | 어두운 배경에 회색 글씨가 많아 새벽에도 대비가 낮아요. |
| Life Reset | [Mobbin](https://mobbin.com/screens/1c4db405-f9f5-4f93-af23-0f81877c66ce) | 큰 숫자 + 위아래 화살표, 오전/오후는 옆에 따로 선택해요. 시간, 반복, 소리, 진동, 점점 커지는 소리를 한 화면에 다 둬요. | 소리 이름에 거친 말을 써요. 우리 소리 이름은 차분하게 지어요. |
| Google Health (Fitbit) | [Mobbin](https://mobbin.com/screens/52413952-4cbd-473c-8dab-13cba2171bd5) | 질문형 제목 + 큰 시간 + "−/+" 버튼이에요. 온보딩 첫 알람 만들기에 딱 맞는 단순함이에요. | 1분 단위로 누르면 오래 걸려요. 길게 누르기나 5분 단위를 같이 둬야 해요. |
| Joi | [Mobbin](https://mobbin.com/screens/fd0d0f1e-7246-45f3-8ab7-139186ed987a) | "기상 / 잠들 준비" 두 줄의 작은 시간 칩을 누르면 휠이 떠요. 온보딩에서 가볍게 보여요. | 휠이 제목 위를 가려서 무엇을 고르는지 잠깐 헷갈려요. |
| 깨비즈 (유아이볼) | [유아이볼](https://uibowl.io/name/%EA%B9%A8%EB%B9%84%EC%A6%88?patterns=%EC%83%9D%EC%84%B1%ED%95%98%EA%B8%B0&patternName=%EC%95%8C%EB%9E%8C%20%EC%83%9D%EC%84%B1) | 한국 기상 앱이에요. 캐릭터 + 단계 슬라이더(LV1~3)로 알림 강도를 고르고, 말풍선으로 설명해요. | 말풍선이 피커를 가려요. 설명은 피커 밖에 둬요. |

### 2-3. 반복 요일

| 앱 | 링크 | 좋은 점 | 피할 점 |
|---|---|---|---|
| Life Reset | [Mobbin](https://mobbin.com/screens/1c4db405-f9f5-4f93-af23-0f81877c66ce) | 동그란 칩 7개, 켠 날은 주황 채움, 끈 날은 어두운 회색이라 한눈에 보여요. | 주말 칩도 같은 모양이라 "평일만" 같은 빠른 선택이 없어요. |
| Vocabulary | [Mobbin](https://mobbin.com/screens/39552df0-5f7f-460d-97b0-94f4cd7c5d94) | 알람, 시간, 반복, 소리를 한 카드에 묶은 설정 그룹이 깔끔해요. | 모든 칩이 같은 색이라 켜짐과 꺼짐이 구분되지 않아요. |
| Lifesum | [Mobbin](https://mobbin.com/screens/407d6bc8-47d6-46c2-9a7f-4634fb5ea43e) | 선택된 날만 초록 채움, 나머지는 테두리라 구분이 분명해요. | 요일 머리글자(S, T)가 겹쳐서 영어로는 헷갈려요. 한글 "일 월 화…"는 이 문제가 없어요. |
| WHOOP | [Mobbin](https://mobbin.com/screens/7af269fb-2a3b-40f9-8730-8ae018912a96) | 목록에서 요일을 "MON · TUE · …" 글자로 보여줘서 칩 없이도 읽혀요. | 가운뎃점 나열은 길어져요. 목록에서는 "평일", "주말", "매일"처럼 묶어 써요. |

### 2-4. 알람 울림 화면

| 앱 | 링크 | 좋은 점 | 피할 점 |
|---|---|---|---|
| TIDE | [Mobbin](https://mobbin.com/screens/951a907e-fbcd-4ba4-8e7f-c2ccec28ba5f) | 새벽 하늘 같은 그라데이션, 큰 시간, 버튼 2개(다시 울림 / 일어나기)만 있어서 잠결에도 읽혀요. | 두 버튼 크기와 무게가 비슷해서 실수로 끄기 쉬워요. |
| pillow (유아이볼) | [유아이볼](https://uibowl.io/name/pillow?patterns=%EC%83%81%EC%84%B8%EC%A0%95%EB%B3%B4%20%28PDP%29&patternName=%EC%95%8C%EB%9E%8C) | 검정 배경, 아주 큰 가는 숫자, "다시 울림"은 위에 큰 알약 버튼, "멈추기"는 아래 작은 원 버튼 + "길게 눌러 멈추기" 안내예요. 실수 방지가 잘 돼요. | 가는 숫자는 저시력 사용자에게 약해요. 우리는 조금 더 굵게 해요. |
| WHOOP | [Mobbin](https://mobbin.com/screens/a3b79a84-bd3d-4788-836c-6d6385aeeb98) | 끄는 중 상태(원형 진행 + "기기를 가까이 두세요")를 따로 보여줘서 기다리는 이유를 알려줘요. | 뒤 화면이 비쳐서 정보가 겹쳐 보여요. |

### 2-5. 미션 화면 (깨우기 과제)

| 앱 | 링크 | 좋은 점 | 피할 점 |
|---|---|---|---|
| Opal (집중 앱의 잠금 해제 문제) | [Mobbin](https://mobbin.com/screens/3719ca66-9d12-44c0-a83e-041a342061ab) | 곱셈 3문제를 한 카드에, 숫자 키패드를 바로 아래에 둬요. 지금 칸만 테두리로 강조해요. "그만두기"는 아주 작게 맨 아래에 있어요. | 그만두기가 너무 숨겨져 있어요. 알람에서는 비상 해제 경로를 분명히 알려야 해요(접근성, 신뢰). |
| Duolingo | [Mobbin](https://mobbin.com/screens/93d23da8-8b8c-45a7-b01b-fcefd7a1074b) | 위에 진행 개수와 남은 시간을 칩으로 보여주고, 맞히면 초록 피드백을 줘요. 큰 둥근 버튼이라 잠결에도 누르기 쉬워요. | 규칙 설명이 짧아 처음 보면 헷갈려요. 미션 첫 화면에 한 줄 예시가 필요해요. |
| Brilliant | [Mobbin](https://mobbin.com/screens/a10baa28-d259-48e7-b739-977ad1f14014) | 위 진행 막대 + "처음부터" 버튼이 있어 틀려도 부담이 적어요. | 수식 키보드가 복잡해요. 우리는 숫자만 있는 키패드로 충분해요. |
| 알라미 (유아이볼, 확인만) | [문장 따라쓰기](https://uibowl.io/name/%EC%95%8C%EB%9D%BC%EB%AF%B8?patterns=%EA%B2%8C%EC%9D%B4%EB%AF%B8%ED%94%BC%EC%BC%80%EC%9D%B4%EC%85%98&patternName=%E1%84%86%E1%85%B5%E1%84%89%E1%85%A7%E1%86%AB%20%E1%84%86%E1%85%AE%E1%86%AB%E1%84%8C%E1%85%A1%E1%86%BC%20%E1%84%8E%E1%85%A1%E1%86%BD%E1%84%80%E1%85%B5), [스쿼트](https://uibowl.io/name/%EC%95%8C%EB%9D%BC%EB%AF%B8?patterns=%EA%B2%8C%EC%9D%B4%EB%AF%B8%ED%94%BC%EC%BC%80%EC%9D%B4%EC%85%98&patternName=%EB%AF%B8%EC%85%98-%EC%8A%A4%EC%BF%BC%ED%8A%B8) | (경쟁사 구조 파악용) 미션 설정은 "설명 + 횟수 휠 + 미리보기/완료 버튼" 구조예요. | **복제 금지.** 미션 이름("따라쓰기", "스쿼트" 등), 예시 문장, 어두운 시트 + 휠 + 하단 2버튼 배치를 그대로 쓰지 않아요. 우리는 미션 이름, 설정 순서, 레이아웃을 따로 정해요. |

### 2-6. 해제 후 (기상 확인) 화면

| 앱 | 링크 | 좋은 점 | 피할 점 |
|---|---|---|---|
| Brilliant | [Mobbin](https://mobbin.com/screens/a91a72d4-2f05-41f1-a499-b08bf6ef9a70) | 큰 숫자 "1" + "연속 시작!" + 이번 주 요일 칸 + 버튼 하나예요. 아주 짧아요. | 그래픽이 커서 아침에 눈이 부셔요. 밝기 대비를 조절해요. |
| BitePal | [Mobbin](https://mobbin.com/screens/b04e4b6c-9383-498a-b2d0-6541427e3cc6) | "2일 연속" 큰 숫자, 요일 7칸 중 오늘 표시, 공유 버튼과 다짐 버튼을 나란히 둬요. | 캐릭터가 귀여운 대신 앱 톤과 안 맞으면 어색해요. |
| Flo | [Mobbin](https://mobbin.com/screens/3ef54ae8-e4bf-49a1-b733-dd3037f75383) | 아침 리포트를 원형 점수 + 짧은 인사 + 수면 시간 2개 숫자로 보여줘요. | 정보가 휴대폰 추정치라 정확하지 않다는 안내가 길어요. 우리는 측정하지 않는 값은 보여주지 않아요. |
| 5 Minute Journal | [Mobbin](https://mobbin.com/screens/a4ad0893-10a6-401f-88e4-cb12c7c09acf) | "오늘 아침 기분은?" 한 질문 + 표정 슬라이더라 10초 안에 끝나요. | 기분 기록을 필수로 만들면 귀찮아져요. 선택 사항으로 둬요. |
| 삼쩜삼 (유아이볼) | [유아이볼](https://uibowl.io/name/%EC%82%BC%EC%A9%9C%EC%82%BC?patterns=%EA%B2%8C%EC%9D%B4%EB%AF%B8%ED%94%BC%EC%BC%80%EC%9D%B4%EC%85%98&patternName=%EC%88%98%EB%A9%B4) | "굿모닝, ○○님의 오늘 수면은 어땠나요?" + 시간 범위 6개 버튼 + 포인트 받기. 한국어 아침 인사 톤이 자연스러워요. | 포인트 보상이 주가 되면 기상 확인의 의미가 흐려져요. |
| 5 Minute Journal (스트릭) | [Mobbin](https://mobbin.com/screens/5e9256bc-653e-4a0a-ad0b-e23050997c56) | 연속 일수 카드 + 한 줄 설명 + 공유 버튼이에요. | 1일차부터 크게 축하하면 가벼워 보여요. 3일, 7일처럼 의미 있는 날에만 크게 축하해요. |

### 2-7. 온보딩

| 앱 | 링크 | 좋은 점 | 피할 점 |
|---|---|---|---|
| Joi | [Mobbin](https://mobbin.com/screens/e4c10a81-3148-49f2-aeb3-6b386e39badb) | "1단계" 표시 + 큰 제목 + 기상, 잠들 준비 시간 2칸 + 계속 버튼. 건너뛰기도 있어요. | 아이콘을 제목 글자 사이에 끼워 넣어 한글에서는 줄바꿈이 어색해질 수 있어요. |
| Google Health (Fitbit) | [Mobbin](https://mobbin.com/screens/52413952-4cbd-473c-8dab-13cba2171bd5) | 질문 + 이유 한 줄("실제 기상 시간과 비교할 수 있어요") + 시간 하나. 왜 묻는지 알려줘요. | 버튼이 오른쪽 아래 작게 있어 한 손으로 누르기 불편해요. |
| Garmin Connect | [Mobbin](https://mobbin.com/screens/e89437ea-8d82-4d20-a6fc-733427f8faa2) | 잠드는 시간, 일어나는 시간을 목록 2줄로 받고, 아래에 "이 시간엔 방해 금지가 켜져요"처럼 결과를 미리 알려줘요. | 대문자 제목과 긴 설명이 겹쳐 무거워요. |
| Pillow | [Mobbin 흐름](https://mobbin.com/flows/7e9a9f8c-77a2-45a4-8e58-146e6e5cb924) | 한 화면 한 질문, 아래 좌우 화살표 버튼, 선택지는 큰 카드예요. | 22화면으로 너무 길어요. 우리는 첫 알람까지 5화면 안쪽으로 줄여요. |
| Calm Sleep | [Mobbin 흐름](https://mobbin.com/flows/90f37c55-9c7e-490e-ae2c-95f20e8247c4) | 5화면으로 짧고, 밤하늘 일러스트로 분위기를 잡아요. | 콘텐츠 홍보 위주라 바로 쓰고 싶은 사람에게는 늦어요. |
| 알라미 스토어 이미지 (유아이볼, 확인만) | [유아이볼](https://uibowl.io/name/%EC%95%8C%EB%9D%BC%EB%AF%B8?patterns=iPhone%20%EC%8A%A4%ED%81%AC%EB%A6%B0%EC%83%B7) | (경쟁사 메시지 파악용) "기본 알람 여러 개 vs 우리 알람 하나" 비교로 가치를 전해요. | **복제 금지.** 같은 비교 구도와 문구를 쓰지 않아요. |

### 2-8. 권한 요청 화면

| 앱 | 링크 | 좋은 점 | 피할 점 |
|---|---|---|---|
| Brilliant | [Mobbin](https://mobbin.com/screens/c33921fd-b1a6-4f0e-8c59-70b4121a18c7) | 실제 알림 모양 카드를 미리 보여주고, "알림 허용 / 괜찮아요" 두 버튼을 크게 둬요. | 거절 버튼도 같은 크기라 괜찮지만, 알람 앱은 알림이 필수라 거절 시 다음 안내가 필요해요. |
| ChatGPT | [Mobbin](https://mobbin.com/screens/0fb5a49b-e98f-4f8f-803d-6571324fc342) | 종 아이콘 + 한 줄 이유 + 시스템 팝업이 바로 아래에 떠서 맥락이 이어져요. | iOS 팝업이라 안드로이드 모양과 달라요. 구조만 참고해요. |
| Cal AI | [Mobbin](https://mobbin.com/screens/021dbea1-92b5-4b3a-bed8-b1d677491e66) | 온보딩 진행 막대 안에 권한 단계를 넣고, 손가락 아이콘으로 "허용"을 가리켜요. | 팝업을 흉내 낸 그림은 사용자가 진짜 팝업과 헷갈릴 수 있어요. |
| 패스오더 (유아이볼) | [유아이볼](https://uibowl.io/name/%ED%8C%A8%EC%8A%A4%EC%98%A4%EB%8D%94?patterns=%EC%98%A8%EB%B3%B4%EB%94%A9&imgId=cmsph7sgi00r3l70469zksc4s) | 권한 4개를 아이콘 + 이름 + 이유 한 줄로 한 장에 정리하고, "허용 안 해도 쓸 수 있어요"를 밝혀요. | 한 장에 다 모으면 각 권한을 왜 켜야 하는지 덜 와닿아요. 알람 필수 권한은 따로 떼어 보여줘요. |
| 바나프레소 (유아이볼) | [유아이볼](https://uibowl.io/name/%EB%B0%94%EB%82%98%ED%94%84%EB%A0%88%EC%86%8C?patterns=%EC%98%A8%EB%B3%B4%EB%94%A9&imgId=cmvg2lnyy00ajjs06zmx7nu99) | 각 권한에 "(필수)", "(선택)"을 붙여 구분해요. 한국 사용자에게 익숙한 형식이에요. | 문장이 길고 글씨가 작아요. |
| 도미노 (유아이볼) | [유아이볼](https://uibowl.io/name/%EB%8F%84%EB%AF%B8%EB%85%B8?patterns=%EC%98%A8%EB%B3%B4%EB%94%A9&imgId=cmue0iit8004cl804kzsldvs5) | 제목에 "알림을 허용하고 ○○를 받아보세요"처럼 얻는 것을 먼저 말하고, 아래에 알림 예시 카드 목록을 깔아 둬요. | 예시 알림이 많아 팝업 뒤가 복잡해 보여요. |

---

## 3. 우리 화면에 가져올 원칙

1. **시간 숫자가 늘 1순위예요.** 목록, 설정, 울림 화면 모두 시간 숫자를 가장 크게 두고, 굵기는 너무 가늘지 않게 해요(새벽 저시력 상황).
2. **울림 화면은 결정 하나만 묻어요.** 주 버튼은 "미션 시작", 다시 울림은 작은 보조 버튼으로 둬요. 두 버튼은 크기, 색, 위치를 확실히 다르게 해서 실수를 막아요.
3. **미션은 숫자 키패드처럼 손에 익은 입력만 써요.** 한 화면에 문제, 진행(몇 개 중 몇 개), 남은 시간을 함께 보여주고, 비상 해제 방법은 숨기지 말고 작게라도 늘 보이게 해요.
4. **권한은 하나씩, 이유 먼저.** 알림, 정확한 알람, 배터리 최적화 제외, 다른 앱 위에 표시를 각각 한 화면으로 나눠요. 화면마다 "이걸 켜야 알람이 꼭 울려요" 같은 결과를 먼저 말하고 그다음 시스템 화면으로 보내요. 필수와 선택을 표시해요.
5. **기상 확인은 10초 안에 끝나게 해요.** 짧은 인사 + 연속 일수 + 이번 주 7칸 체크 + 버튼 하나. 기분 기록 같은 추가 입력은 선택으로 둬요.
6. **온보딩은 첫 알람 만들기까지 5화면 안쪽.** 한 화면 한 질문, 왜 묻는지 한 줄, 건너뛰기를 둬요.
7. **반복 요일은 한글 7칸 칩 + 빠른 선택(매일, 평일, 주말).** 켜짐은 채움, 꺼짐은 테두리로 구분해요.
8. **알라미와 겹치지 않게 해요.** 미션 이름, 아이콘, 미션 설정 시트 배치, 예시 문장, 스토어 비교 구도는 모두 새로 만들어요. 시안 검토 때 알라미 화면과 나란히 놓고 닮은 곳이 있는지 확인해요.

---

## 4. 막힌 것, 못 찾은 것

| 항목 | 상황 | 다음에 할 일 |
|---|---|---|
| Mobbin 안드로이드 화면 | Mobbin 도구가 iOS와 웹만 지원해서 안드로이드 화면은 못 봤어요. | 안드로이드 전용 모양(시스템 권한 화면, 전체 화면 알림)은 실제 기기 캡처로 따로 확인해요. |
| Alarmy, Sleep Cycle, Rise, Hatch, Structured, Fabulous | Mobbin 검색에서 이 앱 이름으로 결과가 나오지 않았어요(다른 앱만 나옴). | Mobbin 웹사이트에서 직접 앱 이름으로 찾아보거나 스토어 스크린샷을 봐요. |
| Google 시계, 삼성 시계 | 두 도구 모두에서 찾지 못했어요. | 우리 폰에서 직접 캡처해 비교해요(안드로이드 표준 감각 확인용). |
| 유아이볼 결과 수 제한 | 무료 등급이라 검색당 3건만 나와요. 알라미는 64개 패턴 중 3개만 확인했어요. "알람" 검색도 22건 중 3건만 봤어요. | 더 보려면 유아이볼 웹에서 로그인하거나 멤버십이 필요해요. |
| 유아이볼 "기상" 문구 검색 | "기상악화", "인기상품"처럼 엉뚱한 화면이 섞여 나왔어요. 쓸 만한 건 열품타 챌린지 목록 정도예요. | 다음엔 "굿모닝", "일어났" 같은 문구로 다시 찾아봐요. |
| 정확한 알람, 배터리 최적화 권한 화면 | 이 안드로이드 전용 권한을 설명하는 레퍼런스는 못 찾았어요. | 다른 안드로이드 알람 앱을 실제로 설치해 흐름을 확인해요. |
| 브라우저 확인 | MCP 도구가 잘 작동해서 브라우저는 쓰지 않았어요. 로그인은 하지 않았어요. | 없음 |

---

## 5. 출처 목록

### Mobbin (iOS)
- WHOOP 알람 일정 목록: https://mobbin.com/screens/7af269fb-2a3b-40f9-8730-8ae018912a96
- WHOOP 알람 방식 선택: https://mobbin.com/screens/799396aa-4f9f-40b9-8f9f-6aad1196643a
- WHOOP 기상 시간 휠 피커: https://mobbin.com/screens/9bf17146-28cb-4b00-ad26-3e9f55c5b4cc
- WHOOP 알람 끄는 중: https://mobbin.com/screens/a3b79a84-bd3d-4788-836c-6d6385aeeb98
- WHOOP 수면 권장 시간 모달: https://mobbin.com/screens/e2060b13-40ed-4b3c-bf11-ce10c9c3219b
- WHOOP 첫 주 기록: https://mobbin.com/screens/9214ddad-b502-43c5-aa25-70bd056e2c76
- Life Reset 스마트 알람 설정: https://mobbin.com/screens/1c4db405-f9f5-4f93-af23-0f81877c66ce
- Vocabulary 알람 설정: https://mobbin.com/screens/39552df0-5f7f-460d-97b0-94f4cd7c5d94
- Me+ 알람 목록: https://mobbin.com/screens/271bcaf6-57d2-4d0d-9233-51731dd4004f
- TIDE 알람 울림: https://mobbin.com/screens/951a907e-fbcd-4ba4-8e7f-c2ccec28ba5f
- Google Health (Fitbit) 기상 시간 질문: https://mobbin.com/screens/52413952-4cbd-473c-8dab-13cba2171bd5
- Garmin Connect 수면 시간 입력: https://mobbin.com/screens/e89437ea-8d82-4d20-a6fc-733427f8faa2
- Joi 기상/잠들 준비 시간: https://mobbin.com/screens/e4c10a81-3148-49f2-aeb3-6b386e39badb
- Joi 시간 휠 열림: https://mobbin.com/screens/fd0d0f1e-7246-45f3-8ab7-139186ed987a
- Lifesum 알림 요일 선택: https://mobbin.com/screens/407d6bc8-47d6-46c2-9a7f-4634fb5ea43e
- Opal 곱셈 문제 잠금 해제: https://mobbin.com/screens/3719ca66-9d12-44c0-a83e-041a342061ab
- Duolingo 숫자 퍼즐: https://mobbin.com/screens/93d23da8-8b8c-45a7-b01b-fcefd7a1074b
- Brilliant 문제 풀이: https://mobbin.com/screens/a10baa28-d259-48e7-b739-977ad1f14014
- Brilliant 연속 시작: https://mobbin.com/screens/a91a72d4-2f05-41f1-a499-b08bf6ef9a70
- Brilliant 알림 권한 안내: https://mobbin.com/screens/c33921fd-b1a6-4f0e-8c59-70b4121a18c7
- BitePal 연속 기록: https://mobbin.com/screens/b04e4b6c-9383-498a-b2d0-6541427e3cc6
- 5 Minute Journal 아침 기분: https://mobbin.com/screens/a4ad0893-10a6-401f-88e4-cb12c7c09acf
- 5 Minute Journal 연속 카드: https://mobbin.com/screens/5e9256bc-653e-4a0a-ad0b-e23050997c56
- Flo 아침 수면 리포트: https://mobbin.com/screens/3ef54ae8-e4bf-49a1-b733-dd3037f75383
- ChatGPT 알림 권한: https://mobbin.com/screens/0fb5a49b-e98f-4f8f-803d-6571324fc342
- Cal AI 알림 권한: https://mobbin.com/screens/021dbea1-92b5-4b3a-bed8-b1d677491e66
- Pillow 온보딩 흐름: https://mobbin.com/flows/7e9a9f8c-77a2-45a4-8e58-146e6e5cb924
- Pillow 수면 프로그램 시작 흐름: https://mobbin.com/flows/bc790f55-de9b-4965-8ed9-1f542439599a
- Calm Sleep 온보딩 흐름: https://mobbin.com/flows/90f37c55-9c7e-490e-ae2c-95f20e8247c4

### 유아이볼 (한국 앱 위주)
- 알라미 스토어 스크린샷: https://uibowl.io/name/%EC%95%8C%EB%9D%BC%EB%AF%B8?patterns=iPhone%20%EC%8A%A4%ED%81%AC%EB%A6%B0%EC%83%B7
- 알라미 미션(문장): https://uibowl.io/name/%EC%95%8C%EB%9D%BC%EB%AF%B8?patterns=%EA%B2%8C%EC%9D%B4%EB%AF%B8%ED%94%BC%EC%BC%80%EC%9D%B4%EC%85%98&patternName=%E1%84%86%E1%85%B5%E1%84%89%E1%85%A7%E1%86%AB%20%E1%84%86%E1%85%AE%E1%86%AB%E1%84%8C%E1%85%A1%E1%86%BC%20%E1%84%8E%E1%85%A1%E1%86%BD%E1%84%80%E1%85%B5
- 알라미 미션(스쿼트): https://uibowl.io/name/%EC%95%8C%EB%9D%BC%EB%AF%B8?patterns=%EA%B2%8C%EC%9D%B4%EB%AF%B8%ED%94%BC%EC%BC%80%EC%9D%B4%EC%85%98&patternName=%EB%AF%B8%EC%85%98-%EC%8A%A4%EC%BF%BC%ED%8A%B8
- 알라미 전체(64개 패턴): https://uibowl.io/name/%EC%95%8C%EB%9D%BC%EB%AF%B8
- 메디코치 알람 편집: https://uibowl.io/name/%EB%A9%94%EB%94%94%EC%BD%94%EC%B9%98?patterns=%ED%8E%B8%EC%A7%91%C2%B7%EC%88%98%EC%A0%95%ED%95%98%EA%B8%B0&patternName=%EC%95%8C%EB%9E%8C
- 깨비즈 알람 생성: https://uibowl.io/name/%EA%B9%A8%EB%B9%84%EC%A6%88?patterns=%EC%83%9D%EC%84%B1%ED%95%98%EA%B8%B0&patternName=%EC%95%8C%EB%9E%8C%20%EC%83%9D%EC%84%B1
- pillow 알람 울림: https://uibowl.io/name/pillow?patterns=%EC%83%81%EC%84%B8%EC%A0%95%EB%B3%B4%20%28PDP%29&patternName=%EC%95%8C%EB%9E%8C
- 삼쩜삼 아침 수면 기록: https://uibowl.io/name/%EC%82%BC%EC%A9%9C%EC%82%BC?patterns=%EA%B2%8C%EC%9D%B4%EB%AF%B8%ED%94%BC%EC%BC%80%EC%9D%B4%EC%85%98&patternName=%EC%88%98%EB%A9%B4
- 워크온 수면 통계: https://uibowl.io/name/%EC%9B%8C%ED%81%AC%EC%98%A8?patterns=%ED%86%B5%EA%B3%84%C2%B7%EB%A6%AC%ED%8F%AC%ED%8A%B8&patternName=%EC%88%98%EB%A9%B4
- OtterLife 수면 리포트(빈 화면 포함): https://uibowl.io/name/OtterLife?patterns=%ED%86%B5%EA%B3%84%C2%B7%EB%A6%AC%ED%8F%AC%ED%8A%B8&patternName=%EC%88%98%EB%A9%B4
- 열품타 기상 챌린지 목록: https://uibowl.io/name/%EC%97%B4%ED%92%88%ED%83%80?patterns=%EB%AA%A9%EB%A1%9D%20%28PLP%29&imgId=cmq968f5g00rkkw04x5px0qgg
- 패스오더 앱 권한 안내: https://uibowl.io/name/%ED%8C%A8%EC%8A%A4%EC%98%A4%EB%8D%94?patterns=%EC%98%A8%EB%B3%B4%EB%94%A9&imgId=cmsph7sgi00r3l70469zksc4s
- 도미노 알림 허용 온보딩: https://uibowl.io/name/%EB%8F%84%EB%AF%B8%EB%85%B8?patterns=%EC%98%A8%EB%B3%B4%EB%94%A9&imgId=cmue0iit8004cl804kzsldvs5
- 바나프레소 권한설정: https://uibowl.io/name/%EB%B0%94%EB%82%98%ED%94%84%EB%A0%88%EC%86%8C?patterns=%EC%98%A8%EB%B3%B4%EB%94%A9&imgId=cmvg2lnyy00ajjs06zmx7nu99
- 카카오워크 접근 권한 요청: https://uibowl.io/name/%EC%B9%B4%EC%B9%B4%EC%98%A4%EC%9B%8C%ED%81%AC?patterns=%EC%8B%A0%EC%B2%AD%ED%95%98%EA%B8%B0&patternName=%EC%A0%91%EA%B7%BC%20%EA%B6%8C%ED%95%9C%20%EC%9A%94%EC%B2%AD
