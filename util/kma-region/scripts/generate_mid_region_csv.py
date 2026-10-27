#!/usr/bin/env python3
"""
중기예보 예보구역코드 + 위경도 매핑 CSV 생성 스크립트 (Kakao Geocoding API 기반)

입력 (scripts/ 디렉토리):
  - mid-region-codes.csv : regId, name, type, query
      query 컬럼: Kakao API 호출 쿼리 (상위 행정구역 포함, 동명이지 방지)
      원본 xlsx(중기예보_중기기온예보구역코드_2025.12.xlsx) 업데이트 시:
        1. xlsx → CSV 내보내기 (regId, name, type 컬럼)
        2. 기존 mid-region-codes.csv의 query 컬럼을 새 CSV에 병합
        3. 신규·변경 항목의 query는 수동으로 채울 것

조회 순서: MANUAL_COORDS → Kakao API
  MANUAL_COORDS에 regId가 있으면 해당 좌표를 사용 (Kakao API 호출 안 함).
  현재 이어도(11G00601)만 해당 — 수중암초라 Kakao 결과 신뢰 불가.

출력:
  - ../src/main/resources/mid-region-latlon.csv
    컬럼: regId, name, lat, lng, type
    type: A=육상(getMidLandFcst), C=기온(getMidTa)

실행:
  cd util/kma-region/scripts && python3 generate_mid_region_csv.py
  또는
  ./gradlew :util:kma-region:generateMidRegionCsv

의존성: 표준 라이브러리만 사용 (외부 패키지 불필요)
"""

import csv
import json
import os
import sys
import time
import urllib.parse
import urllib.request

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
MID_REGION_CSV = os.path.join(SCRIPT_DIR, 'mid-region-codes.csv')
OUTPUT_CSV = os.path.normpath(os.path.join(SCRIPT_DIR, '../src/main/resources/mid-region-latlon.csv'))

if len(sys.argv) < 2 or not sys.argv[1].strip():
    sys.exit('사용법: python3 generate_mid_region_csv.py <kakao_api_key>\n'
             'Gradle: ./gradlew :util:kma-region:generateMidRegionCsv  (local.properties에 kakao.api.key 필요)')
KAKAO_API_KEY = sys.argv[1].strip()

# Kakao API보다 우선 적용되는 수동 좌표 (Kakao 결과 신뢰 불가 항목)
MANUAL_COORDS = {
    '11G00601': (32.1220000, 125.1827000),   # 이어도 (수중암초 — 행정주소 없음)
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


def main():
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
        query = row.get('query', '').strip()

        if not reg_id:
            continue

        # 북한·해상·외국·특수 스킵
        if reg_id[:3] in ('11I', '11J', '11K', '11L'):
            continue
        if typ in ('H', 'I', '0'):
            continue

        # MANUAL_COORDS 우선 (Kakao API 호출 안 함)
        if reg_id in MANUAL_COORDS:
            lat, lng = MANUAL_COORDS[reg_id]
            output_rows.append([reg_id, name, f'{lat:.7f}', f'{lng:.7f}', typ])
            print(f'  [MANUAL] {reg_id}  {name}')
            continue

        # Kakao API 조회
        lat, lng = geocode(query)
        time.sleep(0.05)

        if lat is not None:
            output_rows.append([reg_id, name, f'{lat:.7f}', f'{lng:.7f}', typ])
        else:
            unmatched.append((reg_id, name, typ))
            output_rows.append([reg_id, name, 'UNMATCHED', 'UNMATCHED', typ])
            print(f'  [WARN]   {reg_id}  {name}  쿼리: "{query}"')

    os.makedirs(os.path.dirname(OUTPUT_CSV), exist_ok=True)
    with open(OUTPUT_CSV, 'w', newline='', encoding='utf-8') as f:
        writer = csv.writer(f)
        writer.writerow(['regId', 'name', 'lat', 'lng', 'type'])
        writer.writerows(output_rows)

    print(f'\n출력: {OUTPUT_CSV}')
    print(f'총 {len(output_rows)}개 행')

    if unmatched:
        print(f'\n[WARN] 미매칭 {len(unmatched)}개 — mid-region-codes.csv query 컬럼 수정 필요:')
        for reg_id, name, typ in unmatched:
            print(f'  {reg_id}  {name}  ({typ})')
    else:
        print('\n미매칭 없음.')


if __name__ == '__main__':
    main()
