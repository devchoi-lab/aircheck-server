package com.seriouschoi.aircheck.core.kma

import com.seriouschoi.aircheck.core.domain.model.WeatherResponse
import com.seriouschoi.aircheck.core.domain.port.ApiUsagePort
import com.seriouschoi.aircheck.core.kma.dto.FcstItem
import com.seriouschoi.aircheck.core.kma.dto.KmaApiResponse
import com.seriouschoi.aircheck.core.kma.dto.NcstItem
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component

/**
 * 초단기실황 + 단기예보 조회 캐시 빈.
 *
 * 실패 시 null 반환 대신 예외를 throw → 성공 결과만 캐시됨.
 * KmaWeatherAdapter에서 Result.runCatching으로 감싸서 호출.
 */
@Component
@ConditionalOnProperty(name = ["weather.provider"], havingValue = "kma")
class KmaShortForecastCache(
    @Value("\${kma.api.key:}") private val apiKey: String,
    private val kmaApi: KmaApi,
    private val apiUsagePort: ApiUsagePort?
) {
    @Cacheable("kma-short-forecast", key = "#lat + '_' + #lng")
    fun fetch(lat: Double, lng: Double): WeatherResponse {
        check(apiKey.isNotBlank()) { "KMA API key not configured" }

        val grid = KmaGridConverter.toGrid(lat, lng)
        val (baseDate, baseTime) = calculateBaseDateTime()
        val (vilageFcstDate, vilageFcstTime) = calculateVilageFcstBaseDateTime()

        val currentWeather = fetchCurrentWeather(grid.nx, grid.ny, baseDate, baseTime)
        val vilageFcstItems = fetchVilageFcstItems(grid.nx, grid.ny, vilageFcstDate, vilageFcstTime)

        return WeatherResponse(
            current = currentWeather,
            hourlyForecast = parseExtendedHourlyForecast(vilageFcstItems),
            dailyForecast = parseDailyForecast(vilageFcstItems),
            midTermForecast = emptyList()
        )
    }

    private fun fetchCurrentWeather(nx: Int, ny: Int, baseDate: String, baseTime: String) =
        runWithUsage("KMA_ULTRA_SRT_NCST") {
            val response = kmaApi.getUltraSrtNcst(
                serviceKey = apiKey, pageNo = 1, numOfRows = 60, dataType = "JSON",
                baseDate = baseDate, baseTime = baseTime, nx = nx, ny = ny
            )
            val parsed = forecastJson.decodeFromString<KmaApiResponse<NcstItem>>(response)
            check(parsed.response.header.resultCode == "00") {
                "KMA NCST error: ${parsed.response.header.resultMsg}"
            }
            parseCurrentWeather(parsed.response.body?.items?.item ?: error("KMA NCST: no items"))
        }

    private fun fetchVilageFcstItems(nx: Int, ny: Int, baseDate: String, baseTime: String): List<FcstItem> =
        runWithUsage("KMA_VILAGE_FCST") {
            val response = kmaApi.getVilageFcst(
                serviceKey = apiKey, pageNo = 1, numOfRows = 1000, dataType = "JSON",
                baseDate = baseDate, baseTime = baseTime, nx = nx, ny = ny
            )
            val parsed = forecastJson.decodeFromString<KmaApiResponse<FcstItem>>(response)
            check(parsed.response.header.resultCode == "00") {
                "KMA vilage fcst error: ${parsed.response.header.resultMsg}"
            }
            parsed.response.body?.items?.item ?: emptyList()
        }
}
