#!/usr/bin/env python3
"""
중기예보 예보구역코드 + 위경도 매핑 CSV 생성 스크립트 (Kakao Geocoding API 기반)

입력 (scripts/ 디렉토리):
  - mid-land-region-codes.csv : regId, name, query  — getMidLandFcst용 (남한 10개 광역)
      출처: 기상청28_중기예보 조회서비스_오픈API활용가이드_241128 "중기육상예보구역 코드 정보 표"
  - mid-region-codes.csv : regId, name, type, query  — getMidTa용 (C타입만 처리)
      출처: 중기예보_중기기온예보구역코드_2025.12.xlsx
      xlsx 업데이트 시:
        1. xlsx → CSV 내보내기 (regId, name, type 컬럼)
        2. 기존 mid-region-codes.csv의 query 컬럼을 새 CSV에 병합
        3. 신규·변경 항목의 query는 수동으로 채울 것

조회 순서: MANUAL_COORDS → Kakao API
  MANUAL_COORDS에 regId가 있으면 해당 좌표를 사용 (Kakao API 호출 안 함).
  현재 이어도(11G00601)만 해당 — 수중암초라 Kakao 결과 신뢰 불가.

출력:
  - ../src/main/resources/mid-land-region-latlon.csv
    컬럼: regId, name, lat, lng  (getMidLandFcst용)
  - ../src/main/resources/mid-region-latlon.csv
    컬럼: regId, name, lat, lng  (getMidTa용, C타입)

실행:
  cd util/kma-region/scripts && python3 generate_mid_region_csv.py <kakao_api_key>
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
MID_LAND_REGION_CSV = os.path.join(SCRIPT_DIR, 'mid-land-region-codes.csv')
MID_REGION_CSV = os.path.join(SCRIPT_DIR, 'mid-region-codes.csv')
RESOURCES_DIR = os.path.normpath(os.path.join(SCRIPT_DIR, '../src/main/resources'))
LAND_OUTPUT_CSV = os.path.join(RESOURCES_DIR, 'mid-land-region-latlon.csv')
TA_OUTPUT_CSV = os.path.join(RESOURCES_DIR, 'mid-region-latlon.csv')

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


def process(rows, has_type=False, type_filter=None):
    """
    rows: csv.DictReader rows
    has_type: row에 'type' 컬럼이 있는지
    type_filter: 처리할 type 값 ('C' 등). None이면 모두 처리.
    반환: (output_rows, unmatched)
      output_rows: [regId, name, lat, lng] 리스트
    """
    output_rows = []
    unmatched = []

    for row in rows:
        reg_id = row['regId']
        name = row['name']
        query = row.get('query', '').strip()
        typ = row.get('type', '') if has_type else None

        if not reg_id:
            continue

        # 북한 스킵
        if reg_id[:3] in ('11I', '11J', '11K', '11L'):
            continue

        # type 필터
        if type_filter and typ != type_filter:
            continue

        # 해상·외국·특수 스킵
        if typ in ('H', 'I', '0'):
            continue

        # MANUAL_COORDS 우선
        if reg_id in MANUAL_COORDS:
            lat, lng = MANUAL_COORDS[reg_id]
            output_rows.append([reg_id, name, f'{lat:.7f}', f'{lng:.7f}'])
            print(f'  [MANUAL] {reg_id}  {name}')
            continue

        # Kakao API 조회
        lat, lng = geocode(query)
        time.sleep(0.05)

        if lat is not None:
            output_rows.append([reg_id, name, f'{lat:.7f}', f'{lng:.7f}'])
        else:
            unmatched.append((reg_id, name, query))
            output_rows.append([reg_id, name, 'UNMATCHED', 'UNMATCHED'])
            print(f'  [WARN]   {reg_id}  {name}  쿼리: "{query}"')

    return output_rows, unmatched


def write_csv(path, rows):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', newline='', encoding='utf-8') as f:
        writer = csv.writer(f)
        writer.writerow(['regId', 'name', 'lat', 'lng'])
        writer.writerows(rows)


def main():
    all_unmatched = []

    # getMidLandFcst용 (남한 10개 광역)
    print('=== mid-land-region-codes.csv (getMidLandFcst용) ===')
    with open(MID_LAND_REGION_CSV, encoding='utf-8') as f:
        land_rows = list(csv.DictReader(f))
    print(f'  총 {len(land_rows)}개 구역')
    land_output, land_unmatched = process(land_rows, has_type=False)
    write_csv(LAND_OUTPUT_CSV, land_output)
    print(f'  출력: {LAND_OUTPUT_CSV} ({len(land_output)}개 행)')
    all_unmatched += [('land', *u) for u in land_unmatched]

    print()

    # getMidTa용 (C타입)
    print('=== mid-region-codes.csv (getMidTa용, C타입) ===')
    with open(MID_REGION_CSV, encoding='utf-8') as f:
        ta_rows = list(csv.DictReader(f))
    print(f'  총 {len(ta_rows)}개 구역 (C타입만 처리)')
    ta_output, ta_unmatched = process(ta_rows, has_type=True, type_filter='C')
    write_csv(TA_OUTPUT_CSV, ta_output)
    print(f'  출력: {TA_OUTPUT_CSV} ({len(ta_output)}개 행)')
    all_unmatched += [('ta', *u) for u in ta_unmatched]

    if all_unmatched:
        print(f'\n[WARN] 미매칭 {len(all_unmatched)}개 — query 컬럼 수정 필요:')
        for src, reg_id, name, query in all_unmatched:
            print(f'  [{src}] {reg_id}  {name}  쿼리: "{query}"')
    else:
        print('\n미매칭 없음.')


if __name__ == '__main__':
    main()
