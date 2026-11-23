package com.seriouschoi.aircheck.core.airkorea.api

import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.service.annotation.GetExchange

interface AirKoreaApi {

    @GetExchange("/getMsrstnAcctoRltmMesureDnsty")
    fun getMeasurementData(
        @RequestParam("stationName") stationName: String,
        @RequestParam("serviceKey") serviceKey: String,
        @RequestParam("dataTerm") dataTerm: String,
        @RequestParam("pageNo") pageNo: Int,
        @RequestParam("numOfRows") numOfRows: Int,
        @RequestParam("returnType") returnType: String,
        @RequestParam("ver") ver: String
    ): String
}

interface StationApi {

    @GetExchange("/getMsrstnList")
    fun getStationList(
        @RequestParam("serviceKey") serviceKey: String,
        @RequestParam("pageNo") pageNo: Int,
        @RequestParam("numOfRows") numOfRows: Int,
        @RequestParam("returnType") returnType: String,
        @RequestParam("ver") ver: String
    ): String
}
