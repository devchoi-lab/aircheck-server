#!/usr/bin/env python3
"""
중기예보 예보구역코드 + 위경도 매핑 CSV 생성 스크립트

입력:
  - mid_region_xlsx: 중기예보_중기기온예보구역코드_2025.12.xlsx
  - grid_xlsx: 기상청41_단기예보 조회서비스_오픈API활용가이드_격자_위경도(2610).xlsx

출력:
  - ../src/main/resources/mid-region-latlon.csv
    컬럼: regId, name, lat, lng, type
    type: A=육상(getMidLandFcst), C=기온(getMidTa), H=해상(getMidSeaFcst)

실행:
  cd core/kma-region/scripts && python3 generate_mid_region_csv.py

매칭 전략 (우선순위 순):
  1. 수동 좌표 (MANUAL_COORDS)
  2. 격자 xlsx 시군구 수준(3단계 공백) 행에서 이름 정규화 후 정확 매칭
  3. 격자 xlsx 전 행에서 sigungu가 name으로 시작하는 prefix 매칭 (수원→수원시장안구 등)
  4. 격자 xlsx 전 행에서 sigungu에 name이 포함되는 contains 매칭
  미매칭 항목은 WARN 출력 → 수동 검수 필요
"""

import zipfile
import xml.etree.ElementTree as ET
import csv
import re
import os

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
WORKNOTE = os.path.normpath(os.path.join(SCRIPT_DIR, '../../../worknote'))

MID_REGION_XLSX = os.path.join(
    WORKNOTE,
    '기상청28_중기예보 조회서비스_오픈API활용가이드_251212',
    '중기예보_중기기온예보구역코드_2025.12.xlsx'
)
GRID_XLSX = os.path.join(
    WORKNOTE,
    '기상청41_단기예보 조회서비스_오픈API활용가이드_2610',
    '기상청41_단기예보 조회서비스_오픈API활용가이드_격자_위경도(2610).xlsx'
)
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
    '21F': ['전라남도', '전남광주통합특별시'],
}

# 수동 좌표 지정 (자동 매칭 불가 항목)
MANUAL_COORDS = {
    # 도서·특수 지점
    '11A00101': (37.9660, 124.7086),   # 백령도
    '11E00101': (37.4836, 130.9057),   # 울릉도
    '11E00102': (37.2432, 131.8650),   # 독도
    '11G00302': (33.3939, 126.5292),   # 성판악
    '11G00601': (32.1220, 125.1827),   # 이어도
    '11G00800': (33.9608, 126.8333),   # 추자도
    '11G00901': (33.4289, 126.5811),   # 산천단
    '11G01001': (33.3333, 126.5500),   # 한남
    '11F20701': (34.6833, 125.4333),   # 흑산도
    # 기상 관측 지점 (행정구역명과 다름)
    '11C10401': (36.2116333, 128.000875),    # 추풍령 (충북 영동군 추풍령면)
    '11D20201': (37.6709222, 128.7084306),   # 대관령 (강원 평창군 대관령면)
    '11G00101': (33.4387500, 126.9129417),   # 성산 (제주 서귀포시 성산읍)
    '11G00501': (33.3474000, 126.1860194),   # 고산 (제주 제주시 한경면)
    # A타입 광역 대표 좌표 (해당 광역 대표 도시 기준)
    '11B00000': (37.5636, 126.9800),   # 서울·인천·경기 → 서울
    '11C00000': (36.3471, 127.3866),   # 충청도 → 대전
    '11D10000': (37.8785, 127.7323),   # 강원영서 → 춘천
    '11D20000': (37.7491, 128.8785),   # 강원영동 → 강릉
    '11F00000': (35.8092, 127.1219),   # 전라도 → 전주
    '11F10000': (35.8092, 127.1219),   # 전북자치도 → 전주
    '11F20000': (35.1711, 126.9141),   # 전라남도 → 광주
    '11H00000': (35.8685, 128.6036),   # 경상도 → 대구
    '11H10000': (35.8685, 128.6036),   # 경상북도 → 대구
    '11H20000': (35.1770, 129.0770),   # 경상남도 → 부산
    # 기타 불명확 코드
    '11BD0000': (37.5636, 126.9800),   # 서울경기·강원영서 (임시: 서울)
}


def col_letter_to_index(col_str):
    """Excel 컬럼 문자(A, B, ..., AA, ...) → 0-based 인덱스"""
    idx = 0
    for ch in col_str:
        idx = idx * 26 + (ord(ch.upper()) - ord('A') + 1)
    return idx - 1


def read_xlsx_rows(path):
    """
    Excel XML에서 빈 셀은 생략되므로, 셀의 r(reference) 속성으로
    실제 컬럼 위치를 확인해 정확한 인덱스에 값을 넣는다.
    """
    with zipfile.ZipFile(path) as z:
        ss = ET.parse(z.open('xl/sharedStrings.xml')).getroot()
        ns = {'n': 'http://schemas.openxmlformats.org/spreadsheetml/2006/main'}
        strings = [
            si.find('.//n:t', ns).text or ''
            for si in ss.findall('n:si', ns)
        ]
        ws = ET.parse(z.open('xl/worksheets/sheet1.xml')).getroot()
        result = []
        for row in ws.findall('.//n:row', ns):
            cells = {}
            for c in row.findall('n:c', ns):
                ref = c.get('r', '')  # e.g. "A1", "C3"
                col_str = ''.join(ch for ch in ref if ch.isalpha())
                col_idx = col_letter_to_index(col_str) if col_str else 0
                t = c.get('t')
                v = c.find('n:v', ns)
                if v is None:
                    cells[col_idx] = ''
                elif t == 's':
                    cells[col_idx] = strings[int(v.text)]
                else:
                    cells[col_idx] = v.text
            max_col = max(cells.keys(), default=-1)
            row_list = [cells.get(i, '') for i in range(max_col + 1)]
            result.append(row_list)
        return result


def normalize(name):
    """비교용 이름 정규화: 행정 접미사 제거"""
    return re.sub(
        r'(특별자치시|특별자치도|특별시|광역시|자치시|자치도|특별|도|시|군|구)$',
        '', name
    ).strip()


def build_grid_structures(grid_rows):
    """
    격자 xlsx → 두 가지 구조 구축
    exact_index: { normalize(sigungu): [(sido, lat, lng), ...] }  — 3단계 공백 행
    all_rows: [(sido, sigungu, lat, lng), ...]  — 모든 행 (prefix/contains 매칭용)
    """
    exact_index = {}
    all_rows = []

    for row in grid_rows[1:]:
        while len(row) < 15:
            row.append('')
        sido = row[2]
        sigungu = row[3]
        emd = row[4]
        lng_str = row[13]
        lat_str = row[14]

        if not lat_str or not lng_str:
            continue
        try:
            lat = float(lat_str)
            lng = float(lng_str)
        except ValueError:
            continue

        all_rows.append((sido, sigungu, emd, lat, lng))

        # 시 수준 (sigungu 공백)
        if not sigungu and not emd and sido:
            key = normalize(sido)
            exact_index.setdefault(key, []).append((sido, lat, lng))

        # 시군구 수준 (3단계 공백)
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
        # sido 필터 없이 첫 번째
        return candidates[0][1], candidates[0][2]

    # 2단계: sigungu가 name으로 시작하는 prefix 매칭 (수원→수원시장안구 등)
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
    print('격자 xlsx 읽는 중...')
    grid_rows = read_xlsx_rows(GRID_XLSX)
    exact_index, all_rows = build_grid_structures(grid_rows)
    print(f'  정확 인덱스: {len(exact_index)}개 지명 / 전체 {len(all_rows)}개 행')

    print('중기예보 구역코드 xlsx 읽는 중...')
    mid_rows = read_xlsx_rows(MID_REGION_XLSX)
    print(f'  총 {len(mid_rows) - 1}개 구역')

    output_rows = []
    unmatched = []

    for row in mid_rows[1:]:
        while len(row) < 3:
            row.append('')
        reg_id, name, typ = row[0], row[1], row[2]
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
