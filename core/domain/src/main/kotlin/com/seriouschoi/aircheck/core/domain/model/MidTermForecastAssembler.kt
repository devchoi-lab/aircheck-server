package com.seriouschoi.aircheck.core.domain.model

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * MidLandForecast + MidTaForecast → List<MidTermForecast> 조합
 *
 * MidLandForecast(강수확률·날씨)와 MidTaForecast(기온)를 dayOffset 기준으로 병합.
 * 어느 한쪽에 없는 dayOffset은 결과에서 제외.
 */
fun assembleMidTermForecast(
    midLand: MidLandForecast,
    midTa: MidTaForecast
): List<MidTermForecast> {
    val taByOffset = midTa.days.associateBy { it.dayOffset }
    val today = LocalDate.now()
    val dayOfWeekFormatter = DateTimeFormatter.ofPattern("E", Locale.KOREAN)

    return midLand.days.mapNotNull { landDay ->
        val taDay = taByOffset[landDay.dayOffset] ?: return@mapNotNull null
        val date = today.plusDays(landDay.dayOffset.toLong())
        MidTermForecast(
            date = "${date.year}-${String.format("%02d", date.monthValue)}-${String.format("%02d", date.dayOfMonth)}",
            dayOfWeek = date.format(dayOfWeekFormatter),
            minTemp = taDay.minTemp,
            maxTemp = taDay.maxTemp,
            amPrecipProb = landDay.amPrecipProb,
            pmPrecipProb = landDay.pmPrecipProb,
            weatherDescription = landDay.amWeather
        )
    }
}
