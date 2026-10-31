package com.seriouschoi.aircheck.core.kma

import com.seriouschoi.aircheck.util.kma.region.KmaMidRegionMapper
import com.seriouschoi.aircheck.core.domain.model.CurrentWeather
import com.seriouschoi.aircheck.core.domain.model.DailyForecast
import com.seriouschoi.aircheck.core.domain.model.HourlyForecast
import com.seriouschoi.aircheck.core.domain.model.MidLandForecast
import com.seriouschoi.aircheck.core.domain.model.MidLandForecastDay
import com.seriouschoi.aircheck.core.domain.model.MidTaForecast
import com.seriouschoi.aircheck.core.domain.model.MidTaForecastDay
import com.seriouschoi.aircheck.core.domain.model.WeatherCondition
import com.seriouschoi.aircheck.core.domain.model.WeatherResponse
import com.seriouschoi.aircheck.core.domain.port.ApiUsagePort
import com.seriouschoi.aircheck.core.domain.port.KmaWeatherPort
import com.seriouschoi.aircheck.core.domain.port.ServiceConfigPort
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
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val KST = ZoneId.of("Asia/Seoul")
private val logger = KotlinLogging.logger {}

/**
 * 기상청 API Adapter (KmaWeatherPort 구현체)
 *
 * WeatherPort와 달리 예보 종류별로 메서드가 분리되어 있어
 * UseCase에서 doCode를 활용한 중기육상예보 지역 매핑이 가능하다.
 *
 * 활성화: weather.provider=kma
 */
@Component
@ConditionalOnProperty(name = ["weather.provider"], havingValue = "kma")
class KmaWeatherAdapter(
    private val configPort: ServiceConfigPort,
    private val apiUsagePort: ApiUsagePort?,
    @Value("\${kma.api.key:}") private val apiKey: String,
    private val kmaApi: KmaApi,
    private val kmaMidApi: KmaMidApi
) : KmaWeatherPort {

    companion object {
        private const val API_TYPE_NCST = "KMA_ULTRA_SRT_NCST"
        private const val API_TYPE_VILAGE = "KMA_VILAGE_FCST"
        private const val API_TYPE_MID_LAND = "KMA_MID_LAND_FCST"
        private const val API_TYPE_MID_TA = "KMA_MID_TA"
    }

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    // ── 퍼블릭 메서드 ─────────────────────────────────────────────────────────

    @Cacheable("kma-short-forecast", key = "#lat + '_' + #lng", unless = "#result == null")
    override fun getShortForecast(lat: Double, lng: Double): WeatherResponse? {
        if (apiKey.isBlank()) {
            logger.warn { "KMA API key not configured" }
            return null
        }
        return try {
            val grid = KmaGridConverter.toGrid(lat, lng)
            val (baseDate, baseTime) = calculateBaseDateTime()
            val (vilageFcstDate, vilageFcstTime) = calculateVilageFcstBaseDateTime()

            val currentWeather = fetchCurrentWeather(grid.nx, grid.ny, baseDate, baseTime)
                ?: run { logger.warn { "Failed to fetch current weather" }; return null }
            val vilageFcstItems = fetchVilageFcstItems(grid.nx, grid.ny, vilageFcstDate, vilageFcstTime)

            WeatherResponse(
                current = currentWeather,
                hourlyForecast = parseExtendedHourlyForecast(vilageFcstItems),
                dailyForecast = parseDailyForecast(vilageFcstItems),
                midTermForecast = emptyList()
            )
        } catch (e: Exception) {
            logger.error(e) { "KMA short forecast error: ${e.message}" }
            null
        }
    }

    /**
     * @param doCode 법정동 도 단위 코드. 강원도(5100000000)는 영서(11D10000)로 기본 매핑.
     */
    @Cacheable("kma-mid-land-forecast", key = "#doCode", unless = "#result == null")
    override fun getMidLandForecast(doCode: Long): MidLandForecast? {
        if (apiKey.isBlank()) return null
        val regId = doCodeToLandRegId(doCode)
        val tmFc = calculateMidFcstTmFc()
        val startTime = System.currentTimeMillis()
        return try {
            val response = kmaMidApi.getMidLandFcst(
                serviceKey = apiKey, pageNo = 1, numOfRows = 10,
                dataType = "JSON", regId = regId, tmFc = tmFc
            )
            if (response.isEmpty()) {
                apiUsagePort?.recordFailure(API_TYPE_MID_LAND, "Empty response")
                return null
            }
            val parsed = json.decodeFromString<KmaApiResponse<MidLandFcstItem>>(response)
            if (parsed.response.header.resultCode != "00") {
                apiUsagePort?.recordFailure(API_TYPE_MID_LAND, parsed.response.header.resultMsg)
                return null
            }
            val item = parsed.response.body?.items?.item?.firstOrNull()
                ?: run { apiUsagePort?.recordFailure(API_TYPE_MID_LAND, "No data"); return null }
            apiUsagePort?.recordSuccess(API_TYPE_MID_LAND, System.currentTimeMillis() - startTime)
            parseMidLandForecast(item)
        } catch (e: Exception) {
            logger.error(e) { "KMA mid-land forecast error: ${e.message}" }
            apiUsagePort?.recordFailure(API_TYPE_MID_LAND, e.message)
            null
        }
    }

    @Cacheable("kma-mid-ta-forecast", key = "#lat + '_' + #lng", unless = "#result == null")
    override fun getMidTaForecast(lat: Double, lng: Double): MidTaForecast? {
        if (apiKey.isBlank()) return null
        val regId = KmaMidRegionMapper.toTaRegId(lat, lng)
        val tmFc = calculateMidFcstTmFc()
        val startTime = System.currentTimeMillis()
        return try {
            val response = kmaMidApi.getMidTa(
                serviceKey = apiKey, pageNo = 1, numOfRows = 10,
                dataType = "JSON", regId = regId, tmFc = tmFc
            )
            if (response.isEmpty()) {
                apiUsagePort?.recordFailure(API_TYPE_MID_TA, "Empty response")
                return null
            }
            val parsed = json.decodeFromString<KmaApiResponse<MidTaItem>>(response)
            if (parsed.response.header.resultCode != "00") {
                apiUsagePort?.recordFailure(API_TYPE_MID_TA, parsed.response.header.resultMsg)
                return null
            }
            val item = parsed.response.body?.items?.item?.firstOrNull()
                ?: run { apiUsagePort?.recordFailure(API_TYPE_MID_TA, "No data"); return null }
            apiUsagePort?.recordSuccess(API_TYPE_MID_TA, System.currentTimeMillis() - startTime)
            parseMidTaForecast(item)
        } catch (e: Exception) {
            logger.error(e) { "KMA mid-ta forecast error: ${e.message}" }
            apiUsagePort?.recordFailure(API_TYPE_MID_TA, e.message)
            null
        }
    }

    // ── 파싱 ─────────────────────────────────────────────────────────────────

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

    // ── API 호출 ──────────────────────────────────────────────────────────────

    private fun fetchCurrentWeather(nx: Int, ny: Int, baseDate: String, baseTime: String): CurrentWeather? {
        val startTime = System.currentTimeMillis()
        return try {
            val response = kmaApi.getUltraSrtNcst(
                serviceKey = apiKey, pageNo = 1, numOfRows = 60, dataType = "JSON",
                baseDate = baseDate, baseTime = baseTime, nx = nx, ny = ny
            )
            val parsed = json.decodeFromString<KmaApiResponse<NcstItem>>(response)
            if (parsed.response.header.resultCode != "00") {
                logger.warn { "KMA NCST error: ${parsed.response.header.resultMsg}" }
                apiUsagePort?.recordFailure(API_TYPE_NCST, parsed.response.header.resultMsg)
                return null
            }
            val items = parsed.response.body?.items?.item ?: return null
            val result = parseCurrentWeather(items)
            apiUsagePort?.recordSuccess(API_TYPE_NCST, System.currentTimeMillis() - startTime)
            result
        } catch (e: Exception) {
            logger.error(e) { "Failed to fetch current weather: ${e.message}" }
            apiUsagePort?.recordFailure(API_TYPE_NCST, e.message)
            null
        }
    }

    private fun fetchVilageFcstItems(nx: Int, ny: Int, baseDate: String, baseTime: String): List<FcstItem> {
        val startTime = System.currentTimeMillis()
        return try {
            val response = kmaApi.getVilageFcst(
                serviceKey = apiKey, pageNo = 1, numOfRows = 1000, dataType = "JSON",
                baseDate = baseDate, baseTime = baseTime, nx = nx, ny = ny
            )
            val parsed = json.decodeFromString<KmaApiResponse<FcstItem>>(response)
            if (parsed.response.header.resultCode != "00") {
                logger.warn { "KMA vilage fcst error: ${parsed.response.header.resultMsg}" }
                apiUsagePort?.recordFailure(API_TYPE_VILAGE, parsed.response.header.resultMsg)
                return emptyList()
            }
            val result = parsed.response.body?.items?.item ?: emptyList()
            apiUsagePort?.recordSuccess(API_TYPE_VILAGE, System.currentTimeMillis() - startTime)
            result
        } catch (e: Exception) {
            logger.error(e) { "Failed to fetch vilage forecast: ${e.message}" }
            apiUsagePort?.recordFailure(API_TYPE_VILAGE, e.message)
            emptyList()
        }
    }

    // ── 날씨 파싱 ─────────────────────────────────────────────────────────────

    private fun parseCurrentWeather(items: List<NcstItem>): CurrentWeather {
        var temperature = 0.0; var humidity = 0; var precipitation = 0.0
        var windSpeed = 0.0; var windDirection = 0; var pty = 0
        items.forEach { item ->
            when (item.category) {
                "T1H" -> temperature  = item.obsrValue.toDoubleOrNull() ?: 0.0
                "REH" -> humidity     = item.obsrValue.toIntOrNull()    ?: 0
                "RN1" -> precipitation= item.obsrValue.toDoubleOrNull() ?: 0.0
                "WSD" -> windSpeed    = item.obsrValue.toDoubleOrNull() ?: 0.0
                "VEC" -> windDirection= item.obsrValue.toIntOrNull()    ?: 0
                "PTY" -> pty          = item.obsrValue.toIntOrNull()    ?: 0
            }
        }
        val feelsLike = calculateFeelsLike(temperature, windSpeed, humidity)
        val isDay = java.time.ZonedDateTime.now(KST).hour in 6..18
        return CurrentWeather(
            temperature = temperature, feelsLike = feelsLike,
            precipitation = precipitation, humidity = humidity,
            windSpeed = windSpeed, windDirection = windDirection,
            weatherCondition = convertToWeatherCondition(pty = pty, sky = 1),
            isDay = isDay
        )
    }

    private fun parseExtendedHourlyForecast(items: List<FcstItem>): List<HourlyForecast> {
        if (items.isEmpty()) return emptyList()
        return items.groupBy { "${it.fcstDate}_${it.fcstTime}" }.map { (timeKey, timeItems) ->
            val fcstDate = timeKey.split("_")[0]
            val fcstTime = timeKey.split("_")[1]
            val hour = fcstTime.substring(0, 2).toInt()
            var temperature = 0.0; var sky = 1; var pty = 0; var pop = 0
            var sno = 0.0; var windSpeed = 0.0; var humidity = 50
            timeItems.forEach { item ->
                when (item.category) {
                    "TMP" -> temperature = item.fcstValue.toDoubleOrNull() ?: 0.0
                    "SKY" -> sky         = item.fcstValue.toIntOrNull()    ?: 1
                    "PTY" -> pty         = item.fcstValue.toIntOrNull()    ?: 0
                    "POP" -> pop         = item.fcstValue.toIntOrNull()    ?: 0
                    "SNO" -> sno         = item.fcstValue.toDoubleOrNull() ?: 0.0
                    "WSD" -> windSpeed   = item.fcstValue.toDoubleOrNull() ?: 0.0
                    "REH" -> humidity    = item.fcstValue.toIntOrNull()    ?: 50
                }
            }
            val kstDateTime = LocalDateTime.parse(
                "${fcstDate}${fcstTime}", DateTimeFormatter.ofPattern("yyyyMMddHHmm")
            )
            HourlyForecast(
                time = kstDateTime.atZone(KST).toInstant(),
                hour = hour,
                temperature = temperature,
                feelsLike = calculateFeelsLike(temperature, windSpeed, humidity),
                precipitationProbability = pop,
                snowfall = sno,
                weatherCode = pty * 10 + sky,
                weatherCondition = convertToWeatherCondition(pty, sky)
            )
        }.sortedBy { it.time }.take(48)
    }

    private fun parseDailyForecast(items: List<FcstItem>): List<DailyForecast> {
        val dayOfWeekFormatter = DateTimeFormatter.ofPattern("E", java.util.Locale.KOREAN)
        val dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
        return items.groupBy { it.fcstDate }.map { (date, dateItems) ->
            var minTemp = Double.MAX_VALUE; var maxTemp = Double.MIN_VALUE
            var amPop = 0; var pmPop = 0; var amSky = 1; var pmSky = 1; var amPty = 0; var pmPty = 0
            dateItems.forEach { item ->
                val hour = item.fcstTime.substring(0, 2).toIntOrNull() ?: 0
                val isAm = hour < 12
                when (item.category) {
                    "TMN" -> minTemp = item.fcstValue.toDoubleOrNull() ?: minTemp
                    "TMX" -> maxTemp = item.fcstValue.toDoubleOrNull() ?: maxTemp
                    "TMP" -> {
                        val t = item.fcstValue.toDoubleOrNull() ?: 0.0
                        if (t < minTemp) minTemp = t
                        if (t > maxTemp) maxTemp = t
                    }
                    "POP" -> { val p = item.fcstValue.toIntOrNull() ?: 0; if (isAm) amPop = maxOf(amPop, p) else pmPop = maxOf(pmPop, p) }
                    "SKY" -> { val s = item.fcstValue.toIntOrNull() ?: 1; if (isAm) amSky = s else pmSky = s }
                    "PTY" -> { val p = item.fcstValue.toIntOrNull() ?: 0; if (isAm) amPty = p else pmPty = p }
                }
            }
            val localDate = java.time.LocalDate.parse(date, dateFormatter)
            DailyForecast(
                date = "${localDate.year}-${String.format("%02d", localDate.monthValue)}-${String.format("%02d", localDate.dayOfMonth)}",
                dayOfWeek = localDate.format(dayOfWeekFormatter),
                minTemp = if (minTemp == Double.MAX_VALUE) 0.0 else minTemp,
                maxTemp = if (maxTemp == Double.MIN_VALUE) 0.0 else maxTemp,
                amPrecipProb = amPop, pmPrecipProb = pmPop,
                amWeatherCondition = convertToWeatherCondition(amPty, amSky),
                pmWeatherCondition = convertToWeatherCondition(pmPty, pmSky)
            )
        }.sortedBy { it.date }.take(3)
    }

    // ── 유틸리티 ──────────────────────────────────────────────────────────────

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

    private fun calculateBaseDateTime(): Pair<String, String> {
        val now = java.time.ZonedDateTime.now(KST).toLocalDateTime()
        val baseTime = if (now.minute < 40) now.minusHours(1) else now
        return Pair(
            baseTime.format(DateTimeFormatter.ofPattern("yyyyMMdd")),
            baseTime.format(DateTimeFormatter.ofPattern("HH00"))
        )
    }

    private fun calculateVilageFcstBaseDateTime(): Pair<String, String> {
        val now = java.time.ZonedDateTime.now(KST).toLocalDateTime()
        val hour = now.hour
        val baseTimes = listOf(2, 5, 8, 11, 14, 17, 20, 23)
        val baseHour = baseTimes.lastOrNull { it <= hour - 1 } ?: 23
        val baseDateTime = if (baseHour == 23 && hour < 23) now.minusDays(1).withHour(23) else now.withHour(baseHour)
        return Pair(
            baseDateTime.format(DateTimeFormatter.ofPattern("yyyyMMdd")),
            String.format("%02d00", baseHour)
        )
    }

    private fun calculateMidFcstTmFc(): String {
        val now = LocalDateTime.now()
        val baseDateTime = when {
            now.hour < 6  -> now.minusDays(1).withHour(18)
            now.hour < 18 -> now.withHour(6)
            else          -> now.withHour(18)
        }
        return baseDateTime.format(DateTimeFormatter.ofPattern("yyyyMMddHH00"))
    }

    private fun calculateFeelsLike(temp: Double, windSpeed: Double, humidity: Int): Double = when {
        temp <= 10 && windSpeed >= 1.3 -> {
            val ws016 = Math.pow(windSpeed, 0.16)
            13.12 + 0.6215 * temp - 11.37 * ws016 + 0.3965 * temp * ws016
        }
        temp >= 27 -> temp + 0.33 * (humidity / 100.0 * 6.105 * Math.exp(17.27 * temp / (237.7 + temp))) - 4.0
        else -> temp
    }

    private fun convertToWeatherCondition(pty: Int, sky: Int): WeatherCondition = when (pty) {
        1, 5    -> WeatherCondition.RAIN
        2, 6    -> WeatherCondition.RAIN
        3, 7    -> WeatherCondition.SNOW
        else    -> when (sky) {
            1    -> WeatherCondition.CLEAR
            3    -> WeatherCondition.PARTLY_CLOUDY
            4    -> WeatherCondition.CLOUDY
            else -> WeatherCondition.CLEAR
        }
    }
}
