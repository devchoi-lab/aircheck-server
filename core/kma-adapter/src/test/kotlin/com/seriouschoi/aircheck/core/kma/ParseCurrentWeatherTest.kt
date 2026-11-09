package com.seriouschoi.aircheck.core.kma

import com.seriouschoi.aircheck.core.domain.model.PrecipitationType
import com.seriouschoi.aircheck.core.kma.dto.NcstItem
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * parseCurrentWeather 테스트. 값이 없을 때 0으로 채우지 않고 null이어야 한다. 강수형태(PTY)는 기상청 값 그대로 담는다.
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
        assertEquals(PrecipitationType.NONE, result.precipitationType)
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
        assertNull(result.precipitationType)
    }

    @Test
    fun `숫자로 읽을 수 없는 값은 null이다`() {
        val result = parseCurrentWeather(listOf(item("T1H", "abc"), item("REH", "-")))

        assertNull(result.temperature)
        assertNull(result.humidity)
    }

    @Test
    fun `강수형태는 기상청 코드 그대로 담는다`() {
        val expected = mapOf(
            "0" to PrecipitationType.NONE, "1" to PrecipitationType.RAIN,
            "2" to PrecipitationType.RAIN_SNOW, "3" to PrecipitationType.SNOW,
            "4" to PrecipitationType.SHOWER, "5" to PrecipitationType.RAINDROP,
            "6" to PrecipitationType.RAINDROP_SNOW_FLURRY, "7" to PrecipitationType.SNOW_FLURRY
        )
        expected.forEach { (code, type) ->
            assertEquals(type, parseCurrentWeather(listOf(item("PTY", code))).precipitationType, "PTY=$code")
        }
    }

    @Test
    fun `강수형태 항목이 없거나 정의되지 않은 코드이면 null이다`() {
        assertNull(parseCurrentWeather(emptyList()).precipitationType)
        assertNull(parseCurrentWeather(listOf(item("PTY", "9"))).precipitationType)
        assertNull(parseCurrentWeather(listOf(item("PTY", "abc"))).precipitationType)
    }
}
