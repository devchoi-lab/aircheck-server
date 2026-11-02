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
    @Cacheable("kma-mid-land-forecast", key = "#doCode")
    fun fetch(doCode: Long): MidLandForecast {
        check(apiKey.isNotBlank()) { "KMA API key not configured" }
        val regId = doCodeToLandRegId(doCode)
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
     * 법정동 도 코드 → 중기육상예보 예보구역코드(landRegId)
     *
     * 강원특별자치도(5100000000)는 영서/영동 구분 불가 — 영서(11D10000)로 기본 매핑.
     */
    private fun doCodeToLandRegId(doCode: Long): String = when (doCode) {
        1100000000L -> "11B00000" // 서울특별시
        2800000000L -> "11B00000" // 인천광역시
        4100000000L -> "11B00000" // 경기도
        4300000000L -> "11C10000" // 충청북도
        3000000000L -> "11C20000" // 대전광역시
        3600000000L -> "11C20000" // 세종특별자치시
        4400000000L -> "11C20000" // 충청남도
        5100000000L -> "11D10000" // 강원특별자치도 (영서 기본값)
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
}
