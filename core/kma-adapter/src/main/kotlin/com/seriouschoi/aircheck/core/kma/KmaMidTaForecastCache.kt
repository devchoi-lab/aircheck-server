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
import java.time.LocalDate

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
        val base = calculateMidFcstBase()
        return runWithUsage("KMA_MID_TA", apiUsagePort) {
            val response = kmaMidApi.getMidTa(
                serviceKey = apiKey, pageNo = 1, numOfRows = 10, dataType = "JSON",
                regId = regId, tmFc = base.toMidFcstTmFc()
            )
            check(response.isNotEmpty()) { "KMA mid-ta: empty response" }
            val parsed = forecastJson.decodeFromString<KmaApiResponse<MidTaItem>>(response)
            check(parsed.response.header.resultCode == "00") {
                "KMA mid-ta error: ${parsed.response.header.resultMsg}"
            }
            val item = parsed.response.body?.items?.item?.firstOrNull()
                ?: error("KMA mid-ta: no data")
            parseMidTaForecast(item, base.toLocalDate())
        }
    }
}

/**
 * 중기기온예보 항목에서 3~10일 예보를 만든다.
 * 값이 없으면 0도로 채우지 않고 null로 둔다.
 */
internal fun parseMidTaForecast(item: MidTaItem, baseDate: LocalDate): MidTaForecast {
    val days = (3..10).map { offset ->
        val date = baseDate.plusDays(offset.toLong())
        when (offset) {
            3  -> MidTaForecastDay(date, item.taMin3, item.taMax3)
            4  -> MidTaForecastDay(date, item.taMin4, item.taMax4)
            5  -> MidTaForecastDay(date, item.taMin5, item.taMax5)
            6  -> MidTaForecastDay(date, item.taMin6, item.taMax6)
            7  -> MidTaForecastDay(date, item.taMin7, item.taMax7)
            8  -> MidTaForecastDay(date, item.taMin8, item.taMax8)
            9  -> MidTaForecastDay(date, item.taMin9, item.taMax9)
            10 -> MidTaForecastDay(date, item.taMin10, item.taMax10)
            else -> MidTaForecastDay(date, null, null)
        }
    }
    return MidTaForecast(days)
}
