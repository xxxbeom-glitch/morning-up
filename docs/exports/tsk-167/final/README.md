# 앱 아이콘 최종 (TSK-167, 11-25)

원본: 피그마 `화면` 페이지 프레임 `앱로고_지발좀인나유`(`1093:14193`, 360x360). 사용자가 만든 디자인을 그대로 썼어요(바꾼 것 없음).

## 구성
- 배경: 단색 `#0B6CF0`
- 앞 그림: 흰 글자 '지발좀 / 인나유'(글꼴은 윤곽선으로 바꿔서 글꼴 파일이 필요 없어요)

## 안전 영역 확인
- 원본 프레임 360을 아이콘에서 보이는 72dp 칸으로 잡고, 108dp 레이어 가운데에 놓았어요(사방 18dp 여백).
- 글자의 가장 먼 점은 가운데서 162.3(프레임 단위)이에요. 66dp 안전 원(반지름 165)과 보이는 72dp 원(반지름 180) 안에 들어가요. 원, 둥근 사각, 사각 마스크 모두 잘리지 않아요(`preview-masks.png`).
- 피그마 SVG를 360으로 그려 피그마 캡처와 비교했을 때 평균 차이는 0.24/255였어요. 눈에 띄게 다른 픽셀은 없었어요.

## 파일
| 경로 | 쓰임 |
|---|---|
| `res/mipmap-anydpi-v26/ic_launcher.xml`, `ic_launcher_round.xml` | 적응형 아이콘 정의(배경 색, 앞 그림 벡터, 테마 아이콘용 monochrome) |
| `res/drawable/ic_launcher_foreground.xml` | 앞 그림 벡터(108dp, viewport 540) |
| `res/values/ic_launcher_background.xml` | 배경 색 `ic_launcher_background` |
| `res/mipmap-{mdpi~xxxhdpi}/ic_launcher_foreground.png`, `ic_launcher_background.png` | 앞 그림, 배경 PNG(108dp: 108, 162, 216, 324, 432px). 벡터 대신 쓸 때 |
| `res/mipmap-{mdpi~xxxhdpi}/ic_launcher.png`, `ic_launcher_round.png` | 안드로이드 7 이하용 아이콘(48dp: 48, 72, 96, 144, 192px) |
| `playstore-icon-512.png` | 플레이 스토어 512px |
| `source/` | 피그마 원본 SVG, 앞 그림과 배경 SVG(540, 108dp 기준) |
| `preview-masks.png` | 안전 원(빨강 66dp, 노랑 72dp)과 마스크 3종 미리보기 |

monochrome(안드로이드 13 테마 아이콘)은 앞 그림을 그대로 썼어요. 필요 없으면 그 줄을 빼면 돼요.
