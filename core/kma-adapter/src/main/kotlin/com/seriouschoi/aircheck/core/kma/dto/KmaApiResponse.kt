package com.seriouschoi.aircheck.core.kma.dto

import kotlinx.serialization.Serializable

/**
 * 기상청 API 공통 응답 구조
 */
@Serializable
data class KmaApiResponse<T>(
    val response: KmaResponse<T>
)

@Serializable
data class KmaResponse<T>(
    val header: KmaHeader,
    val body: KmaBody<T>? = null
)

@Serializable
data class KmaHeader(
    val resultCode: String,
    val resultMsg: String
)

@Serializable
data class KmaBody<T>(
    val dataType: String? = null,
    val items: KmaItems<T>? = null,
    val pageNo: Int? = null,
    val numOfRows: Int? = null,
    val totalCount: Int? = null
)

@Serializable
data class KmaItems<T>(
    val item: List<T>
)

/**
 * 초단기예보/단기예보 아이템
 */
@Serializable
data class FcstItem(
    val baseDate: String,      // 발표일자
    val baseTime: String,      // 발표시각
    val category: String,      // 자료구분코드
    val fcstDate: String,      // 예보일자
    val fcstTime: String,      // 예보시각
    val fcstValue: String,     // 예보값
    val nx: Int,               // 격자 X
    val ny: Int                // 격자 Y
)

/**
 * 초단기실황 아이템 — 예보 구역 대표 AWS(자동기상관측장비) 관측값 1건.
 *
 * API 응답은 category별로 item이 1개씩 반환됨 (totalCount=8 고정).
 * category + obsrValue 쌍으로 사용.
 *
 * 초단기실황 category 목록:
 * | category | 항목명       | 단위  | 비고                                                              |
 * |----------|------------|-------|------------------------------------------------------------------|
 * | T1H      | 기온         | ℃    | 실수                                                              |
 * | RN1      | 1시간 강수량  | mm   | 실수. 강수 없으면 "0"                                               |
 * | UUU      | 동서바람성분  | m/s  | 실수. 동(+), 서(-)                                                 |
 * | VVV      | 남북바람성분  | m/s  | 실수. 북(+), 남(-)                                                 |
 * | REH      | 습도         | %    | 정수                                                              |
 * | PTY      | 강수형태      | 코드  | 0:없음 1:비 2:비/눈 3:눈 4:소나기 5:빗방울 6:빗방울눈날림 7:눈날림   |
 * | VEC      | 풍향         | deg  | 정수                                                              |
 * | WSD      | 풍속         | m/s  | 실수                                                              |
 *
 * ※ SKY(하늘상태)는 초단기실황 항목에 없음 — 초단기예보에서만 제공.
 * ※ +900 이상/-900 이하 값은 Missing(관측 불가)으로 처리.
 */
@Serializable
data class NcstItem(
    /** 발표일자 (yyyyMMdd, KST). 예: "20210628" */
    val baseDate: String,
    /**
     * 발표시각 (HHmm, KST). 예: "0600".
     * 매시간 정시에 10분 이후 생성, 10분마다 최신값으로 갱신.
     * "현재" 관측값을 얻으려면 호출 시각 기준 가장 최근 정시를 계산해 요청해야 함.
     * (예: 13:25 호출 → base_time=1300)
     */
    val baseTime: String,
    /**
     * 자료구분코드. T1H / RN1 / UUU / VVV / REH / PTY / VEC / WSD 중 하나.
     * 코드별 단위 및 비고는 클래스 KDoc 참조.
     */
    val category: String,
    /**
     * 실황값 (String). category에 따라 단위와 형식이 다름.
     * 파싱: T1H·RN1·UUU·VVV·WSD → toDoubleOrNull(), REH·VEC·PTY → toIntOrNull().
     * 클래스 KDoc 참조.
     */
    val obsrValue: String,
    /** 예보지점 X 좌표 (격자 좌표). 위경도 → 격자 변환 필요 */
    val nx: Int,
    /** 예보지점 Y 좌표 (격자 좌표). 위경도 → 격자 변환 필요 */
    val ny: Int
)

/**
 * 중기예보 아이템 (육상)
 */
@Serializable
data class MidLandFcstItem(
    val regId: String,         // 예보구역코드
    val rnSt3Am: Int? = null,  // 3일 후 오전 강수확률
    val rnSt3Pm: Int? = null,  // 3일 후 오후 강수확률
    val rnSt4Am: Int? = null,
    val rnSt4Pm: Int? = null,
    val rnSt5Am: Int? = null,
    val rnSt5Pm: Int? = null,
    val rnSt6Am: Int? = null,
    val rnSt6Pm: Int? = null,
    val rnSt7Am: Int? = null,
    val rnSt7Pm: Int? = null,
    val rnSt8: Int? = null,
    val rnSt9: Int? = null,
    val rnSt10: Int? = null,
    val wf3Am: String? = null, // 3일 후 오전 날씨
    val wf3Pm: String? = null,
    val wf4Am: String? = null,
    val wf4Pm: String? = null,
    val wf5Am: String? = null,
    val wf5Pm: String? = null,
    val wf6Am: String? = null,
    val wf6Pm: String? = null,
    val wf7Am: String? = null,
    val wf7Pm: String? = null,
    val wf8: String? = null,
    val wf9: String? = null,
    val wf10: String? = null
)

/**
 * 중기기온예보 아이템
 */
@Serializable
data class MidTaItem(
    val regId: String,         // 예보구역코드
    val taMin3: Int? = null,   // 3일 후 최저기온
    val taMax3: Int? = null,   // 3일 후 최고기온
    val taMin4: Int? = null,
    val taMax4: Int? = null,
    val taMin5: Int? = null,
    val taMax5: Int? = null,
    val taMin6: Int? = null,
    val taMax6: Int? = null,
    val taMin7: Int? = null,
    val taMax7: Int? = null,
    val taMin8: Int? = null,
    val taMax8: Int? = null,
    val taMin9: Int? = null,
    val taMax9: Int? = null,
    val taMin10: Int? = null,
    val taMax10: Int? = null
)
