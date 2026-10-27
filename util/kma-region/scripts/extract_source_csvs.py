#!/usr/bin/env python3
"""
xlsx → 중간 CSV 추출 스크립트 (1회성 작업)

xlsx 파일이 업데이트될 때만 재실행 필요.
결과 CSV는 scripts/ 에 커밋.

출력:
  - mid-region-codes.csv  : 예보구역코드, 구역명, 특성
  - grid-latlon.csv       : 시도, 시군구, 읍면동, 위도, 경도
"""

import zipfile
import xml.etree.ElementTree as ET
import csv
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

MID_REGION_CSV = os.path.join(SCRIPT_DIR, 'mid-region-codes.csv')
GRID_LATLON_CSV = os.path.join(SCRIPT_DIR, 'grid-latlon.csv')


def col_letter_to_index(col_str):
    idx = 0
    for ch in col_str:
        idx = idx * 26 + (ord(ch.upper()) - ord('A') + 1)
    return idx - 1


def read_xlsx_rows(path):
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
                ref = c.get('r', '')
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
            result.append([cells.get(i, '') for i in range(max_col + 1)])
        return result


def extract_mid_region_codes():
    print('중기예보 구역코드 xlsx 읽는 중...')
    rows = read_xlsx_rows(MID_REGION_XLSX)
    with open(MID_REGION_CSV, 'w', newline='', encoding='utf-8') as f:
        writer = csv.writer(f)
        writer.writerow(['regId', 'name', 'type'])
        count = 0
        for row in rows[1:]:
            while len(row) < 3:
                row.append('')
            reg_id, name, typ = row[0], row[1], row[2]
            if reg_id:
                writer.writerow([reg_id, name, typ])
                count += 1
    print(f'  → {MID_REGION_CSV} ({count}개)')


def extract_grid_latlon():
    print('격자 위경도 xlsx 읽는 중...')
    rows = read_xlsx_rows(GRID_XLSX)
    # 헤더: 구분,행정구역코드,1단계(시도),2단계(시군구),3단계(읍면동),...,경도(초/100)[13],위도(초/100)[14]
    with open(GRID_LATLON_CSV, 'w', newline='', encoding='utf-8') as f:
        writer = csv.writer(f)
        writer.writerow(['sido', 'sigungu', 'eupmyeondong', 'lat', 'lng'])
        count = 0
        for row in rows[1:]:
            while len(row) < 15:
                row.append('')
            sido = row[2]
            sigungu = row[3]
            emd = row[4]
            lng_str = row[13]
            lat_str = row[14]
            if sido and lat_str and lng_str:
                try:
                    float(lat_str)
                    float(lng_str)
                    writer.writerow([sido, sigungu, emd, lat_str, lng_str])
                    count += 1
                except ValueError:
                    pass
    print(f'  → {GRID_LATLON_CSV} ({count}개)')


if __name__ == '__main__':
    extract_mid_region_codes()
    extract_grid_latlon()
    print('\n완료. 결과 CSV를 검수 후 커밋하세요.')
