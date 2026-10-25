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

    @GetExchange("/getVilageFcst")
    fun getVilageFcst(
        @RequestParam("serviceKey") serviceKey: String,
        @RequestParam("pageNo") pageNo: Int,
        @RequestParam("numOfRows") numOfRows: Int,
        @RequestParam("dataType") dataType: String,
        @RequestParam("base_date") baseDate: String,
        @RequestParam("base_time") baseTime: String,
        /** 예보지점 X 좌표 (격자 좌표). 위경도 → 격자 변환: [KmaGridConverter.toGrid] */
        @RequestParam("nx") nx: Int,
        /** 예보지점 Y 좌표 (격자 좌표). 위경도 → 격자 변환: [KmaGridConverter.toGrid] */
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
