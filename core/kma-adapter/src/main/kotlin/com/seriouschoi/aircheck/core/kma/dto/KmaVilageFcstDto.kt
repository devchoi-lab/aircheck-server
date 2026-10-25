package com.seriouschoi.aircheck.core.kma.dto

import kotlinx.serialization.Serializable

/**
 * 단기예보 아이템 — 예보지점별 시간대별 예보값 1건.
 *
 * API 응답은 category별로 item이 1개씩 반환됨.
 * category + fcstValue 쌍으로 사용.
 *
 * 단기예보 category 목록:
 * | category | 항목명        | 단위       | 비고                                          |
 * |----------|-------------|-----------|----------------------------------------------|
 * | TMP      | 1시간 기온    | ℃         | 실수                                          |
 * | TMN      | 일 최저기온   | ℃         | 실수. 해당 날짜 1회만 제공                        |
 * | TMX      | 일 최고기온   | ℃         | 실수. 해당 날짜 1회만 제공                        |
 * | SKY      | 하늘상태      | 코드       | 1:맑음 3:구름많음 4:흐림                         |
 * | PTY      | 강수형태      | 코드       | 0:없음 1:비 2:비/눈 3:눈 4:소나기               |
 * | POP      | 강수확률      | %         | 정수                                          |
 * | PCP      | 1시간 강수량  | 범주(mm)  | 문자열. "강수없음", "1mm 미만", "1.0mm" 등        |
 * | SNO      | 1시간 신적설  | 범주(cm)  | 문자열. "적설없음", "1cm 미만", "1.0cm" 등        |
 * | REH      | 습도         | %         | 정수                                          |
 * | WSD      | 풍속         | m/s       | 실수                                          |
 * | VEC      | 풍향         | deg       | 정수                                          |
 * | UUU      | 풍속(동서성분) | m/s      | 실수. 동(+), 서(-)                             |
 * | VVV      | 풍속(남북성분) | m/s      | 실수. 북(+), 남(-)                             |
 * | WAV      | 파고         | m         | 실수                                          |
 *
 * ※ PCP·SNO는 수치가 아닌 범주 문자열로 제공됨 — toDoubleOrNull() 불가.
 * ※ +900 이상/-900 이하 값은 Missing(관측 불가)으로 처리.
 */
@Serializable
data class FcstItem(
    /** 발표일자 (yyyyMMdd, KST). 예: "20210628" */
    val baseDate: String,
    /** 발표시각 (HHmm, KST). 단기예보 유효값: 0200·0500·0800·1100·1400·1700·2000·2300 */
    val baseTime: String,
    /**
     * 자료구분코드. 클래스 KDoc의 category 목록 참조.
     */
    val category: String,
    /** 예보일자 (yyyyMMdd, KST). 예: "20210628" */
    val fcstDate: String,
    /** 예보시각 (HHmm, KST). 예: "0600" */
    val fcstTime: String,
    /**
     * 예보값 (String). category에 따라 단위와 형식이 다름.
     * TMP·TMN·TMX·WSD·UUU·VVV·WAV → toDoubleOrNull()
     * SKY·PTY·POP·REH·VEC → toIntOrNull()
     * PCP·SNO → 범주 문자열 (수치 변환 불가)
     */
    val fcstValue: String,
    /** 예보지점 X 좌표 (격자 좌표). 위경도 → 격자 변환: [KmaGridConverter.toGrid] */
    val nx: Int,
    /** 예보지점 Y 좌표 (격자 좌표). 위경도 → 격자 변환: [KmaGridConverter.toGrid] */
    val ny: Int
)
