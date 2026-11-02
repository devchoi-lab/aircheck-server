package com.seriouschoi.aircheck.core.kma

import com.seriouschoi.aircheck.util.kma.region.KmaMidRegionMapper
import com.seriouschoi.aircheck.core.domain.model.MidTaForecast
import com.seriouschoi.aircheck.core.domain.model.MidTaForecastDay
import com.seriouschoi.aircheck.core.domain.port.ApiUsagePort
import com.seriouschoi.aircheck.core.kma.dto.KmaApiResponse
import com.seriouschoi.aircheck.core.kma.dto.MidTaItem
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["weather.provider"], havingValue = "kma")
class KmaMidTaForecastCache(
    @Value("\${kma.api.key:}") private val apiKey: String,
    private val kmaMidApi: KmaMidApi,
    private val apiUsagePort: ApiUsagePort?
) {
    @Cacheable("kma-mid-ta-forecast", key = "#lat + '_' + #lng")
    fun fetch(lat: Double, lng: Double): MidTaForecast {
        check(apiKey.isNotBlank()) { "KMA API key not configured" }
        val regId = KmaMidRegionMapper.toTaRegId(lat, lng)
        return runWithUsage("KMA_MID_TA") {
            val response = kmaMidApi.getMidTa(
                serviceKey = apiKey, pageNo = 1, numOfRows = 10, dataType = "JSON",
                regId = regId, tmFc = calculateMidFcstTmFc()
            )
            check(response.isNotEmpty()) { "KMA mid-ta: empty response" }
            val parsed = forecastJson.decodeFromString<KmaApiResponse<MidTaItem>>(response)
            check(parsed.response.header.resultCode == "00") {
                "KMA mid-ta error: ${parsed.response.header.resultMsg}"
            }
            val item = parsed.response.body?.items?.item?.firstOrNull()
                ?: error("KMA mid-ta: no data")
            parseMidTaForecast(item)
        }
    }

    private fun parseMidTaForecast(item: MidTaItem): MidTaForecast {
        val days = (3..10).map { offset ->
            when (offset) {
                3  -> MidTaForecastDay(offset, item.taMin3  ?: 0, item.taMax3  ?: 0)
                4  -> MidTaForecastDay(offset, item.taMin4  ?: 0, item.taMax4  ?: 0)
                5  -> MidTaForecastDay(offset, item.taMin5  ?: 0, item.taMax5  ?: 0)
                6  -> MidTaForecastDay(offset, item.taMin6  ?: 0, item.taMax6  ?: 0)
                7  -> MidTaForecastDay(offset, item.taMin7  ?: 0, item.taMax7  ?: 0)
                8  -> MidTaForecastDay(offset, item.taMin8  ?: 0, item.taMax8  ?: 0)
                9  -> MidTaForecastDay(offset, item.taMin9  ?: 0, item.taMax9  ?: 0)
                10 -> MidTaForecastDay(offset, item.taMin10 ?: 0, item.taMax10 ?: 0)
                else -> MidTaForecastDay(offset, 0, 0)
            }
        }
        return MidTaForecast(days)
    }
}
