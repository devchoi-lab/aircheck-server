package com.seriouschoi.aircheck.core.kma

import com.seriouschoi.aircheck.core.domain.model.MidLandForecast
import com.seriouschoi.aircheck.core.domain.model.MidLandForecastDay
import com.seriouschoi.aircheck.core.domain.port.ApiUsagePort
import com.seriouschoi.aircheck.core.kma.dto.KmaApiResponse
import com.seriouschoi.aircheck.core.kma.dto.MidLandFcstItem
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component

/**
 * 중기육상예보 조회 캐시 빈.
 *
 * 캐시 키: doCode — 법정동 도 코드 기준이므로 lat/lng 정규화 없이도 캐시 효율이 높음.
 */
@Component
@ConditionalOnProperty(name = ["weather.provider"], havingValue = "kma")
class KmaMidLandForecastCache(
    @Value("\${kma.api.key:}") private val apiKey: String,
    private val kmaMidApi: KmaMidApi,
    private val apiUsagePort: ApiUsagePort?
) {
    @Cacheable("kma-mid-land-forecast", key = "#sigunguCode")
    fun fetch(doCode: Long, sigunguCode: Long): MidLandForecast {
        check(apiKey.isNotBlank()) { "KMA API key not configured" }
        val regId = doCodeToLandRegId(doCode, sigunguCode)
        return runWithUsage("KMA_MID_LAND_FCST", apiUsagePort) {
            val response = kmaMidApi.getMidLandFcst(
                serviceKey = apiKey, pageNo = 1, numOfRows = 10, dataType = "JSON",
                regId = regId, tmFc = calculateMidFcstTmFc()
            )
            check(response.isNotEmpty()) { "KMA mid-land fcst: empty response" }
            val parsed = forecastJson.decodeFromString<KmaApiResponse<MidLandFcstItem>>(response)
            check(parsed.response.header.resultCode == "00") {
                "KMA mid-land fcst error: ${parsed.response.header.resultMsg}"
            }
            val item = parsed.response.body?.items?.item?.firstOrNull()
                ?: error("KMA mid-land fcst: no data")
            parseMidLandForecast(item)
        }
    }

    private fun parseMidLandForecast(item: MidLandFcstItem): MidLandForecast {
        val days = (3..10).map { offset ->
            when (offset) {
                3  -> MidLandForecastDay(offset, item.rnSt3Am ?: 0, item.rnSt3Pm ?: 0, item.wf3Am ?: "", item.wf3Pm ?: "")
                4  -> MidLandForecastDay(offset, item.rnSt4Am ?: 0, item.rnSt4Pm ?: 0, item.wf4Am ?: "", item.wf4Pm ?: "")
                5  -> MidLandForecastDay(offset, item.rnSt5Am ?: 0, item.rnSt5Pm ?: 0, item.wf5Am ?: "", item.wf5Pm ?: "")
                6  -> MidLandForecastDay(offset, item.rnSt6Am ?: 0, item.rnSt6Pm ?: 0, item.wf6Am ?: "", item.wf6Pm ?: "")
                7  -> MidLandForecastDay(offset, item.rnSt7Am ?: 0, item.rnSt7Pm ?: 0, item.wf7Am ?: "", item.wf7Pm ?: "")
                8  -> MidLandForecastDay(offset, item.rnSt8  ?: 0, item.rnSt8  ?: 0, item.wf8  ?: "", item.wf8  ?: "")
                9  -> MidLandForecastDay(offset, item.rnSt9  ?: 0, item.rnSt9  ?: 0, item.wf9  ?: "", item.wf9  ?: "")
                10 -> MidLandForecastDay(offset, item.rnSt10 ?: 0, item.rnSt10 ?: 0, item.wf10 ?: "", item.wf10 ?: "")
                else -> MidLandForecastDay(offset, 0, 0, "", "")
            }
        }
        return MidLandForecast(days)
    }

    /**
     * 법정동 도/시군구 코드 → 중기육상예보 예보구역코드(landRegId)
     *
     * 강원특별자치도(5100000000)는 시군구 코드로 영동/영서를 구분.
     * 영동: 강릉·동해·태백·속초·삼척·고성·양양
     * 영서: 그 외 (춘천·원주·홍천·횡성·영월·평창·정선·철원·화천·양구·인제)
     */
    private fun doCodeToLandRegId(doCode: Long, sigunguCode: Long): String = when (doCode) {
        1100000000L -> "11B00000" // 서울특별시
        2800000000L -> "11B00000" // 인천광역시
        4100000000L -> "11B00000" // 경기도
        4300000000L -> "11C10000" // 충청북도
        3000000000L -> "11C20000" // 대전광역시
        3600000000L -> "11C20000" // 세종특별자치시
        4400000000L -> "11C20000" // 충청남도
        5100000000L -> gangwonLandRegId(sigunguCode)
        5200000000L -> "11F10000" // 전북특별자치도
        1200000000L -> "11F20000" // 전남광주통합특별시
        5000000000L -> "11G00000" // 제주특별자치도
        2700000000L -> "11H10000" // 대구광역시
        4700000000L -> "11H10000" // 경상북도
        2600000000L -> "11H20000" // 부산광역시
        3100000000L -> "11H20000" // 울산광역시
        4800000000L -> "11H20000" // 경상남도
        else         -> "11B00000" // fallback
    }

    private fun gangwonLandRegId(sigunguCode: Long): String {
        val yeongdong = setOf(
            5115000000L, // 강릉시
            5117000000L, // 동해시
            5119000000L, // 태백시
            5121000000L, // 속초시
            5123000000L, // 삼척시
            5182000000L, // 고성군
            5183000000L  // 양양군
        )
        return if (sigunguCode in yeongdong) "11D20000" else "11D10000"
    }
}
