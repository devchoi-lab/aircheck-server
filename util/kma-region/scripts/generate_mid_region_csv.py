#!/usr/bin/env python3
"""
중기예보 예보구역코드 + 위경도 매핑 CSV 생성 스크립트

입력 (scripts/ 디렉토리):
  - mid-region-codes.csv : 예보구역코드, 구역명, 특성
      출처: 중기예보_중기기온예보구역코드_2025.12.xlsx (extract_source_csvs.py로 추출)
  - grid-latlon.csv      : 시도, 시군구, 읍면동, 위도, 경도
      출처: 기상청41_단기예보 조회서비스_오픈API활용가이드_격자_위경도(2610).xlsx

출력:
  - ../src/main/resources/mid-region-latlon.csv
    컬럼: regId, name, lat, lng, type
    type: A=육상(getMidLandFcst), C=기온(getMidTa)

실행:
  cd util/kma-region/scripts && python3 generate_mid_region_csv.py
  또는
  ./gradlew :util:kma-region:generateMidRegionCsv

매칭 전략 (우선순위 순):
  1. 수동 좌표 (MANUAL_COORDS)
  2. grid-latlon.csv 시군구 수준(읍면동 공백) 행에서 이름 정규화 후 정확 매칭
  3. sigungu가 name으로 시작하는 prefix 매칭 (수원→수원시장안구 등)
  4. sigungu에 name이 포함되는 contains 매칭
  미매칭 항목은 WARN 출력 → 수동 검수 필요

  동명이지 처리: 예보구역코드 앞 3자리(11B, 11C, ...)로 광역(시도) 후보군을 먼저 결정하여
  같은 이름의 다른 지역이 잘못 매칭되는 것을 방지.
"""

import csv
import re
import os

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))

MID_REGION_CSV = os.path.join(SCRIPT_DIR, 'mid-region-codes.csv')
GRID_LATLON_CSV = os.path.join(SCRIPT_DIR, 'grid-latlon.csv')
OUTPUT_CSV = os.path.normpath(os.path.join(SCRIPT_DIR, '../src/main/resources/mid-region-latlon.csv'))


# 예보구역코드 앞 3자리 → 시도 후보군
PREFIX_TO_SIDO = {
    '11A': ['인천광역시'],
    '11B': ['서울특별시', '인천광역시', '경기도'],
    '11C': ['충청북도', '충청남도', '대전광역시', '세종특별자치시'],
    '11D': ['강원특별자치도', '강원도'],
    '11E': ['경상북도'],
    '11F': ['전북특별자치도', '전라북도', '전라남도', '광주광역시', '전남광주통합특별시'],
    '11G': ['제주특별자치도'],
    '11H': ['경상북도', '경상남도', '대구광역시', '부산광역시', '울산광역시'],
    '11I': [],
    '11J': [],
    '11K': [],
    '11L': [],
    '21F': ['전라남도', '전북특별자치도', '전라북도', '전남광주통합특별시'],
}

# 수동 좌표 지정 (자동 매칭 불가 항목)
MANUAL_COORDS = {
    # 도서·특수 지점
    '11A00101': (37.9660000, 124.7086000),   # 백령도
    '11E00101': (37.4836000, 130.9057000),   # 울릉도
    '11E00102': (37.2432000, 131.8650000),   # 독도
    '11G00302': (33.3939000, 126.5292000),   # 성판악
    '11G00601': (32.1220000, 125.1827000),   # 이어도
    '11G00800': (33.9608000, 126.8333000),   # 추자도
    '11G00901': (33.4289000, 126.5811000),   # 산천단
    '11G01001': (33.3333000, 126.5500000),   # 한남
    '11F20701': (34.6833000, 125.4333000),   # 흑산도
    # 기상 관측 지점 (행정구역명과 다름)
    '11C10401': (36.2116333, 128.0008750),   # 추풍령 (충북 영동군 추풍령면)
    '11D20201': (37.6709222, 128.7084306),   # 대관령 (강원 평창군 대관령면)
    '11G00101': (33.4387500, 126.9129417),   # 성산 (제주 서귀포시 성산읍)
    '11G00501': (33.3474000, 126.1860194),   # 고산 (제주 제주시 한경면)
    # A타입 광역 대표 좌표
    '11B00000': (37.5636000, 126.9800000),   # 서울·인천·경기 → 서울
    '11C00000': (36.3471000, 127.3866000),   # 충청도 → 대전
    '11D10000': (37.8785000, 127.7323000),   # 강원영서 → 춘천
    '11D20000': (37.7491000, 128.8785000),   # 강원영동 → 강릉
    '11F00000': (35.8092000, 127.1219000),   # 전라도 → 전주
    '11F10000': (35.8092000, 127.1219000),   # 전북자치도 → 전주
    '11F20000': (35.1711000, 126.9141000),   # 전라남도 → 광주
    '11H00000': (35.8685000, 128.6036000),   # 경상도 → 대구
    '11H10000': (35.8685000, 128.6036000),   # 경상북도 → 대구
    '11H20000': (35.1770000, 129.0770000),   # 경상남도 → 부산
    '11BD0000': (37.5636000, 126.9800000),   # 서울경기·강원영서 (임시: 서울)
}


def normalize(name):
    """비교용 이름 정규화: 행정 접미사 제거"""
    return re.sub(
        r'(특별자치시|특별자치도|특별시|광역시|자치시|자치도|특별|도|시|군|구)$',
        '', name
    ).strip()


def build_grid_structures(grid_csv):
    """
    grid-latlon.csv → 두 가지 구조 구축
    exact_index: { normalize(sigungu): [(sido, lat, lng), ...] }  — 읍면동 공백 행
    all_rows: [(sido, sigungu, emd, lat, lng), ...]
    """
    exact_index = {}
    all_rows = []

    with open(grid_csv, encoding='utf-8') as f:
        reader = csv.DictReader(f)
        for row in reader:
            sido = row['sido']
            sigungu = row['sigungu']
            emd = row['eupmyeondong']
            try:
                lat = float(row['lat'])
                lng = float(row['lng'])
            except ValueError:
                continue

            all_rows.append((sido, sigungu, emd, lat, lng))

            # 시 수준 (sigungu 공백)
            if not sigungu and not emd and sido:
                key = normalize(sido)
                exact_index.setdefault(key, []).append((sido, lat, lng))

            # 시군구 수준 (읍면동 공백)
            if sigungu and not emd:
                key = normalize(sigungu)
                exact_index.setdefault(key, []).append((sido, lat, lng))

    return exact_index, all_rows


def find_latlon(reg_id, name, exact_index, all_rows):
    """위경도 검색 (3단계 우선순위 매칭)"""
    prefix = reg_id[:3]
    sido_candidates = PREFIX_TO_SIDO.get(prefix, [])

    # 1단계: 정규화 정확 매칭
    norm = normalize(name)
    candidates = exact_index.get(norm, [])
    if candidates:
        filtered = [(s, la, lo) for s, la, lo in candidates if not sido_candidates or s in sido_candidates]
        if filtered:
            return filtered[0][1], filtered[0][2]
        return candidates[0][1], candidates[0][2]

    # 2단계: sigungu가 name으로 시작하는 prefix 매칭
    for sido, sigungu, emd, lat, lng in all_rows:
        if not sigungu:
            continue
        if sido_candidates and sido not in sido_candidates:
            continue
        if sigungu.startswith(name) and len(sigungu) > len(name):
            return lat, lng

    # 3단계: sigungu에 name이 포함되는 contains 매칭
    for sido, sigungu, emd, lat, lng in all_rows:
        if not sigungu:
            continue
        if sido_candidates and sido not in sido_candidates:
            continue
        if name in sigungu:
            return lat, lng

    return None


def main():
    print('grid-latlon.csv 읽는 중...')
    exact_index, all_rows = build_grid_structures(GRID_LATLON_CSV)
    print(f'  정확 인덱스: {len(exact_index)}개 지명 / 전체 {len(all_rows)}개 행')

    print('mid-region-codes.csv 읽는 중...')
    with open(MID_REGION_CSV, encoding='utf-8') as f:
        mid_rows = list(csv.DictReader(f))
    print(f'  총 {len(mid_rows)}개 구역')

    output_rows = []
    unmatched = []

    for row in mid_rows:
        reg_id = row['regId']
        name = row['name']
        typ = row['type']

        if not reg_id:
            continue

        # 수동 좌표 우선
        if reg_id in MANUAL_COORDS:
            lat, lng = MANUAL_COORDS[reg_id]
            output_rows.append([reg_id, name, f'{lat:.7f}', f'{lng:.7f}', typ])
            continue

        # 북한 → 스킵
        if reg_id[:3] in ('11I', '11J', '11K', '11L'):
            continue

        # 해상(H타입), 외국(I타입) → 스킵
        if typ in ('H', 'I'):
            continue

        result = find_latlon(reg_id, name, exact_index, all_rows)
        if result:
            lat, lng = result
            output_rows.append([reg_id, name, f'{lat:.7f}', f'{lng:.7f}', typ])
        else:
            unmatched.append((reg_id, name, typ))
            output_rows.append([reg_id, name, 'UNMATCHED', 'UNMATCHED', typ])

    os.makedirs(os.path.dirname(OUTPUT_CSV), exist_ok=True)
    with open(OUTPUT_CSV, 'w', newline='', encoding='utf-8') as f:
        writer = csv.writer(f)
        writer.writerow(['regId', 'name', 'lat', 'lng', 'type'])
        writer.writerows(output_rows)

    print(f'\n출력: {OUTPUT_CSV}')
    print(f'총 {len(output_rows)}개 행 (북한·해상 제외)')

    if unmatched:
        print(f'\n[WARN] 미매칭 {len(unmatched)}개 — 수동 검수 필요:')
        for reg_id, name, typ in unmatched:
            print(f'  {reg_id}  {name}  ({typ})')
    else:
        print('\n미매칭 없음.')


if __name__ == '__main__':
    main()
