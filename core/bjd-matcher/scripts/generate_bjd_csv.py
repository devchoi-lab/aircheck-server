#!/usr/bin/env python3
"""
법정동(法定洞) 위경도 매핑 CSV 생성 스크립트 (Kakao Geocoding API 기반)

입력 (scripts/ 디렉토리):
  - 국토교통부_전국 법정동_20260630.csv : 법정동 코드표 원본
      컬럼: 법정동코드, 시도명, 시군구명, 읍면동명, 리명, 순번, 생성일자

처리 규칙:
  - 읍면동명이 있는 항목만 Kakao API로 위경도 조회 (말단 단위)
  - 리 단위 중복 제거: 시도명+시군구명+읍면동명 기준 첫 번째만 유지
  - 도/시군구 단위 항목은 위경도 없이 포함 (법정동 코드 조회용)

출력:
  - ../src/main/resources/bjd-lat-lng.csv
    컬럼: code, doCode, doName, guCode, guName, dongCode, dongName, lat, lng

실행:
  cd core/bjd-matcher/scripts && python3 generate_bjd_csv.py <kakao_api_key>
  또는
  ./gradlew :core:bjd-matcher:generateBjdCsv

의존성: 표준 라이브러리만 사용
"""

import csv
import json
import os
import sys
import time
import urllib.parse
import urllib.request

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
BJD_INPUT_CSV = os.path.join(SCRIPT_DIR, '국토교통부_전국 법정동_20260630.csv')
RESOURCES_DIR = os.path.normpath(os.path.join(SCRIPT_DIR, '../src/main/resources'))
OUTPUT_CSV = os.path.join(RESOURCES_DIR, 'bjd-lat-lng.csv')

if len(sys.argv) < 2 or not sys.argv[1].strip():
    sys.exit('사용법: python3 generate_bjd_csv.py <kakao_api_key>\n'
             'Gradle: ./gradlew :core:bjd-matcher:generateBjdCsv  (local.properties에 kakao.api.key 필요)')
KAKAO_API_KEY = sys.argv[1].strip()


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


def derive_do_code(code):
    return code[:2] + '0' * 8


def derive_gu_code(code):
    return code[:5] + '0' * 5


def derive_dong_code(code):
    return code[:8] + '0' * 2


def main():
    print(f'입력: {BJD_INPUT_CSV}')
    with open(BJD_INPUT_CSV, encoding='utf-8-sig') as f:
        rows = list(csv.DictReader(f))
    print(f'총 {len(rows)}개 행')

    # 읍면동 단위 중복 제거 (리 단위 → 동 단위로 합침)
    seen_dong = set()
    deduped = []
    for row in rows:
        do_name = row['시도명'].strip()
        gu_name = row['시군구명'].strip()
        dong_name = row['읍면동명'].strip()
        ri_name = row['리명'].strip()

        if dong_name:
            key = f'{do_name}{gu_name}{dong_name}'
            if key in seen_dong:
                continue  # 리 단위 중복 스킵
            seen_dong.add(key)

        deduped.append(row)

    print(f'중복 제거 후 {len(deduped)}개 행')

    output_rows = []
    unmatched = []

    for row in deduped:
        code = row['법정동코드'].strip()
        do_name = row['시도명'].strip()
        gu_name = row['시군구명'].strip()
        dong_name = row['읍면동명'].strip()

        do_code = derive_do_code(code)
        gu_code = derive_gu_code(code) if gu_name else ''
        dong_code = derive_dong_code(code) if dong_name else ''

        if dong_name:
            # 읍면동 단위 — Kakao API로 위경도 조회
            query = f'{do_name} {gu_name} {dong_name}'.strip()
            lat, lng = geocode(query)
            time.sleep(0.05)

            if lat is not None:
                output_rows.append([code, do_code, do_name, gu_code, gu_name,
                                     dong_code, dong_name, f'{lat:.7f}', f'{lng:.7f}'])
            else:
                unmatched.append((code, do_name, gu_name, dong_name))
                output_rows.append([code, do_code, do_name, gu_code, gu_name,
                                     dong_code, dong_name, 'UNMATCHED', 'UNMATCHED'])
                print(f'  [WARN] {code}  {do_name} {gu_name} {dong_name}')
        else:
            # 도/시군구 단위 — 위경도 없이 포함
            output_rows.append([code, do_code, do_name, gu_code, gu_name,
                                 dong_code, dong_name, '', ''])

    os.makedirs(RESOURCES_DIR, exist_ok=True)
    with open(OUTPUT_CSV, 'w', newline='', encoding='utf-8') as f:
        writer = csv.writer(f)
        writer.writerow(['code', 'doCode', 'doName', 'guCode', 'guName',
                         'dongCode', 'dongName', 'lat', 'lng'])
        writer.writerows(output_rows)

    print(f'\n출력: {OUTPUT_CSV} ({len(output_rows)}개 행)')

    if unmatched:
        print(f'\n[WARN] 미매칭 {len(unmatched)}개:')
        for code, do_name, gu_name, dong_name in unmatched:
            print(f'  {code}  {do_name} {gu_name} {dong_name}')
    else:
        print('미매칭 없음.')


if __name__ == '__main__':
    main()
