package com.seriouschoi.aircheck.core.kma

import com.seriouschoi.aircheck.util.kma.region.KmaMidRegionMapper
import com.seriouschoi.aircheck.core.domain.model.MidLandForecast
import com.seriouschoi.aircheck.core.domain.model.MidLandForecastDay
import com.seriouschoi.aircheck.core.domain.model.MidTaForecast
import com.seriouschoi.aircheck.core.domain.model.MidTaForecastDay
import com.seriouschoi.aircheck.core.domain.model.WeatherResponse
import com.seriouschoi.aircheck.core.domain.port.ApiUsagePort
import com.seriouschoi.aircheck.core.kma.dto.FcstItem
import com.seriouschoi.aircheck.core.kma.dto.KmaApiResponse
import com.seriouschoi.aircheck.core.kma.dto.MidLandFcstItem
import com.seriouschoi.aircheck.core.kma.dto.MidTaItem
import com.seriouschoi.aircheck.core.kma.dto.NcstItem
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component

private val cacheLogger = KotlinLogging.logger {}

private val forecastJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
}

// ── 초단기실황 + 단기예보 캐시 ────────────────────────────────────────────────

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
            parseCurrentWeather(parsed.response.body?.items?.item
                ?: error("KMA NCST: no items"))
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

// ── 중기육상예보 캐시 ──────────────────────────────────────────────────────────

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
        return runWithUsage("KMA_MID_LAND_FCST") {
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

// ── 중기기온예보 캐시 ──────────────────────────────────────────────────────────

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

// ── 공통 헬퍼 ─────────────────────────────────────────────────────────────────

private fun <T> runWithUsage(apiType: String, block: () -> T): T {
    val startTime = System.currentTimeMillis()
    return try {
        val result = block()
        cacheLogger.debug { "$apiType success (${System.currentTimeMillis() - startTime}ms)" }
        result
    } catch (e: Exception) {
        cacheLogger.error(e) { "$apiType failed: ${e.message}" }
        throw e
    }
}
