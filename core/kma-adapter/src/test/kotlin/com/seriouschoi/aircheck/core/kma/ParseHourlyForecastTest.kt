package com.seriouschoi.aircheck.core.kma

import com.seriouschoi.aircheck.core.domain.model.WeatherCondition
import com.seriouschoi.aircheck.core.kma.dto.FcstItem
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * parseExtendedHourlyForecast 테스트. 값이 없을 때 기본값으로 채우지 않고 null이어야 한다.
 */
class ParseHourlyForecastTest {

    private fun item(category: String, value: String, time: String = "0900") = FcstItem(
        baseDate = "20260320", baseTime = "0500",
        category = category, fcstDate = "20260320", fcstTime = time, fcstValue = value,
        nx = 60, ny = 127
    )

    @Test
    fun `모든 항목이 있으면 값을 그대로 담는다`() {
        val result = parseExtendedHourlyForecast(
            listOf(
                item("TMP", "15.0"), item("SKY", "4"), item("PTY", "0"), item("POP", "30"),
                item("WSD", "2.0"), item("REH", "60"), item("SNO", "0.5")
            )
        ).single()

        assertEquals(15.0, result.temperature)
        assertEquals(30, result.precipitationProbability)
        assertEquals(0.5, result.snowfall)
        assertEquals(WeatherCondition.CLOUDY, result.weatherCondition)
        assertEquals(15.0, result.feelsLike)
    }

    @Test
    fun `일부 항목만 있으면 있는 값만 담고 나머지는 null이다`() {
        val result = parseExtendedHourlyForecast(listOf(item("POP", "30"))).single()

        assertEquals(30, result.precipitationProbability)
        assertNull(result.temperature)
        assertNull(result.feelsLike)
        assertNull(result.snowfall)
        assertNull(result.weatherCondition)
    }

    @Test
    fun `하늘상태나 강수형태 중 하나만 있으면 날씨는 null이다`() {
        val result = parseExtendedHourlyForecast(listOf(item("SKY", "1"))).single()

        assertNull(result.weatherCondition)
    }

    @Test
    fun `SNO가 범주 문자열이면 적설은 null이다`() {
        val result = parseExtendedHourlyForecast(listOf(item("SNO", "적설없음"))).single()

        assertNull(result.snowfall)
    }

    @Test
    fun `시각별로 나뉘고 시각순으로 정렬된다`() {
        val result = parseExtendedHourlyForecast(
            listOf(item("TMP", "12.0", time = "1000"), item("TMP", "10.0", time = "0900"))
        )

        assertEquals(listOf(10.0, 12.0), result.map { it.temperature })
    }
}
