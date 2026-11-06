package com.seriouschoi.aircheck.core.kma

import com.seriouschoi.aircheck.core.domain.model.CurrentWeather
import com.seriouschoi.aircheck.core.domain.model.DailyForecast
import com.seriouschoi.aircheck.core.domain.model.HourlyForecast
import com.seriouschoi.aircheck.core.domain.model.WeatherCondition
import com.seriouschoi.aircheck.core.domain.port.ApiUsagePort
import com.seriouschoi.aircheck.core.kma.dto.FcstItem
import com.seriouschoi.aircheck.core.kma.dto.NcstItem
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

internal val KST_ZONE: ZoneId = ZoneId.of("Asia/Seoul")

internal val forecastJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
}

private val usageLogger = KotlinLogging.logger {}

internal fun <T> runWithUsage(apiType: String, apiUsagePort: ApiUsagePort?, block: () -> T): T {
    val startTime = System.currentTimeMillis()
    return try {
        val result = block()
        val elapsed = System.currentTimeMillis() - startTime
        apiUsagePort?.recordSuccess(apiType, elapsed)
        usageLogger.debug { "$apiType success (${elapsed}ms)" }
        result
    } catch (e: Exception) {
        apiUsagePort?.recordFailure(apiType, e.message)
        usageLogger.error(e) { "$apiType failed: ${e.message}" }
        throw e
    }
}

// ── 시간 계산 ─────────────────────────────────────────────────────────────────

/** 초단기실황 base_date/base_time 계산 */
internal fun calculateBaseDateTime(): Pair<String, String> {
    val now = LocalDateTime.now(KST_ZONE)
    val base = if (now.minute < 40) now.minusHours(1) else now
    return base.format(DateTimeFormatter.ofPattern("yyyyMMdd")) to
            base.format(DateTimeFormatter.ofPattern("HH00"))
}

/** 단기예보 base_date/base_time 계산 (발표시각: 02·05·08·11·14·17·20·23시) */
internal fun calculateVilageFcstBaseDateTime(): Pair<String, String> {
    val now = LocalDateTime.now(KST_ZONE)
    val baseTimes = listOf(2, 5, 8, 11, 14, 17, 20, 23)
    val baseHour = baseTimes.lastOrNull { it <= now.hour - 1 } ?: 23
    val base = if (baseHour == 23 && now.hour < 23) now.minusDays(1).withHour(23) else now.withHour(baseHour)
    return base.format(DateTimeFormatter.ofPattern("yyyyMMdd")) to
            String.format("%02d00", baseHour)
}

/** 중기예보 tmFc 계산 (발표시각: 06시·18시) */
internal fun calculateMidFcstTmFc(): String {
    val now = LocalDateTime.now(KST_ZONE)
    val base = when {
        now.hour < 6  -> now.minusDays(1).withHour(18)
        now.hour < 18 -> now.withHour(6)
        else          -> now.withHour(18)
    }
    return base.format(DateTimeFormatter.ofPattern("yyyyMMddHH00"))
}

// ── 날씨 파싱 ─────────────────────────────────────────────────────────────────

internal fun parseCurrentWeather(items: List<NcstItem>): CurrentWeather {
    var temperature = 0.0; var humidity = 0; var precipitation = 0.0
    var windSpeed = 0.0; var windDirection = 0; var pty = 0
    items.forEach { item ->
        when (item.category) {
            "T1H" -> temperature   = item.obsrValue.toDoubleOrNull() ?: 0.0
            "REH" -> humidity      = item.obsrValue.toIntOrNull()    ?: 0
            "RN1" -> precipitation = item.obsrValue.toDoubleOrNull() ?: 0.0
            "WSD" -> windSpeed     = item.obsrValue.toDoubleOrNull() ?: 0.0
            "VEC" -> windDirection = item.obsrValue.toIntOrNull()    ?: 0
            "PTY" -> pty           = item.obsrValue.toIntOrNull()    ?: 0
        }
    }
    val isDay = LocalDateTime.now(KST_ZONE).hour in 6..18
    return CurrentWeather(
        temperature = temperature,
        feelsLike = calculateFeelsLike(temperature, windSpeed, humidity),
        precipitation = precipitation,
        humidity = humidity,
        windSpeed = windSpeed,
        windDirection = windDirection,
        weatherCondition = convertToWeatherCondition(pty = pty, sky = 1),
        isDay = isDay
    )
}

internal fun parseExtendedHourlyForecast(items: List<FcstItem>): List<HourlyForecast> {
    if (items.isEmpty()) return emptyList()
    return items.groupBy { "${it.fcstDate}_${it.fcstTime}" }.map { (timeKey, timeItems) ->
        val (fcstDate, fcstTime) = timeKey.split("_")
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
        val kstDateTime: LocalDateTime = LocalDateTime.parse(
            "${fcstDate}${fcstTime}", DateTimeFormatter.ofPattern("yyyyMMddHHmm")
        )
        HourlyForecast(
            time = kstDateTime.atZone(KST_ZONE).toInstant(),
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

internal fun parseDailyForecast(items: List<FcstItem>): List<DailyForecast> {
    val dayOfWeekFmt = DateTimeFormatter.ofPattern("E", Locale.KOREAN)
    val dateFmt = DateTimeFormatter.ofPattern("yyyyMMdd")
    return items.groupBy { it.fcstDate }.map { (date, dateItems) ->
        var minTemp = Double.MAX_VALUE
        var maxTemp = Double.MIN_VALUE
        var amPop = 0; var pmPop = 0
        var amSky = 1; var pmSky = 1
        var amPty = 0; var pmPty = 0
        dateItems.forEach { item ->
            val isAm = (item.fcstTime.substring(0, 2).toIntOrNull() ?: 0) < 12
            when (item.category) {
                "TMN" -> minTemp = item.fcstValue.toDoubleOrNull() ?: minTemp
                "TMX" -> maxTemp = item.fcstValue.toDoubleOrNull() ?: maxTemp
                "TMP" -> {
                    val t = item.fcstValue.toDoubleOrNull() ?: 0.0
                    if (t < minTemp) minTemp = t
                    if (t > maxTemp) maxTemp = t
                }
                "POP" -> {
                    val p = item.fcstValue.toIntOrNull() ?: 0
                    if (isAm)
                        amPop = maxOf(amPop, p)
                    else
                        pmPop = maxOf(pmPop, p)
                }
                "SKY" -> {
                    val s = item.fcstValue.toIntOrNull() ?: 1
                    if (isAm)
                        amSky = s
                    else
                        pmSky = s
                }
                "PTY" -> {
                    val p = item.fcstValue.toIntOrNull() ?: 0
                    if (isAm)
                        amPty = p
                    else
                        pmPty = p
                }
            }
        }
        val localDate = LocalDate.parse(date, dateFmt)
        DailyForecast(
            date = localDate,
            dayOfWeek = localDate.format(dayOfWeekFmt),
            minTemp = if (minTemp == Double.MAX_VALUE) 0.0 else minTemp,
            maxTemp = if (maxTemp == Double.MIN_VALUE) 0.0 else maxTemp,
            amPrecipProb = amPop, pmPrecipProb = pmPop,
            amWeatherCondition = convertToWeatherCondition(amPty, amSky),
            pmWeatherCondition = convertToWeatherCondition(pmPty, pmSky)
        )
    }.sortedBy { it.date }.take(3)
}

// ── 공통 유틸 ─────────────────────────────────────────────────────────────────

internal fun calculateFeelsLike(temp: Double, windSpeed: Double, humidity: Int): Double = when {
    temp <= 10 && windSpeed >= 1.3 -> {
        val ws016 = Math.pow(windSpeed, 0.16)
        13.12 + 0.6215 * temp - 11.37 * ws016 + 0.3965 * temp * ws016
    }
    temp >= 27 -> temp + 0.33 * (humidity / 100.0 * 6.105 * Math.exp(17.27 * temp / (237.7 + temp))) - 4.0
    else -> temp
}

internal fun convertToWeatherCondition(pty: Int, sky: Int): WeatherCondition = when (pty) {
    1, 5 -> WeatherCondition.RAIN
    2, 6 -> WeatherCondition.RAIN
    3, 7 -> WeatherCondition.SNOW
    else -> when (sky) {
        1    -> WeatherCondition.CLEAR
        3    -> WeatherCondition.PARTLY_CLOUDY
        4    -> WeatherCondition.CLOUDY
        else -> WeatherCondition.CLEAR
    }
}
