# 알람음 파일 목록

원본은 `sounds/ringtones/`에 두고(제목 표는 `sounds/ringtones/_titles.md`), 앱에는 가볍게 줄여 `core/alarm/src/main/res/raw/`에 넣었어요(2026-11-18).
줄인 방법: `ffmpeg -i 원본.mp3 -t 30 -map_metadata -1 -c:a libvorbis -q:a 2 ringtone_id.ogg`. 한 곡 약 720KB(mp3 192kbps)에서 약 370KB(ogg 약 96kbps)로 줄었어요. ogg는 반복 재생 때 이음새 틈이 없어요.

| 원본 (`sounds/ringtones/`) | 앱 파일 (`res/raw/`) | 앱 안 id | S5 묶음 | 화면 이름 (원래 제목) |
|---|---|---|---|---|
| Soleil Mineur.mp3 | ringtone_soleil_mineur.ogg | `soleil_mineur` | 경쾌한 | 구름 낀 햇살 (Soleil Mineur) |
| Petit Bois.mp3 | ringtone_petit_bois.ogg | `petit_bois` | 잔잔한 | 아침 숲 산책 (Petit Bois) |
| Gouttes de Bois.mp3 | ringtone_gouttes_de_bois.ogg | `gouttes_de_bois` | 잔잔한 | 나뭇잎 이슬 (Gouttes de Bois) |
| Primavera.mp3 | ringtone_primavera.ogg | `primavera` | 잔잔한 | 봄날의 창가 (Primavera) |
| Fortissimo.mp3 | ringtone_fortissimo.ogg | `fortissimo` | 시끄러운 | 정신 번쩍 (Fortissimo) |
| Nuit Blanche.mp3 | ringtone_nuit_blanche.ogg | `nuit_blanche` | 시끄러운 | 벌떡 기상 (Nuit Blanche) |
| Tambour Battant.mp3 | ringtone_tambour_battant.ogg | `tambour_battant` | 시끄러운 | 북소리 행진 (Tambour Battant) |
| Neon Rouge.mp3 | ringtone_neon_rouge.ogg | `neon_rouge` | 시끄러운 | 빨간 경보 (Néon Rouge) |
| Presto.mp3 | ringtone_presto.ogg | `presto` | 경쾌한 | 바쁜 아침 (Presto) |
| Allegro Vivace.mp3 | ringtone_allegro_vivace.ogg | `allegro_vivace` | 경쾌한 | 가벼운 발걸음 (Allegro Vivace) |
| Joie de Vivre.mp3 | ringtone_joie_de_vivre.ogg | `joie_de_vivre` | 경쾌한 | 콧노래 (Joie de Vivre) |
| Bonjour Soleil.mp3 | ringtone_bonjour_soleil.ogg | `bonjour_soleil` | 경쾌한 | 햇살 인사 (Bonjour Soleil) |
| Buongiorno.mp3 | ringtone_buongiorno.ogg | `buongiorno` | 경쾌한 | 굿모닝 카페 (Buongiorno) |
| Route de Campagne.mp3 | ringtone_route_de_campagne.ogg | `route_de_campagne` | 경쾌한 | 시골길 드라이브 (Route de Campagne) |
| Joyeux Noel.mp3 | ringtone_joyeux_noel.ogg | `joyeux_noel` | 크리스마스 | 산타의 종소리 (Joyeux Noël) |
| Reverie.mp3 | ringtone_reverie.ogg | `reverie` | 신비로운 | 꿈결 (Rêverie) |
| Danse des Ombres.mp3 | ringtone_danse_des_ombres.ogg | `danse_des_ombres` | 신비로운 | 그림자 왈츠 (Danse des Ombres) |
| Maestoso.mp3 | ringtone_maestoso.ogg | `maestoso` | 웅장한 | 왕의 아침 (Maestoso) |
| Marche Triomphale.mp3 | ringtone_marche_triomphale.ogg | `marche_triomphale` | 웅장한 | 개선 행진 (Marche Triomphale) |
| Grandeur.mp3 | ringtone_grandeur.ogg | `grandeur` | 웅장한 | 장엄한 서막 (Grandeur) |
| Aube Paisible.mp3 | ringtone_aube_paisible.ogg | `aube_paisible` | 잔잔한 | 고요한 새벽빛 (Aube Paisible) |
| Douceur.mp3 | ringtone_douceur.ogg | `douceur` | 잔잔한 | 보드라운 아침 (Douceur) |
| Nuit de Noel.mp3 | ringtone_nuit_de_noel.ogg | `nuit_de_noel` | 크리스마스 | 눈 내리는 밤 (Nuit de Noël) |
| Petits Rebonds.mp3 | ringtone_petits_rebonds.ogg | `petits_rebonds` | 경쾌한 | 말랑 통통 (Petits Rebonds) |
| (원래 있던 기본음) | alarm_default.wav | `default` | 기본 | 기본 알람음 |

11-17에 넣었던 5곡(Morning, Morning Loop, Love Serenade, Spring Season, Good Morning)은 위 곡과 같은 곡이라 새 이름으로 합쳤어요. 옛 id로 저장된 알람은 `Ringtones.currentId`가 새 id로 바꿔 같은 곡으로 울려요.
사극 벨소리(공주마마, 폐하)는 아직 앱에 넣지 않았어요.
11-18 사용자 지시로 화면 이름을 한글로 바꿨어요. 원래 제목 뜻과 곡 묶음 느낌을 살려 아침 장면이 떠오르는 짧은 이름으로 지었어요. 파일 이름과 id는 그대로예요.

## 소리를 더하거나 바꾸는 법
1. 위 방법으로 줄여서 `core/alarm/src/main/res/raw/`에 `ringtone_이름.ogg`(영어 소문자, 숫자, 밑줄만)로 넣어요.
2. `core/model/.../Ringtones.kt`에 묶음과 화면 이름을 한 줄 더해요.
3. `core/alarm/.../ring/RingtoneFiles.kt`에 id와 파일을 한 줄 더해요. 빠뜨리면 테스트(`RingtoneFilesTest`)가 알려 줘요.

묶음은 곡 느낌으로 임시로 나눴어요. 나중에 태그 방식으로 바꿀 예정이에요(태그 기능은 아직 안 만듦).
