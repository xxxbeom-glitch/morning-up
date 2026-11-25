# TSK-206 스플래시 손글씨 빠르기 미리보기

- `splash-handwriting.gif`: 스플래시에서 '일어나'가 한 획씩 쓰이는 모습(3초 쓰기 + 0.5초 멈춤, 초당 25장).
- `splash-handwriting-done.png`: 다 쓴 모습. 움직임 줄이기를 켜면 처음부터 이 모습이에요.
- `make_preview.py`: GIF를 다시 만드는 스크립트. 앱 코드(`SplashHandwriting.kt`)의 획, 시간표, 빠르기 곡선을 그대로 읽어 그려요.

## 주의
- 이 GIF는 폰 화면 녹화가 아니라 앱과 같은 계산으로 그린 재현본이에요. 실제 폰 녹화는 설치 뒤 Leader나 QA가 남겨요.
- 획 시간표(ms): 1획 0~391, 2획 561~937, 3획 1027~1304, 4획 1354~1405, 5획 1615~1989, 6획 2149~2348, 7획 2458~2755, 8획 2885~3000.

## 빠르기 고치는 곳
`feature/settings/.../SplashHandwriting.kt`
- `DRAW_MILLIS`(3000): 다 쓰는 데 걸리는 전체 시간
- `HOLD_MILLIS`(500): 다 쓴 뒤 멈춤
- `PACE`: 획마다 느린 정도(클수록 느림)
- `PAUSES`: 획 사이 쉼(ms)

값을 바꾼 뒤 `python docs/exports/tsk-206/make_preview.py .`로 GIF를 다시 만들 수 있어요.
