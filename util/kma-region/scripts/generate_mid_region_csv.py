#!/usr/bin/env python3
"""
중기예보 예보구역코드 + 위경도 매핑 CSV 생성 스크립트 (Kakao Geocoding API 기반)

입력 (scripts/ 디렉토리):
  - mid-region-codes.csv : 예보구역코드, 구역명, 특성
      출처: 중기예보_중기기온예보구역코드_2025.12.xlsx (extract_source_csvs.py로 추출)
  - grid-latlon.csv      : 시도, 시군구, 읍면동, nx, ny, 위도, 경도
      출처: 기상청41_단기예보 조회서비스_오픈API활용가이드_격자_위경도(2610).xlsx
      용도: nx/ny 격자 좌표 조회 전용 (lat/lng는 Kakao API로 취득)

출력:
  - ../src/main/resources/mid-region-latlon.csv
    컬럼: regId, name, lat, lng, nx, ny, type
    type: A=육상(getMidLandFcst), C=기온(getMidTa)

실행:
  cd util/kma-region/scripts && python3 generate_mid_region_csv.py
  또는
  ./gradlew :util:kma-region:generateMidRegionCsv

의존성: 표준 라이브러리만 사용 (외부 패키지 불필요)

검색 실패 시 [WARN] 출력 → MANUAL_COORDS에 수동 추가 후 재실행
"""

import csv
import json
import os
import time
import urllib.parse
import urllib.request

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
MID_REGION_CSV = os.path.join(SCRIPT_DIR, 'mid-region-codes.csv')
GRID_LATLON_CSV = os.path.join(SCRIPT_DIR, 'grid-latlon.csv')
OUTPUT_CSV = os.path.normpath(os.path.join(SCRIPT_DIR, '../src/main/resources/mid-region-latlon.csv'))

KAKAO_API_KEY = '366992c357dd822f4c7ee547e6aaa5d7'

# 예보구역코드 앞 4자리 → Kakao 검색 시도 힌트 (동명이지 방지)
# 예: "광주"(11F2) → "전라남도 광주"로 검색하여 경기도 광주시 오매칭 방지
PREFIX_TO_SIDO_HINT = {
    '11A0': '인천광역시',    # 서해5도
    '11D1': '강원',          # 강원영서
    '11D2': '강원',          # 강원영동
    '11F1': '전북',          # 전북자치도
    '11F2': '전라남도',      # 전남 — 광주 동명이지 방지
    '11G0': '제주',          # 제주
    '11H1': '경상북도',      # 경북
    '11H2': '경상남도',      # 경남
    '21F2': '전라남도',      # 전남 도서
}

# Kakao API로 조회 불가한 항목 수동 좌표
#   - A타입 광역 구역명: "서울.인천.경기", "강원영서" 등 행정구역명이 아닌 기상 구역명
#   - 이어도: 수중암초 (행정구역 없음)
MANUAL_COORDS = {
    '11B00000': (37.5636000, 126.9800000),   # 서울·인천·경기 → 서울
    '11C00000': (36.3471000, 127.3866000),   # 충청도 → 대전
    '11C10000': (36.6372000, 127.4897000),   # 충청북도 → 청주
    '11C20000': (36.3471000, 127.3866000),   # 충청남도 → 대전
    '11D10000': (37.8785000, 127.7323000),   # 강원영서 → 춘천
    '11D20000': (37.7491000, 128.8785000),   # 강원영동 → 강릉
    '11F00000': (35.8092000, 127.1219000),   # 전라도 → 전주
    '11F10000': (35.8092000, 127.1219000),   # 전북자치도 → 전주
    '11F20000': (35.1711000, 126.9141000),   # 전라남도 → 광주
    '11G00000': (33.4996000, 126.5312000),   # 제주도 → 제주시
    '11H00000': (35.8685000, 128.6036000),   # 경상도 → 대구
    '11H10000': (35.8685000, 128.6036000),   # 경상북도 → 대구
    '11H20000': (35.1770000, 129.0770000),   # 경상남도 → 부산
    '11G00601': (32.1220000, 125.1827000),   # 이어도 (수중암초)
}


def _kakao_get(endpoint, query):
    """Kakao Local API GET 요청 → documents 리스트. 실패 시 []."""
    params = urllib.parse.urlencode({'query': query, 'size': 1})
    url = f'https://dapi.kakao.com/v2/local/{endpoint}?{params}'
    req = urllib.request.Request(url, headers={'Authorization': f'KakaoAK {KAKAO_API_KEY}'})
    with urllib.request.urlopen(req, timeout=10) as resp:
        return json.loads(resp.read()).get('documents', [])


def geocode(query):
    """
    Kakao API로 쿼리 → (lat, lng).
    1차: 주소 검색(search/address), 2차: 키워드 검색(search/keyword).
    실패 시 (None, None).
    """
    try:
        docs = _kakao_get('search/address.json', query)
        if docs:
            return float(docs[0]['y']), float(docs[0]['x'])
        time.sleep(0.05)
        docs = _kakao_get('search/keyword.json', query)
        if docs:
            return float(docs[0]['y']), float(docs[0]['x'])
    except Exception as e:
        print(f'  [ERROR] {query}: {e}')
    return None, None


def load_grid_rows(grid_csv):
    """grid-latlon.csv → [(lat, lng, nx, ny), ...] nx/ny 있는 행만."""
    rows = []
    with open(grid_csv, encoding='utf-8') as f:
        for row in csv.DictReader(f):
            try:
                lat, lng = float(row['lat']), float(row['lng'])
                nx, ny = row.get('nx', ''), row.get('ny', '')
                if nx and ny:
                    rows.append((lat, lng, nx, ny))
            except ValueError:
                continue
    return rows


def nearest_grid(lat, lng, grid_rows):
    """유클리드 최근접 격자(nx, ny) 반환."""
    best = min(grid_rows, key=lambda r: (r[0] - lat) ** 2 + (r[1] - lng) ** 2)
    return best[2], best[3]


def main():
    print('grid-latlon.csv 읽는 중...')
    grid_rows = load_grid_rows(GRID_LATLON_CSV)
    print(f'  {len(grid_rows)}개 격자 로드')

    print('mid-region-codes.csv 읽는 중...')
    with open(MID_REGION_CSV, encoding='utf-8') as f:
        mid_rows = list(csv.DictReader(f))
    print(f'  총 {len(mid_rows)}개 구역\n')

    output_rows = []
    unmatched = []

    for row in mid_rows:
        reg_id = row['regId']
        name = row['name']
        typ = row['type']

        if not reg_id:
            continue

        # 북한·해상·외국·특수 스킵
        if reg_id[:3] in ('11I', '11J', '11K', '11L'):
            continue
        if typ in ('H', 'I', '0'):
            continue

        # 수동 좌표 우선
        if reg_id in MANUAL_COORDS:
            lat, lng = MANUAL_COORDS[reg_id]
            nx, ny = nearest_grid(lat, lng, grid_rows)
            output_rows.append([reg_id, name, f'{lat:.7f}', f'{lng:.7f}', nx, ny, typ])
            print(f'  [MANUAL] {reg_id}  {name}')
            continue

        # Kakao API 조회
        hint = PREFIX_TO_SIDO_HINT.get(reg_id[:4], '')
        query = f'{hint} {name}'.strip() if hint else name
        lat, lng = geocode(query)
        time.sleep(0.05)

        if lat is not None:
            nx, ny = nearest_grid(lat, lng, grid_rows)
            output_rows.append([reg_id, name, f'{lat:.7f}', f'{lng:.7f}', nx, ny, typ])
        else:
            unmatched.append((reg_id, name, typ, query))
            output_rows.append([reg_id, name, 'UNMATCHED', 'UNMATCHED', '', '', typ])
            print(f'  [WARN]   {reg_id}  {name}  쿼리: "{query}"')

    os.makedirs(os.path.dirname(OUTPUT_CSV), exist_ok=True)
    with open(OUTPUT_CSV, 'w', newline='', encoding='utf-8') as f:
        writer = csv.writer(f)
        writer.writerow(['regId', 'name', 'lat', 'lng', 'nx', 'ny', 'type'])
        writer.writerows(output_rows)

    print(f'\n출력: {OUTPUT_CSV}')
    print(f'총 {len(output_rows)}개 행')

    if unmatched:
        print(f'\n[WARN] 미매칭 {len(unmatched)}개 — MANUAL_COORDS에 수동 추가 필요:')
        for reg_id, name, typ, query in unmatched:
            print(f'  {reg_id}  {name}  ({typ})  쿼리: "{query}"')
    else:
        print('\n미매칭 없음.')


if __name__ == '__main__':
    main()
