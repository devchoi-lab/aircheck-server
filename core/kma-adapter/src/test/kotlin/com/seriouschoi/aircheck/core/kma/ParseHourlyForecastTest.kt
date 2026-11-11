package com.seriouschoi.aircheck.core.kma

import com.seriouschoi.aircheck.core.domain.model.PrecipitationType
import com.seriouschoi.aircheck.core.domain.model.SkyCondition
import com.seriouschoi.aircheck.core.kma.dto.FcstDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * parseExtendedHourlyForecast 테스트. 값이 없을 때 기본값으로 채우지 않고 null이어야 한다.
 */
class ParseHourlyForecastTest {

    private fun item(category: String, value: String, time: String = "0900") = FcstDto(
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
        assertEquals(SkyCondition.OVERCAST, result.sky)
        assertEquals(PrecipitationType.NONE, result.precipitationType)
        assertEquals(15.0, result.feelsLike)
    }

    @Test
    fun `일부 항목만 있으면 있는 값만 담고 나머지는 null이다`() {
        val result = parseExtendedHourlyForecast(listOf(item("POP", "30"))).single()

        assertEquals(30, result.precipitationProbability)
        assertNull(result.temperature)
        assertNull(result.feelsLike)
        assertNull(result.snowfall)
        assertNull(result.sky)
        assertNull(result.precipitationType)
    }

    @Test
    fun `하늘상태와 강수형태는 서로 영향을 주지 않는다`() {
        val onlySky = parseExtendedHourlyForecast(listOf(item("SKY", "1"))).single()
        assertEquals(SkyCondition.CLEAR, onlySky.sky)
        assertNull(onlySky.precipitationType)

        val onlyPty = parseExtendedHourlyForecast(listOf(item("PTY", "4"))).single()
        assertNull(onlyPty.sky)
        assertEquals(PrecipitationType.SHOWER, onlyPty.precipitationType)
    }

    @Test
    fun `정의되지 않은 코드는 UNKNOWN이고 결측값은 null이다`() {
        val unknown = parseExtendedHourlyForecast(listOf(item("SKY", "2"), item("PTY", "9"))).single()
        assertEquals(SkyCondition.UNKNOWN, unknown.sky)
        assertEquals(PrecipitationType.UNKNOWN, unknown.precipitationType)

        val missing = parseExtendedHourlyForecast(listOf(item("SKY", "-999"), item("PTY", "-999"))).single()
        assertNull(missing.sky)
        assertNull(missing.precipitationType)
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
