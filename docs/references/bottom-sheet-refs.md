# 바텀시트, 모달 레퍼런스 (11-27)

- 관리판: TSK-302, 맡은 방 `[디자인 27]`
- 피그마: `화면` 페이지, 구역 '바텀시트 레퍼런스' (노드 `1239:22297`), 11-27 구역(`1218:20106`) 오른쪽 x 31000
- 출처: 사용자가 준 Pinterest 4개, Mobbin(iOS 앱), 유아이볼(국내 안드로이드, iOS 앱). 시안이 아니라 참고 자료예요.
- 고른 기준: 줄 수가 적은 것, 제목과 선택지가 바로 보이는 것, 카드 안에 카드가 없는 것, 버튼이 하나나 둘인 것.
- 우리 시트 이름: 미루기, 다시 잠들기 방지, 미션 설정, Crazy Mode 속도, 좀비 모드 횟수, 한 주 시작 요일, 이어폰 다이얼로그

## 우리에게 맞는 패턴 3가지

| 패턴 | 참고 | 우리 부품으로 짜는 법 | 쓸 곳 |
|---|---|---|---|
| 1. 고르면 바로 닫히는 Radio Row 시트 | 01 Headway, 02 Spotify, 05 Pillow | Sheet Header(제목만) + Radio Row 4~7줄. 버튼 없이 누르면 적용하고 닫혀요. 줄 왼쪽은 비우지 않고 라디오나 체크를 둬요. | 미루기 간격, 한 주 시작 요일, 다시 잠들기 방지 확인 시간 |
| 2. 큰 값 하나 + 버튼 하나 | 21 Apple Store, 23 Monzo, 12 CREME, 15 Liven | Sheet Header + 가운데 큰 값 한 줄(+, - 횟수 부품 또는 Choice Chip 한 줄) + Button 하나. '횟수' 글자는 제목에만 한 번 써요. | 좀비 모드 횟수, Crazy Mode 속도, 미루기 사용자 정의 시간 |
| 3. 스위치 한 줄 + 아래 상세 값 | 27 DoorDash, 26 Booking.com, 28 Polestar | Sheet Header 오른쪽에 스위치(헤더 스위치 규칙) + 켜졌을 때만 아래에 Choice Chip이나 Radio Row. 설명은 한 줄. Button 하나. | 미루기, 다시 잠들기 방지, 미션 설정 |

이어폰 다이얼로그는 F 묶음(35 Withings, 36 Kit)처럼 제목, 설명 두 줄, 버튼 두 개 가로가 가장 단순해요. '다시 보지 않기'가 필요하면 38 CapCut을 봐요.

## 0. 사용자 레퍼런스 (사용자가 직접 고른 것)

영상은 내장 브라우저로 열어 8장면을 뽑아 한 장으로 붙였어요(피그마 U1, U2).

| 번호 | 출처 | 사용자 설명 | 무엇이 어떻게 보이고 움직이는지 | 쓸 곳 | 링크 |
|---|---|---|---|---|---|
| U1 | Airlearn 위젯 안내 (영상 15초) | 미션이나 권한 안내 설명에 넣을 애니메이션과 시트 방식 | 시트 위쪽 절반이 그림 칸. 그 안에서 홈 화면 아이콘이 흔들리고, Edit 버튼이 커지고, 위젯 고르기 작은 시트가 올라오는 동작이 차례로 반복돼요. 아래는 제목, 설명 한 줄, 번호 붙은 단계 2줄, 버튼 하나로 고정. 움직이는 건 그림 칸뿐이에요. | 미션 시트 예제 영역, 첫 실행 권한 안내(16번) | [Pinterest](https://kr.pinterest.com/pin/280771358014596499/) |
| U2 | 저축 계산 화면 (영상 22초, 국내 금융 앱) | 레벨이나 문제 수를 조절할 때 쓸 슬라이드 형식 | 위에 결과 숫자가 크게, 아래 카드에 슬라이더 2개. 슬라이더 제목 글자 안에 지금 값이 들어가고('한 달에 80만원'), 양 끝에 최소, 최대 값이 작게 있어요. 손을 움직이면 위 결과 숫자가 바로 바뀌어요. 눈금 표시는 없고 '직접 입력' 글자 버튼이 따로 있어요. | 미션 난이도, 문제 수, Crazy Mode 속도, 좀비 모드 횟수 | [Pinterest](https://kr.pinterest.com/pin/280771358014596501/) |
| U3 | 뱅크샐러드 동의 시트 (이미지) | 권한 안내 때 제목 아래에 가로형 그림이나 애니메이션 | 닫기 X, 가운데 제목 2줄, 설명 2줄, 그 아래 연한 회색 가로형 그림 칸, 작은 안내 한 줄, 아니오, 예 2버튼. 그림 칸은 시트 폭보다 조금 좁고 모서리가 둥글어요. | 첫 실행 권한 안내, 다시 잠들기 방지 설명 | [Pinterest](https://kr.pinterest.com/pin/60517188735332363/) |
| U4 | 토스 기차 예매 인원 (이미지) | 카운트 UI를 쓸 거라면 이런 구성 | 제목 '인원' 아래 줄 3개. 왼쪽에 이름과 작은 설명, 오른쪽에 -, 숫자, + 묶음. 고른 값은 이름 옆 파란 글자('어른 7명')로 한 번 더 보여요. 아래 안내 두 줄과 버튼 하나. | 좀비 모드 횟수, 미션 문제 수, 다시 울림 횟수 | [Pinterest](https://kr.pinterest.com/pin/825143963007075772/) |

### 슬라이더와 기존 부품 비교 (슬라이더는 아직 design-system에 없어요)

| 부품 | 좋은 점 | 아쉬운 점 | 맞는 곳 |
|---|---|---|---|
| 슬라이더 (U2, 15 Liven) | 단계가 많아도 한 줄. 끌면서 바로 느낌을 봐요 | 정확한 값 맞추기가 어렵고, 손이 작은 값 차이를 놓쳐요. 새 부품을 만들어야 해요 | Crazy Mode 속도(10단계), 미션 난이도 |
| Choice Chip | 고를 값이 한눈에 다 보이고 한 번에 눌러요. 이미 있는 부품 | 값이 6개를 넘으면 두 줄이 되거나 넘겨야 해요 | 미루기 간격, 문제 수(3~5개 값) |
| +, - 횟수 부품 (U4, 21 Apple Store) | 정확한 숫자. 이미 있는 부품 | 값 차이가 크면 여러 번 눌러야 해요 | 좀비 모드 횟수, 다시 울림 횟수 |

## A. 하나 고르기 (Radio Row)

| 번호 | 앱 | 왜 좋은지 | 쓸 곳 | 링크 |
|---|---|---|---|---|
| 01 | Headway | 라디오 원이 왼쪽에 있고 누르면 바로 적용. 제목 하나, 버튼 없음 | 미루기 간격, 한 주 시작 요일 | [Mobbin](https://mobbin.com/screens/8ad4417e-2e1c-416e-8662-2fbce656260d) |
| 02 | Spotify | 글만 있는 줄. 고르면 시트가 닫힘. 가장 가벼움 | 미루기 간격 | [Mobbin](https://mobbin.com/screens/6ec07461-8c5c-4a56-92dc-4e00a9e6f42f) |
| 03 | ElevenReader | 고른 줄만 오른쪽 체크. 아래 버튼 하나로 저장 | 미루기 간격 | [Mobbin](https://mobbin.com/screens/86912e17-3e84-4689-983f-42ac54505aac) |
| 04 | The New York Times | 제목 아래 구분선 하나, 줄 간격 넓음 | 미루기 간격 | [Mobbin](https://mobbin.com/screens/3e972e04-e561-4816-b146-17958df3efcd) |
| 05 | Pillow (수면 알람) | 고른 줄은 바탕색으로만 표시. 테두리 없음 | 미루기 간격, 다시 잠들기 방지 | [Mobbin](https://mobbin.com/screens/83073b88-c98c-4524-8e93-996630b6a580) |
| 06 | Linear | 왼쪽 이름, 오른쪽 결과 값. 무엇이 바뀌는지 바로 보임 | 다시 잠들기 방지 (확인 시간) | [Mobbin](https://mobbin.com/screens/71e64165-edeb-4ae5-a51c-f7776c8e5629) |
| 07 | Copilot (금융) | 선택지 3개만 가운데 정렬. 아주 짧은 시트 | 한 주 시작 요일 | [Mobbin](https://mobbin.com/screens/de8d202b-63e1-49cc-a941-7e0be3bd51c5) |
| 08 | Atoms | 아이콘, 제목, 설명 한 줄, 선택지 4개, 버튼 2개 | 다시 잠들기 방지 | [Mobbin](https://mobbin.com/screens/f51cb078-371c-42dd-9e78-fd5d656a47af) |
| 09 | KB스타뱅킹 | 오른쪽 체크, 닫기 X만. 국내 앱 기본형 | 한 주 시작 요일 | [유아이볼](https://uibowl.io/name/KB%EC%8A%A4%ED%83%80%EB%B1%85%ED%82%B9?patterns=%EC%9D%B8%EC%A6%9D%ED%95%98%EA%B8%B0&imgId=cmvym4w3n001sjs08e3ktl1qq) |
| 10 | iM뱅크 | 라디오 원이 오른쪽. 긴 목록도 깔끔 | 미루기 간격 | [유아이볼](https://uibowl.io/name/iM%EB%B1%85%ED%81%AC?patterns=%EB%82%B4%EC%97%AD&imgId=cmp0txdm900jhl204vgbdwgxj) |
| 11 | 골프존 | 소제목 2개로 묶은 라디오 + 초기화, 적용 2버튼 | 미션 설정 (난이도, 개수) | [유아이볼](https://uibowl.io/name/%EA%B3%A8%ED%94%84%EC%A1%B4?patterns=%ED%95%84%ED%84%B0&imgId=cmunkg9uq00aajq04sxhw8u9s) |

## B. 값 고르기 (칩, 슬라이더, 큰 숫자)

| 번호 | 앱 | 왜 좋은지 | 쓸 곳 | 링크 |
|---|---|---|---|---|
| 12 | CREME | 큰 숫자 하나 + 확인 버튼. 아래 작은 '직접 입력' | 미루기 사용자 정의 시간 | [Mobbin](https://mobbin.com/screens/e2335f1f-6f5d-49a2-806e-f5eebf850c81) |
| 13 | Transit | 분 칩이 가로로 넘어가고 기본값 표시. 버튼 하나 | 미루기 간격 (칩형) | [Mobbin](https://mobbin.com/screens/0f79582b-27a5-46f1-8932-2c316b97c18d) |
| 14 | Brink | 눈금자 + 큰 숫자 15 MIN. 시작 버튼 하나 | 미루기 사용자 정의 시간 | [Mobbin](https://mobbin.com/screens/26b9caca-9522-4631-b8d1-41282ec5cb49) |
| 15 | Liven | 값 이름이 위에 늘어선 단계 슬라이더 | Crazy Mode 속도 (10단계) | [Mobbin](https://mobbin.com/screens/faab3d56-ef50-43dd-8d40-d8605e526a4c) |
| 16 | Epidemic Sound | 설명 한 줄 + 양 끝 값 표시 슬라이더 + 저장 | Crazy Mode 속도 | [Mobbin](https://mobbin.com/screens/85178e11-13ad-4f88-8527-4ac5788aa2a8) |
| 17 | Chime (금융) | 큰 숫자, 슬라이더, 버튼. 위에서 아래로 한 줄씩 | Crazy Mode 속도 | [Mobbin](https://mobbin.com/screens/4f6cc1f4-b7de-42f1-8104-52132fda3c12) |
| 18 | Setel | 값 상자 + 슬라이더 + 양 끝 범위 + 버튼 | Crazy Mode 속도 | [Mobbin](https://mobbin.com/screens/2e8187ff-6948-414c-bc45-1a64e01c65c7) |
| 19 | The Outsiders (건강) | 큰 시간 숫자 + 추천 구간이 보이는 슬라이더 | 미루기 사용자 정의 시간 | [Mobbin](https://mobbin.com/screens/c904709a-0683-410b-8611-79589b9e4644) |
| 20 | Strava | 단계 슬라이더 + 고른 단계 설명이 아래에 | Crazy Mode 속도, 미션 난이도 | [Mobbin](https://mobbin.com/screens/13b7cbb1-1ff9-449e-af27-e6c236eb6710) |

## C. 횟수 (+, - 부품)

| 번호 | 앱 | 왜 좋은지 | 쓸 곳 | 링크 |
|---|---|---|---|---|
| 21 | Apple Store | 제목, 설명, +, - 하나, 버튼 하나. 가장 단순 | 좀비 모드 횟수 | [Mobbin](https://mobbin.com/screens/58317aed-ad22-4043-bb85-4d2dc246612b) |
| 22 | BitePal (건강) | +, - 와 눈금을 한 줄에. 저장 하나 | 좀비 모드 횟수 | [Mobbin](https://mobbin.com/screens/88b78f24-9b47-445f-b32c-5cc2a33c89d8) |
| 23 | Monzo (금융) | 큰 제목 + 한 줄짜리 +, - + 확인 | 좀비 모드 횟수, 다시 울림 횟수 | [Mobbin](https://mobbin.com/screens/ebf6491e-4042-46f8-944d-b57604835f57) |
| 24 | Bevel (건강) | 큰 숫자 양옆 +, -. 아래 줄 하나와 버튼 | 좀비 모드 횟수 | [Mobbin](https://mobbin.com/screens/99156b40-816b-42b2-8e95-e997608f4fac) |
| 25 | Klook | 이름 왼쪽, +, - 오른쪽 줄 여러 개 | 미션 설정 (문제 수) | [Mobbin](https://mobbin.com/screens/0d74feb8-eb46-417d-89ef-26c3ecda1fab) |
| 26 | Booking.com | +, - 줄 아래 스위치 줄 하나 | 다시 잠들기 방지 (횟수 + 켜기) | [Mobbin](https://mobbin.com/screens/c11fa0f8-efb7-4903-9ded-6e06cb928d0f) |

## D. 켜고 끄기 + 상세 값

| 번호 | 앱 | 왜 좋은지 | 쓸 곳 | 링크 |
|---|---|---|---|---|
| 27 | DoorDash | 제목, 설명 한 줄, 스위치 2개, 완료 버튼 | 다시 잠들기 방지 | [Mobbin](https://mobbin.com/screens/eaa33f03-078a-4af4-b9e0-661ec10ad970) |
| 28 | Polestar | 소제목 아래 라디오, 그 아래 스위치 목록 | 미루기 (켜기 + 간격) | [Mobbin](https://mobbin.com/screens/226ebf9b-56bf-4cbb-b32d-8e60c0544b54) |
| 29 | ElevenReader | 스위치 2줄만 있는 작은 시트 | 다시 잠들기 방지 | [Mobbin](https://mobbin.com/screens/0069b1e2-4e75-4fee-8bd3-6dece98e1cbf) |
| 30 | Airalo | 설명 한 줄 + 스위치 2줄. 닫기 X | 이어폰 설정 | [Mobbin](https://mobbin.com/screens/867b8579-57fe-45d4-ad6b-6cde99dd18f2) |

## E. 미리보기가 있는 설정

| 번호 | 앱 | 왜 좋은지 | 쓸 곳 | 링크 |
|---|---|---|---|---|
| 31 | Chime (금융) | 큰 미리보기 + 아래 점 선택 + 버튼 하나 | 미션 설정 (미리보기) | [Mobbin](https://mobbin.com/screens/ebc7ce66-26e2-4540-89f0-f37d572dcbdb) |
| 32 | Instagram | 미리보기가 시트 대부분. 취소, 적용 2버튼 | 미션 미리 해보기 | [Mobbin](https://mobbin.com/screens/7d276c82-caa4-4b53-b442-e03657240fd4) |
| 33 | 석가머니 | 소리 칸 2열 + 아래 음량 슬라이더 | 벨소리, 볼륨 | [유아이볼](https://uibowl.io/name/%EC%84%9D%EA%B0%80%EB%A8%B8%EB%8B%88?patterns=%EC%84%A4%EC%A0%95&imgId=cmvfy6tmj002hl206hl2cueoy) |
| 34 | Character AI | 작은 미리보기 + 색 칩 + 줄 하나 | 미션 설정 | [Mobbin](https://mobbin.com/screens/bb280689-9622-4f6a-9254-b56140d793f8) |

## F. 확인 한 번 (가운데 다이얼로그)

| 번호 | 앱 | 왜 좋은지 | 쓸 곳 | 링크 |
|---|---|---|---|---|
| 35 | Withings (건강) | 제목, 설명, 버튼 2개 가로. 기본 다이얼로그 | 이어폰 다이얼로그 | [Mobbin](https://mobbin.com/screens/649fa47f-56fa-4924-a335-357638705d86) |
| 36 | Kit (금융) | 제목, 설명 2줄, 취소, 예 가로 버튼 | 이어폰 다이얼로그 | [Mobbin](https://mobbin.com/screens/f18f009f-9069-4845-86d1-2a4c1b22cd7f) |
| 37 | Life Reset | 설명 + 버튼 2개 세로. 주 버튼만 색 | 이어폰 다이얼로그 | [Mobbin](https://mobbin.com/screens/104bab06-a01d-47aa-bc08-1de4360322ce) |
| 38 | CapCut | 설명 + '다시 보지 않기' 체크 + 확인, 취소 | 이어폰 다이얼로그 | [Mobbin](https://mobbin.com/screens/26ab78b5-9465-46d2-8517-9dd5ce08a249) |
| 39 | Perplexity | 아이콘, 제목, 설명, 글자 버튼 하나인 시트 | 이어폰 안내 (시트형 대안) | [Mobbin](https://mobbin.com/screens/971399b7-2c04-4fec-a708-f8c26d9f8be7) |

## 참고할 때 주의

- 08 Atoms, 11 골프존, 31 Chime처럼 선택지에 테두리나 칸이 있는 것은 구성만 봐요. 우리 규칙은 고른 칸을 칠로만 표시해요(테두리 금지).
- 안드로이드 전용 레퍼런스는 Mobbin에 없어서 국내 앱(유아이볼)으로 채웠어요.
