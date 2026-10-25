package com.seriouschoi.aircheck.core.kma

import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.service.annotation.GetExchange

interface KmaApi {

    /**
     * 초단기실황조회 — 예보 구역 대표 AWS 관측값 제공
     *
     * 발표 주기: 매시 정각에 생성, 10분 이후 호출 가능. 이후 10분마다 최신값으로 갱신.
     * 응답 항목 (totalCount=8 고정):
     *   T1H(기온 ℃), RN1(1시간강수량 mm), UUU(동서바람 m/s), VVV(남북바람 m/s),
     *   REH(습도 %), PTY(강수형태 코드), VEC(풍향 deg), WSD(풍속 m/s)
     *   — SKY(하늘상태) 없음 (초단기예보에서만 제공)
     */
    @GetExchange("/getUltraSrtNcst")
    fun getUltraSrtNcst(
        /** 공공데이터포털 발급 인증키 (URL Encode) */
        @RequestParam("serviceKey") serviceKey: String,
        /** 페이지 번호. Default: 1 */
        @RequestParam("pageNo") pageNo: Int,
        /** 한 페이지 결과 수. totalCount=8 고정이므로 10으로 충분. Default: 10 */
        @RequestParam("numOfRows") numOfRows: Int,
        /** 응답자료형식. XML 또는 JSON. Default: XML */
        @RequestParam("dataType") dataType: String,
        /**
         * 발표일자 (yyyyMMdd, KST). 예: 20210628
         * 현재 날씨를 가져오려면 호출 시각 기준 가장 최근 정시의 날짜를 넣어야 함.
         */
        @RequestParam("base_date") baseDate: String,
        /**
         * 발표시각 (HHmm, KST). 예: 0600
         * 매시각 정시에 생성되며, 10분 이후부터 호출 가능.
         * 현재 날씨를 가져오려면 호출 시각 기준 가장 최근 정시를 넣어야 함.
         * 예: 13:25 호출 → 1300 (13:10 이후 유효), 13:05 호출 → 1200 (아직 1300 미생성)
         */
        @RequestParam("base_time") baseTime: String,
        /**
         * 예보지점 X 좌표 (격자 좌표).
         * 위경도 → 격자 변환: [KmaGridConverter.toGrid]
         * 변환 공식: Lambert Conformal Conic Projection (기상청 5km 격자)
         */
        @RequestParam("nx") nx: Int,
        /**
         * 예보지점 Y 좌표 (격자 좌표).
         * 위경도 → 격자 변환: [KmaGridConverter.toGrid]
         */
        @RequestParam("ny") ny: Int
    ): String

    /**
     * 단기예보조회 — 최대 +5일 예보 (근접 기간 1시간 간격, 연장 기간 3시간 간격)
     *
     * 발표 주기: 하루 8회 — 0200, 0500, 0800, 1100, 1400, 1700, 2000, 2300 (각 발표 후 10분 이후 호출 가능)
     * 예보 범위:
     *   - 02·05·08·11·14시 발표: +4일(글피)까지 (1시간 간격 정량값 + 연장기간은 3시간 간격 정성정보)
     *   - 17·20·23시 발표: +5일(그글피)까지
     * 응답 항목: TMP(기온), TMN(최저기온), TMX(최고기온), SKY(하늘상태), PTY(강수형태),
     *            POP(강수확률), PCP(1시간강수량), SNO(1시간신적설), WSD(풍속), REH(습도)
     */
    @GetExchange("/getVilageFcst")
    fun getVilageFcst(
        /** 공공데이터포털 발급 인증키 (URL Encode) */
        @RequestParam("serviceKey") serviceKey: String,
        /** 페이지 번호. Default: 1 */
        @RequestParam("pageNo") pageNo: Int,
        /**
         * 한 페이지 결과 수. Default: 10
         * 단기예보는 totalCount가 최대 742건 이상이므로 충분히 크게 설정할 것.
         */
        @RequestParam("numOfRows") numOfRows: Int,
        /** 응답자료형식. XML 또는 JSON. Default: XML */
        @RequestParam("dataType") dataType: String,
        /**
         * 발표일자 (yyyyMMdd, KST). 예: 20210628
         * 현재 예보를 가져오려면 호출 시각 기준 가장 최근 발표 시각의 날짜를 넣어야 함.
         */
        @RequestParam("base_date") baseDate: String,
        /**
         * 발표시각 (HHmm, KST). 하루 8회 고정: 0200·0500·0800·1100·1400·1700·2000·2300
         * 각 발표 시각 10분 이후(예: 0210, 0510...)부터 호출 가능.
         * 예: 13:25 호출 → 1100 (14:10 이전이므로 1400 미생성)
         */
        @RequestParam("base_time") baseTime: String,
        /**
         * 예보지점 X 좌표 (격자 좌표).
         * 위경도 → 격자 변환: [KmaGridConverter.toGrid]
         * 변환 공식: Lambert Conformal Conic Projection (기상청 5km 격자)
         */
        @RequestParam("nx") nx: Int,
        /**
         * 예보지점 Y 좌표 (격자 좌표).
         * 위경도 → 격자 변환: [KmaGridConverter.toGrid]
         */
        @RequestParam("ny") ny: Int
    ): String
}

interface KmaMidApi {

    @GetExchange("/getMidLandFcst")
    fun getMidLandFcst(
        @RequestParam("serviceKey") serviceKey: String,
        @RequestParam("pageNo") pageNo: Int,
        @RequestParam("numOfRows") numOfRows: Int,
        @RequestParam("dataType") dataType: String,
        @RequestParam("regId") regId: String,
        @RequestParam("tmFc") tmFc: String
    ): String

    @GetExchange("/getMidTa")
    fun getMidTa(
        @RequestParam("serviceKey") serviceKey: String,
        @RequestParam("pageNo") pageNo: Int,
        @RequestParam("numOfRows") numOfRows: Int,
        @RequestParam("dataType") dataType: String,
        @RequestParam("regId") regId: String,
        @RequestParam("tmFc") tmFc: String
    ): String
}
