package com.seriouschoi.aircheck.core.kma

import com.seriouschoi.aircheck.core.domain.model.WeatherCondition
import com.seriouschoi.aircheck.core.kma.dto.NcstItem
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * parseCurrentWeather 테스트. 값이 없을 때 0이나 맑음으로 채우지 않고 null이어야 한다.
 */
class ParseCurrentWeatherTest {

    private fun item(category: String, value: String) = NcstItem(
        baseDate = "20260320", baseTime = "1300",
        category = category, obsrValue = value, nx = 60, ny = 127
    )

    @Test
    fun `모든 항목이 있으면 값을 그대로 담는다`() {
        val result = parseCurrentWeather(
            listOf(
                item("T1H", "20.7"), item("REH", "49"), item("RN1", "0"),
                item("WSD", "1.8"), item("VEC", "158"), item("PTY", "0")
            )
        )

        assertEquals(20.7, result.temperature)
        assertEquals(49, result.humidity)
        assertEquals(0.0, result.precipitation)
        assertEquals(1.8, result.windSpeed)
        assertEquals(158, result.windDirection)
    }

    @Test
    fun `항목이 없으면 0으로 채우지 않고 null이다`() {
        val result = parseCurrentWeather(emptyList())

        assertNull(result.temperature)
        assertNull(result.feelsLike)
        assertNull(result.precipitation)
        assertNull(result.humidity)
        assertNull(result.windSpeed)
        assertNull(result.windDirection)
        assertNull(result.weatherCondition)
    }

    @Test
    fun `숫자로 읽을 수 없는 값은 null이다`() {
        val result = parseCurrentWeather(listOf(item("T1H", "abc"), item("REH", "-")))

        assertNull(result.temperature)
        assertNull(result.humidity)
    }

    @Test
    fun `비나 눈이 오면 날씨를 알 수 있다`() {
        assertEquals(WeatherCondition.RAIN, parseCurrentWeather(listOf(item("PTY", "1"))).weatherCondition)
        assertEquals(WeatherCondition.RAIN, parseCurrentWeather(listOf(item("PTY", "5"))).weatherCondition)
        assertEquals(WeatherCondition.SNOW, parseCurrentWeather(listOf(item("PTY", "3"))).weatherCondition)
        assertEquals(WeatherCondition.SNOW, parseCurrentWeather(listOf(item("PTY", "7"))).weatherCondition)
    }

    @Test
    fun `강수가 없으면 하늘상태를 알 수 없으므로 날씨는 null이다`() {
        // 초단기실황에는 SKY가 없다. 기존에는 PTY=0이면 항상 CLEAR(맑음)로 내려갔다
        assertNull(parseCurrentWeather(listOf(item("PTY", "0"))).weatherCondition)
    }
}
