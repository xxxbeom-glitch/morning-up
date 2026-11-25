# 앱 아이콘 일어나ver2 (TSK-200, 11-25)

원본: 피그마 `화면` 페이지 프레임 `앱로고_일어나ver2`(`1124:21227`, 360x360). 이 이름 말고 '일어나'가 든 프레임은 없었어요. 사용자 디자인을 그대로 썼어요(바꾼 것 없음).

## 구성
- 배경: 단색 `#171A1F`
- 앞 그림: 흰 손글씨 선 8개(선 두께 5.6, 둥근 끝, 둥근 이음). 벡터에서도 선(stroke)으로 그대로 그려요.
- monochrome(테마 아이콘): 앞 그림 그대로

## 안전 영역
- 프레임 360을 보이는 72dp 칸으로 잡고 108dp 레이어 가운데에 놓았어요(TSK-188과 같은 방식).
- 선 두께까지 넣은 가장 먼 점은 가운데서 151.7(프레임 단위). 66dp 안전 원(반지름 165) 안이라 원, 둥근 사각, 사각 마스크 모두 잘리지 않아요(`preview-masks.png`, 빨강 66dp, 노랑 72dp).
- 피그마 캡처(`source/figma-capture-360.png`)와 직접 그린 360 그림(`render-360.png`)의 평균 차이는 0.43/255, 크게 다른 픽셀(64 넘게 차이) 0개예요.

## 파일
| 경로 | 쓰임 |
|---|---|
| `preview-masks.png` | 108dp 레이어와 안전 원, 원, 둥근 사각, 사각 마스크 |
| `preview-launcher.png` | 앱 서랍 크기로 본 모습 |
| `playstore-icon-512.png` | 플레이 스토어 512px(앱 `app/src/main/ic_launcher_app-playstore.png`와 같음) |
| `source/app-logo-figma.svg` | 피그마에서 내보낸 SVG |
| `source/make_icon.py` | SVG로 앱 리소스(벡터, 옛 기기 png, 둥근 png, 스토어 512)와 미리보기를 만드는 스크립트. `python make_icon.py <저장소 경로>` |

앱에서 바뀐 것: `app/src/main/res`의 `ic_launcher_app*` 리소스(이름 그대로), 배경 색 `ic_launcher_app_background`.
