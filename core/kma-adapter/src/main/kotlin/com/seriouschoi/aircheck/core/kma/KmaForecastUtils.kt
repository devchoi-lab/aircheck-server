package com.seriouschoi.aircheck.core.kma

import com.seriouschoi.aircheck.core.domain.model.CurrentWeather
import com.seriouschoi.aircheck.core.domain.model.DailyForecast
import com.seriouschoi.aircheck.core.domain.model.HourlyForecast
import com.seriouschoi.aircheck.core.domain.model.PrecipitationType
import com.seriouschoi.aircheck.core.domain.model.SkyCondition
import com.seriouschoi.aircheck.core.domain.port.ApiUsagePort
import com.seriouschoi.aircheck.core.kma.dto.FcstDto
import com.seriouschoi.aircheck.core.kma.dto.NcstDto
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal val KST_ZONE: ZoneId = ZoneId.of("Asia/Seoul")

internal val forecastJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
}

private val usageLogger = KotlinLogging.logger {}
private val codeLogger = KotlinLogging.logger {}

/** 기상청 결측값 기준: +900 이상, -900 이하는 Missing 값 */
private const val KMA_MISSING_ABS = 900

/** 코드값 문자열을 Int로 읽는다. 숫자가 아니거나 결측값이면 null. */
private fun String.toKmaCodeOrNull(): Int? = toIntOrNull()?.takeIf { kotlin.math.abs(it) < KMA_MISSING_ABS }

/** 결측이면 null, 정의되지 않은 코드면 UNKNOWN(경고 로그에 원래 코드를 남김). */
private fun String.toPrecipitationTypeOrNull(): PrecipitationType? = toKmaCodeOrNull()?.let { code ->
    PrecipitationType.fromCode(code).also {
        if (it == PrecipitationType.UNKNOWN) codeLogger.warn { "정의되지 않은 PTY 코드: $code" }
    }
}

/** 결측이면 null, 정의되지 않은 코드면 UNKNOWN(경고 로그에 원래 코드를 남김). */
private fun String.toSkyConditionOrNull(): SkyCondition? = toKmaCodeOrNull()?.let { code ->
    SkyCondition.fromCode(code).also {
        if (it == SkyCondition.UNKNOWN) codeLogger.warn { "정의되지 않은 SKY 코드: $code" }
    }
}

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

/**
 * 중기예보 발표시각 계산 (KST, 06시·18시).
 * 중기예보의 "N일 후"는 이 발표일(날짜) 기준이다.
 */
internal fun calculateMidFcstBase(): LocalDateTime {
    val now = LocalDateTime.now(KST_ZONE)
    return when {
        now.hour < 6  -> now.minusDays(1).withHour(18)
        now.hour < 18 -> now.withHour(6)
        else          -> now.withHour(18)
    }
}

/** 중기예보 API의 tmFc 파라미터 (yyyyMMddHH00) */
internal fun LocalDateTime.toMidFcstTmFc(): String =
    format(DateTimeFormatter.ofPattern("yyyyMMddHH00"))

// ── 날씨 파싱 ─────────────────────────────────────────────────────────────────

/**
 * 초단기실황 항목에서 현재 날씨를 만든다.
 *
 * 항목이 없거나 숫자로 읽을 수 없으면 기본값(0 등)으로 채우지 않고 null로 둔다.
 * 초단기실황에는 하늘상태(SKY)가 없어 강수형태(PTY)만 기상청 값 그대로 담는다.
 */
internal fun parseCurrentWeather(items: List<NcstDto>): CurrentWeather {
    var temperature: Double? = null; var humidity: Int? = null; var precipitation: Double? = null
    var windSpeed: Double? = null; var windDirection: Int? = null; var pty: PrecipitationType? = null
    items.forEach { item ->
        when (item.category) {
            "T1H" -> item.obsrValue.toDoubleOrNull()?.let { temperature = it }
            "REH" -> item.obsrValue.toIntOrNull()?.let { humidity = it }
            "RN1" -> item.obsrValue.toDoubleOrNull()?.let { precipitation = it }
            "WSD" -> item.obsrValue.toDoubleOrNull()?.let { windSpeed = it }
            "VEC" -> item.obsrValue.toIntOrNull()?.let { windDirection = it }
            "PTY" -> item.obsrValue.toPrecipitationTypeOrNull()?.let { pty = it }
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
        precipitationType = pty,
        isDay = isDay
    )
}

/**
 * 단기예보 항목에서 시간별 예보를 만든다.
 *
 * 항목이 없거나 숫자로 읽을 수 없으면 기본값으로 채우지 않고 null로 둔다.
 * 참고: 기상청 SNO는 범주 문자열(예: "적설없음")이라 숫자로 읽을 수 없으면 snowfall은 null이다.
 */
internal fun parseExtendedHourlyForecast(items: List<FcstDto>): List<HourlyForecast> {
    if (items.isEmpty()) return emptyList()
    return items.groupBy { "${it.fcstDate}_${it.fcstTime}" }.map { (timeKey, timeItems) ->
        val (fcstDate, fcstTime) = timeKey.split("_")
        var temperature: Double? = null; var sky: SkyCondition? = null; var pty: PrecipitationType? = null; var pop: Int? = null
        var sno: Double? = null; var windSpeed: Double? = null; var humidity: Int? = null
        timeItems.forEach { item ->
            when (item.category) {
                "TMP" -> item.fcstValue.toDoubleOrNull()?.let { temperature = it }
                "SKY" -> item.fcstValue.toSkyConditionOrNull()?.let { sky = it }
                "PTY" -> item.fcstValue.toPrecipitationTypeOrNull()?.let { pty = it }
                "POP" -> item.fcstValue.toIntOrNull()?.let { pop = it }
                "SNO" -> item.fcstValue.toDoubleOrNull()?.let { sno = it }
                "WSD" -> item.fcstValue.toDoubleOrNull()?.let { windSpeed = it }
                "REH" -> item.fcstValue.toIntOrNull()?.let { humidity = it }
            }
        }
        val kstDateTime: LocalDateTime = LocalDateTime.parse(
            "${fcstDate}${fcstTime}", DateTimeFormatter.ofPattern("yyyyMMddHHmm")
        )
        HourlyForecast(
            time = kstDateTime.atZone(KST_ZONE).toInstant(),
            temperature = temperature,
            feelsLike = calculateFeelsLike(temperature, windSpeed, humidity),
            precipitationProbability = pop,
            snowfall = sno,
            sky = sky,
            precipitationType = pty
        )
    }.sortedBy { it.time }.take(48)
}

/**
 * 단기예보 항목에서 일별 예보를 만든다.
 *
 * 값이 없거나 숫자로 읽을 수 없으면 기본값(0, 맑음 등)으로 채우지 않고 null로 둔다.
 * 오전/오후는 예보 시각이 12시 이전/이후인지로 나눈다.
 */
internal fun parseDailyForecast(items: List<FcstDto>): List<DailyForecast> {
    val dateFmt = DateTimeFormatter.ofPattern("yyyyMMdd")
    return items.groupBy { it.fcstDate }.map { (date, dateItems) ->
        var minTemp: Double? = null
        var maxTemp: Double? = null
        var amPop: Int? = null; var pmPop: Int? = null
        var amSky: SkyCondition? = null; var pmSky: SkyCondition? = null
        var amPty: PrecipitationType? = null; var pmPty: PrecipitationType? = null
        dateItems.forEach { item ->
            val isAm = (item.fcstTime.substring(0, 2).toIntOrNull() ?: 0) < 12
            when (item.category) {
                // 일 최저/최고기온 (TMN/TMX). 읽을 수 없으면 기존 값 유지
                "TMN" -> item.fcstValue.toDoubleOrNull()?.let { minTemp = it }
                "TMX" -> item.fcstValue.toDoubleOrNull()?.let { maxTemp = it }
                // TMN/TMX가 없을 때를 위해 시간대별 기온(TMP)으로 최솟값/최댓값 갱신
                "TMP" -> item.fcstValue.toDoubleOrNull()?.let { t ->
                    val curMin = minTemp
                    val curMax = maxTemp
                    if (curMin == null || t < curMin) minTemp = t
                    if (curMax == null || t > curMax) maxTemp = t
                }
                "POP" -> item.fcstValue.toIntOrNull()?.let { p ->
                    if (isAm) amPop = maxOf(amPop ?: p, p) else pmPop = maxOf(pmPop ?: p, p)
                }
                "SKY" -> item.fcstValue.toSkyConditionOrNull()?.let { s ->
                    if (isAm) amSky = s else pmSky = s
                }
                "PTY" -> item.fcstValue.toPrecipitationTypeOrNull()?.let { p ->
                    if (isAm) amPty = p else pmPty = p
                }
            }
        }
        DailyForecast(
            date = LocalDate.parse(date, dateFmt),
            minTemp = minTemp,
            maxTemp = maxTemp,
            amPrecipProb = amPop, pmPrecipProb = pmPop,
            amSky = amSky, pmSky = pmSky,
            amPrecipitationType = amPty, pmPrecipitationType = pmPty
        )
    }.sortedBy { it.date }.take(3)
}

// ── 공통 유틸 ─────────────────────────────────────────────────────────────────

/**
 * 체감온도. 기온이 없으면 null.
 * - 기온 ≤10℃: 풍속이 있어야 판단할 수 있다 (≥1.3m/s이면 Wind Chill, 아니면 기온). 풍속이 없으면 null
 * - 기온 ≥27℃: 습도가 있어야 Heat Index를 계산할 수 있다. 습도가 없으면 null
 * - 그 외: 기온
 */
internal fun calculateFeelsLike(temp: Double?, windSpeed: Double?, humidity: Int?): Double? {
    if (temp == null) return null
    return when {
        temp <= 10 -> when {
            windSpeed == null -> null
            windSpeed >= 1.3 -> {
                val ws016 = Math.pow(windSpeed, 0.16)
                13.12 + 0.6215 * temp - 11.37 * ws016 + 0.3965 * temp * ws016
            }
            else -> temp
        }
        temp >= 27 ->
            if (humidity == null) null
            else temp + 0.33 * (humidity / 100.0 * 6.105 * Math.exp(17.27 * temp / (237.7 + temp))) - 4.0
        else -> temp
    }
}
