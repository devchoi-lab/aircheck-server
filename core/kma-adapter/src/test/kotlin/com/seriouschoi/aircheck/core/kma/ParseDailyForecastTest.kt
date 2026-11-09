package com.seriouschoi.aircheck.core.kma

import com.seriouschoi.aircheck.core.domain.model.PrecipitationType
import com.seriouschoi.aircheck.core.domain.model.SkyCondition
import com.seriouschoi.aircheck.core.kma.dto.FcstItem
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDate

/**
 * parseDailyForecast 테스트. 값이 없을 때 기본값으로 채우지 않고 null이어야 한다.
 */
class ParseDailyForecastTest {

    private fun item(category: String, time: String, value: String, date: String = "20260320") = FcstItem(
        baseDate = "20260320", baseTime = "0200",
        category = category, fcstDate = date, fcstTime = time, fcstValue = value,
        nx = 60, ny = 127
    )

    @Test
    fun `TMN TMX가 있으면 TMP보다 우선하지 않고 TMN TMX 값을 쓴다`() {
        val result = parseDailyForecast(
            listOf(
                item("TMP", "0600", "5.0"),
                item("TMP", "1500", "15.0"),
                item("TMN", "0600", "3.0"),
                item("TMX", "1500", "18.0")
            )
        ).single()

        assertEquals(3.0, result.minTemp)
        assertEquals(18.0, result.maxTemp)
    }

    @Test
    fun `TMN TMX가 없으면 TMP 최솟값 최댓값을 쓴다`() {
        val result = parseDailyForecast(
            listOf(
                item("TMP", "0600", "5.0"),
                item("TMP", "1500", "15.0"),
                item("TMP", "2100", "9.0")
            )
        ).single()

        assertEquals(5.0, result.minTemp)
        assertEquals(15.0, result.maxTemp)
    }

    @Test
    fun `최고기온이 영하여도 0이 되지 않는다`() {
        // 기존에는 maxTemp 초기값이 Double.MIN_VALUE(가장 작은 양수)라 0.0으로 내려갔다
        val result = parseDailyForecast(
            listOf(
                item("TMP", "0600", "-9.0"),
                item("TMP", "1500", "-3.0")
            )
        ).single()

        assertEquals(-9.0, result.minTemp)
        assertEquals(-3.0, result.maxTemp)
    }

    @Test
    fun `기온 항목이 없으면 최저 최고기온은 null이다`() {
        val result = parseDailyForecast(listOf(item("POP", "0600", "30"))).single()

        assertNull(result.minTemp)
        assertNull(result.maxTemp)
    }

    @Test
    fun `숫자로 읽을 수 없는 값은 0으로 채우지 않고 건너뛴다`() {
        val result = parseDailyForecast(
            listOf(
                item("TMP", "0600", "abc"),
                item("POP", "0600", "abc")
            )
        ).single()

        assertNull(result.minTemp)
        assertNull(result.maxTemp)
        assertNull(result.amPrecipProb)
    }

    @Test
    fun `오전 항목이 없으면 오전 값은 null이고 오후 값은 그대로 나온다`() {
        val result = parseDailyForecast(
            listOf(
                item("POP", "1500", "60"),
                item("POP", "1800", "40"),
                item("SKY", "1800", "4"),
                item("PTY", "1800", "0")
            )
        ).single()

        assertNull(result.amPrecipProb)
        assertNull(result.amSky)
        assertNull(result.amPrecipitationType)
        assertEquals(60, result.pmPrecipProb)
        assertEquals(SkyCondition.OVERCAST, result.pmSky)
        assertEquals(PrecipitationType.NONE, result.pmPrecipitationType)
    }

    @Test
    fun `오전 강수확률은 오전 시간대의 최댓값이고 강수확률 0은 null과 구분된다`() {
        val result = parseDailyForecast(
            listOf(
                item("POP", "0600", "20"),
                item("POP", "0900", "50"),
                item("POP", "1500", "0")
            )
        ).single()

        assertEquals(50, result.amPrecipProb)
        assertEquals(0, result.pmPrecipProb)
    }

    @Test
    fun `하늘상태와 강수형태는 서로 영향을 주지 않는다`() {
        val result = parseDailyForecast(
            listOf(
                item("SKY", "0900", "1"),
                item("PTY", "1500", "0")
            )
        ).single()

        assertEquals(SkyCondition.CLEAR, result.amSky)
        assertNull(result.amPrecipitationType)
        assertNull(result.pmSky)
        assertEquals(PrecipitationType.NONE, result.pmPrecipitationType)
    }

    @Test
    fun `결측값은 앞서 읽은 값을 지우지 않고 정의되지 않은 코드는 UNKNOWN이다`() {
        val missing = parseDailyForecast(
            listOf(item("PTY", "0600", "1"), item("PTY", "0900", "-999"))
        ).single()
        assertEquals(PrecipitationType.RAIN, missing.amPrecipitationType)

        val unknown = parseDailyForecast(listOf(item("SKY", "0600", "9"))).single()
        assertEquals(SkyCondition.UNKNOWN, unknown.amSky)
    }

    @Test
    fun `날짜별로 나뉘고 날짜순으로 정렬된다`() {
        val result = parseDailyForecast(
            listOf(
                item("TMP", "0600", "10.0", date = "20260321"),
                item("TMP", "0600", "5.0", date = "20260320")
            )
        )

        assertEquals(listOf(LocalDate.of(2026, 3, 20), LocalDate.of(2026, 3, 21)), result.map { it.date })
    }
}
