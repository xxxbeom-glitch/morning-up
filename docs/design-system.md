# morning-up 디자인 시스템

피그마 morning-up 파일(`MazZLYfaQndMh9AxlRcz2v`)의 `09 디자인 시안` 페이지에 있는 규칙을 정리한 문서예요.
홈(A)과 알람 편집(B) 화면 기준이고, 알람 울림(D)은 아직 예전 다크 디자인이에요.
2026-11-16 기준이에요.

## 방향

- 캐시워크식 밝은 라이트: 밝은 회색 바탕, 흰 카드, 옅은 그림자, 강조색 초록.
- 글래스 효과, 테두리 선, 구분선은 쓰지 않아요.
- 선택 표시는 칠로만 해요. 선택 버튼에 테두리를 두르지 않아요.
- 아이콘은 Lucide 하나로 통일해요 (선 두께 2, 크기 24).
- 화면에 쓰는 색, 글자, 간격, 라운드, 그림자는 모두 아래 변수와 스타일에 묶어요.

## 색 변수 (`morning-up 색`)

모드는 '라이트'와 '다크'가 있어요. 지금 시안은 라이트를 써요.

| 변수 | 라이트 | 다크 | 쓰는 곳 |
|---|---|---|---|
| `bg/base` | #F4F5F7 | #0B0E11 | 화면 바탕 |
| `bg/elevated` | #FFFFFF | #161B20 | 떠 있는 바탕 |
| `surface/card` | #FFFFFF | #1E252C | 카드, 하단 메뉴, 토글 손잡이 |
| `surface/raised` | #F1F3F5 | #2A323A | 선택 안 된 요일 칸 |
| `line/default` | #E5E8EB | #323B44 | 꺼진 토글 바탕 |
| `text/primary` | #191B1E | #FFFFFF | 제목, 시간, 기본 아이콘 |
| `text/body` | #4E5560 | #D1D5DA | 설정 줄 라벨 |
| `text/secondary` | #6B7178 | #A1AAB3 | 보조 글, 꺼진 값 |
| `text/tertiary` | #B0B6BC | #69737D | 흐린 글, 안 쓰는 요일, 비활성 아이콘 |
| `text/inverse` | #FFFFFF | #0B0E11 | 초록 버튼 위 글자 |
| `accent/green` | #00C281 | #00F19F | 강조색, 켜진 토글, 켜진 값 |
| `accent/green-soft` | #E6F9F2 | #1A3D3A | 선택한 요일 칸, 선택된 아이콘 버튼 바탕 |
| `accent/yellow` | #FFC800 | #FFD60A | 예비 |
| `accent/blue` | #2D7FF9 | #0093E7 | 예비 |

## 크기 변수 (`morning-up 크기`)

간격은 4의 배수로 맞춰요.

| 묶음 | 변수와 값 |
|---|---|
| 간격 | `space/4` 4, `space/8` 8, `space/12` 12, `space/16` 16, `space/20` 20, `space/24` 24, `space/32` 32, `space/40` 40, `space/48` 48 |
| 화면 배치 | `layout/screen-margin` 16 (화면 좌우 여백), `layout/card-gap` 12 (카드 사이), `layout/card-padding` 24 (카드 안쪽 여백), `layout/header-top` 72 (홈 상단 문구 위 여백) |
| 라운드 | `radius/card` 24, `radius/tile` 16, `radius/button` 16, `radius/chip` 12, `radius/checkbox` 6, `radius/full` 999 |
| 크기 | `size/row` 36, `size/button` 48, `size/button-lg` 56, `size/chip` 36, `size/icon-box` 40, `size/checkbox` 20 |
| 토글 | `size/switch-w` 48, `size/switch-h` 28, `size/switch-sm-w` 40, `size/switch-sm-h` 24, `size/switch-inset` 3 (손잡이와 틀 사이) |
| 아이콘 | `icon/sm` 16, `icon/md` 20, `icon/lg` 24 |

## 글자 스타일

글꼴은 Pretendard예요.

| 스타일 | 굵기 | 크기/줄 높이 | 자간 | 쓰는 곳 |
|---|---|---|---|---|
| `morning-up/드럼롤` | SemiBold | 40/48 | -2% | 알람 편집 시간 드럼롤 (세 줄 모두) |
| `morning-up/숫자` | SemiBold | 28/36 | -2% | 홈 알람 카드 시간 |
| `morning-up/큰 제목` | Bold | 24/32 | -2% | 홈 상단 남은 시간 문구 |
| `morning-up/제목` | SemiBold | 16/24 | -1% | 헤더 제목, 카드 제목, 큰 버튼 글자 |
| `morning-up/본문` | Medium | 14/20 | 0 | 설정 줄 라벨, 안내 글 |
| `morning-up/본문 강조` | SemiBold | 14/20 | 0 | 보통 버튼 글자 |
| `morning-up/보조` | Medium | 13/18 | 0 | 날짜 같은 보조 글 |
| `morning-up/보조 강조` | SemiBold | 13/18 | 0 | 요일 칸 글자 |
| `morning-up/캡션` | Medium | 12/16 | 0 | 설정 줄 값, 알람 카드의 안 쓰는 요일 |
| `morning-up/캡션 강조` | SemiBold | 12/16 | 0 | 알람 카드의 설정한 요일 |

## 그림자 스타일

색은 모두 #1A1F26이에요.

| 스타일 | 아래로 | 퍼짐 | 진하기 | 쓰는 곳 |
|---|---|---|---|---|
| `shadow/card` | 2 | 12 | 5% | 홈 알람 카드, 편집 화면 카드 |
| `shadow/float` | 4 | 20 | 8% | 떠 있는 하단 메뉴 |
| `shadow/knob` | 1 | 3 | 18% | 토글 손잡이 |

## 부품

09 페이지 오른쪽 `Components` 영역에 모여 있어요. Alarm Card만 그 위쪽에 따로 있어요.

| 부품 | 베리언트 | 바꿀 수 있는 것 | 쓰는 곳 |
|---|---|---|---|
| Toggle | Size: Default(48x28), Small(40x24) × State: On, Off | | 알람 카드(Default), 설정 줄(Small) |
| Setting Row | State: On, Off | Label, Value 글자 | 편집 화면 설정 줄 |
| Day Chip | State: Selected, Default | Label 글자 | 반복 요일 칸 |
| Button | Size: Large(56), Medium(48) | Label 글자 | 하단 고정 저장 버튼(Large) |
| Icon Button | State: Default, Active | Icon (아이콘 바꾸기) | 하단 메뉴, 목록 위 +와 더보기, 헤더 뒤로가기 |
| Top Bar | Type: Back, Title Only | Title 글자 | 편집 화면 헤더 |
| Alarm Card | State: On, Off | | 홈 알람 목록 |
| 아이콘 | alarm, chart, settings, plus, ellipsis-vertical, chevron-left | | Icon Button과 헤더 안 |

### 부품별 규칙

- **Toggle**: 켜면 `accent/green`, 끄면 `line/default` 바탕이에요. 손잡이는 `surface/card`에 `shadow/knob`을 써요.
- **Setting Row**: 왼쪽에 라벨, 그 아래 값, 오른쪽에 작은 토글이 있어요. 켜면 값이 초록, 끄면 회색이에요. 라벨과 값 사이는 4예요. 줄 사이 구분선은 없어요. 토글을 켜서 추가 설정이 필요하면 바텀시트를 띄워 이어가요.
- **Day Chip**: 36x36 정사각형이에요. 선택하면 `accent/green-soft` 바탕에 초록 글자, 아니면 `surface/raised` 바탕에 회색 글자예요. 테두리는 쓰지 않아요.
- **Icon Button**: 48 원형이에요. Active는 `accent/green-soft` 바탕과 초록 아이콘이에요.
- **Top Bar**: 높이 56이고 상태 바(32) 아래에 둬요. 뒤로가기 아이콘은 화면 왼쪽에서 16 떨어져 있어요.
- **Alarm Card**: 시간, 월~일 요일(설정한 요일만 초록), 오른쪽 기본 토글이 있어요.

## 화면별 메모

### 홈 (A)

- 상단에 "7시간 20분 후,\n알림이 울립니다"(큰 제목)와 날짜가 있어요. 위 여백은 `layout/header-top` 72예요.
- 그 아래 오른쪽에 +와 더보기 아이콘 버튼이 있고, 그 아래 알람 카드가 이어져요.
- 하단 메뉴는 가운데 떠 있는 둥근 바에 아이콘 버튼 3개만 있어요.

### 알람 편집 (B)

- Top Bar 아래에 `time-picker` 묶음이 있어요. 안내 글('7시간 20분 뒤에 울려요')과 시간 드럼롤로 되어 있고, 위아래 여백은 32, 둘 사이는 24예요.
- 드럼롤은 세 줄 모두 같은 크기(40)예요. 선택한 줄만 크게 하지 않아요. 선택한 줄은 흰 띠(높이 64, `radius/tile`)와 진한 글자로, 위아래 줄은 연한 글자(`text/tertiary`)로만 구분해요.
- 콜론은 숫자 높이의 가운데에 맞춰요. Pretendard 콜론은 원래 4쯤 아래에 그려지므로 그만큼 올려요.
- 카드는 3개예요.
  - 반복: 요일 칸과 '공휴일에는 끄기'
  - 기상 미션: 미션 선택, 다시 울림, 다시 잠들기 방지, 전원 종료 방지
  - 소리와 진동: 알람음, 볼륨 및 진동, 부드럽게 깨우기
- 저장은 하단에 고정된 초록 버튼(Large 56)이에요. 버튼 둘레 여백은 모두 16이에요. 뒤 바탕은 투명에서 `bg/base`로 흐려지는 그라데이션이에요.

## 예외

- 하단 저장 영역 그라데이션의 첫 지점(투명)은 변수에 묶으면 투명도가 사라지므로 원색 값으로 둬요. 나머지 지점은 `bg/base`에 묶여 있어요.
