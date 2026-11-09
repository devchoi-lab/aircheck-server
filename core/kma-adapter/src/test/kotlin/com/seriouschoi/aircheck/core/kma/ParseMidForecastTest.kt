package com.seriouschoi.aircheck.core.kma

import com.seriouschoi.aircheck.core.domain.model.MidLandWeather
import com.seriouschoi.aircheck.core.domain.model.MidLandWeatherType
import com.seriouschoi.aircheck.core.kma.dto.MidLandFcstItem
import com.seriouschoi.aircheck.core.kma.dto.MidTaItem
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDate

/**
 * 중기예보 파싱 테스트. 값이 없을 때 0이나 빈 문자열로 채우지 않고 null이어야 한다.
 */
class ParseMidForecastTest {

    private val baseDate = LocalDate.of(2026, 3, 20)

    @Test
    fun `중기육상예보는 값이 있으면 그대로 담고 날짜는 발표일 기준이다`() {
        val result = parseMidLandForecast(
            MidLandFcstItem(regId = "11B00000", rnSt3Am = 10, rnSt3Pm = 20, wf3Am = "맑음", wf3Pm = "구름많음"),
            baseDate
        )

        val day3 = result.days.first()
        assertEquals(LocalDate.of(2026, 3, 23), day3.date)
        assertEquals(10, day3.amPrecipProb)
        assertEquals(20, day3.pmPrecipProb)
        assertEquals(MidLandWeather(MidLandWeatherType.CLEAR, "맑음"), day3.amWeather)
        assertEquals(MidLandWeather(MidLandWeatherType.MOSTLY_CLOUDY, "구름많음"), day3.pmWeather)
        assertEquals(8, result.days.size)
    }

    @Test
    fun `중기육상예보는 값이 없으면 0이나 빈 문자열이 아니라 null이다`() {
        val result = parseMidLandForecast(MidLandFcstItem(regId = "11B00000"), baseDate)

        result.days.forEach {
            assertNull(it.amPrecipProb)
            assertNull(it.pmPrecipProb)
            assertNull(it.amWeather)
            assertNull(it.pmWeather)
        }
    }

    @Test
    fun `중기육상예보 8일 이후는 단일값을 오전 오후에 공통으로 쓴다`() {
        val result = parseMidLandForecast(
            MidLandFcstItem(regId = "11B00000", rnSt8 = 40, wf8 = "흐림"),
            baseDate
        )

        val day8 = result.days.single { it.date == LocalDate.of(2026, 3, 28) }
        assertEquals(40, day8.amPrecipProb)
        assertEquals(40, day8.pmPrecipProb)
        assertEquals(MidLandWeather(MidLandWeatherType.OVERCAST, "흐림"), day8.amWeather)
        assertEquals(MidLandWeather(MidLandWeatherType.OVERCAST, "흐림"), day8.pmWeather)
    }

    @Test
    fun `중기육상예보 날씨 문서의 모든 문구가 UNKNOWN이 아닌 타입이 된다`() {
        val texts = listOf(
            "맑음", "구름많음", "구름많고 비", "구름많고 눈", "구름많고 비/눈", "구름많고 소나기",
            "흐림", "흐리고 비", "흐리고 눈", "흐리고 비/눈", "흐리고 소나기"
        )
        texts.forEach { text ->
            val result = parseMidLandForecast(MidLandFcstItem(regId = "11B00000", wf3Am = text), baseDate)
            val weather = result.days.first().amWeather
            assertEquals(text, weather?.text)
            assertNotEquals(MidLandWeatherType.UNKNOWN, weather?.type, text)
        }
        assertEquals(texts.size, texts.map { MidLandWeatherType.fromKmaText(it) }.toSet().size)
    }

    @Test
    fun `문서에 없는 문구는 UNKNOWN이고 원문이 그대로 남는다`() {
        val result = parseMidLandForecast(
            MidLandFcstItem(regId = "11B00000", wf3Am = "흐리고 우박"),
            baseDate
        )

        assertEquals(MidLandWeather(MidLandWeatherType.UNKNOWN, "흐리고 우박"), result.days.first().amWeather)
    }

    @Test
    fun `앞뒤 공백은 타입을 찾을 때만 무시하고 원문은 그대로 담는다`() {
        val result = parseMidLandForecast(MidLandFcstItem(regId = "11B00000", wf3Am = " 맑음 "), baseDate)

        assertEquals(MidLandWeather(MidLandWeatherType.CLEAR, " 맑음 "), result.days.first().amWeather)
    }

    @Test
    fun `빈 문자열과 공백만 있는 값은 가져오지 못한 값이므로 null이다`() {
        val result = parseMidLandForecast(
            MidLandFcstItem(regId = "11B00000", wf3Am = "", wf3Pm = "  "),
            baseDate
        )

        assertNull(result.days.first().amWeather)
        assertNull(result.days.first().pmWeather)
    }

    @Test
    fun `중기기온예보는 영하 기온과 0도를 그대로 담는다`() {
        val result = parseMidTaForecast(
            MidTaItem(regId = "11B10101", taMin3 = -5, taMax3 = 0),
            baseDate
        )

        val day3 = result.days.first()
        assertEquals(-5, day3.minTemp)
        assertEquals(0, day3.maxTemp)
    }

    @Test
    fun `중기기온예보는 값이 없으면 0도가 아니라 null이다`() {
        val result = parseMidTaForecast(MidTaItem(regId = "11B10101"), baseDate)

        result.days.forEach {
            assertNull(it.minTemp)
            assertNull(it.maxTemp)
        }
    }
}
