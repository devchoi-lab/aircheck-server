package com.seriouschoi.aircheck.core.kma

import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.service.annotation.GetExchange

interface KmaApi {

    /**
     * 초단기실황조회 — 예보 구역 대표 AWS 관측값 제공
     *
     * 발표 주기: 매시 정각, 10분 이후 호출 가능
     * 응답 항목: T1H(기온), RN1(1시간강수량), UUU(동서바람성분), VVV(남북바람성분),
     *           REH(습도), PTY(강수형태), VEC(풍향), WSD(풍속) — SKY(하늘상태) 없음
     */
    @GetExchange("/getUltraSrtNcst")
    fun getUltraSrtNcst(
        /** 공공데이터포털 발급 인증키 (URL Encode) */
        @RequestParam("serviceKey") serviceKey: String,
        /** 페이지 번호. Default: 1 */
        @RequestParam("pageNo") pageNo: Int,
        /** 한 페이지 결과 수. Default: 10 */
        @RequestParam("numOfRows") numOfRows: Int,
        /** 응답자료형식. XML 또는 JSON. Default: XML */
        @RequestParam("dataType") dataType: String,
        /** 발표일자 (yyyyMMdd). 예: 20210628 */
        @RequestParam("base_date") baseDate: String,
        /** 발표시각 (HHmm). 예: 0600. 매시각 10분 이후 호출 */
        @RequestParam("base_time") baseTime: String,
        /** 예보지점 X 좌표 (격자 좌표). 위경도 → 격자 변환 필요 */
        @RequestParam("nx") nx: Int,
        /** 예보지점 Y 좌표 (격자 좌표). 위경도 → 격자 변환 필요 */
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
        @RequestParam("nx") nx: Int,
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
