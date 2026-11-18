package com.seriouschoi.aircheck.core.kma

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * calculateFeelsLike 테스트. 기상청 기상자료개방포털의 산출식을 따른다.
 * https://data.kma.go.kr/climate/windChill/selectWindChillChart.do
 *
 * 기대값은 코드 밖에서 같은 산출식으로 따로 계산한 값이다. (풍속은 m/s로 입력하고 공식에서는 km/h로 쓴다.)
 * 계산에 필요한 값이 없거나 산출 조건 밖이면 기본값을 가정하지 않고 null이어야 한다.
 */
class CalculateFeelsLikeTest {

    // ── 겨울철 (10~4월): 기온 10℃ 이하, 풍속 1.3m/s 이상일 때만 산출 ─────────────────

    @Test
    fun `겨울철 기온 0도 풍속 5m_s는, 풍속을 km_h로 바꿔서 계산한다`() {
        // 5m/s = 18km/h. 풍속 5를 그대로 넣으면 -1.59로 3도 넘게 달라진다.
        assertEquals(-4.94, calculateFeelsLike(JANUARY, 0.0, 5.0, null)!!, TOLERANCE)
    }

    @Test
    fun `겨울철 기온 영하 10도 풍속 10m_s는, 체감온도를 계산한다`() {
        assertEquals(-20.30, calculateFeelsLike(JANUARY, -10.0, 10.0, null)!!, TOLERANCE)
    }

    @Test
    fun `겨울철 기온 10도 풍속 1_3m_s는, 산출 조건 경계라 계산한다`() {
        assertEquals(9.86, calculateFeelsLike(JANUARY, 10.0, 1.3, null)!!, TOLERANCE)
    }

    @Test
    fun `겨울철 기온이 10도를 넘으면, 산출하지 않고 null이다`() {
        assertNull(calculateFeelsLike(JANUARY, 10.1, 5.0, 50))
    }

    @Test
    fun `겨울철 풍속이 1_3m_s 미만이면, 산출하지 않고 null이다`() {
        assertNull(calculateFeelsLike(JANUARY, 5.0, 1.29, 50))
    }

    @Test
    fun `겨울철 기온이 10도 이하인데 풍속이 없으면, 판단할 수 없어 null이다`() {
        assertNull(calculateFeelsLike(JANUARY, 5.0, null, 50))
    }

    // ── 여름철 (5~9월): 기온과 습도로 계산 ──────────────────────────────────────────

    @Test
    fun `여름철 기온 32도 습도 70퍼센트는, 습구온도를 쓴 기상청 식으로 계산한다`() {
        assertEquals(33.32, calculateFeelsLike(JULY, 32.0, null, 70)!!, TOLERANCE)
    }

    @Test
    fun `여름철 기온 35도 습도 60퍼센트는, 체감온도를 계산한다`() {
        assertEquals(35.46, calculateFeelsLike(JULY, 35.0, null, 60)!!, TOLERANCE)
    }

    @Test
    fun `여름철에는 기온이 낮아도 여름 식으로 계산한다`() {
        // 기온으로 계절을 나누지 않고 월로 나눈다.
        assertEquals(18.55, calculateFeelsLike(JULY, 18.0, null, 60)!!, TOLERANCE)
    }

    @Test
    fun `여름철 습도가 없으면, 계산할 수 없어 null이다`() {
        assertNull(calculateFeelsLike(JULY, 32.0, 2.0, null))
    }

    // ── 계절 경계 ───────────────────────────────────────────────────────────────

    @Test
    fun `4월은 겨울철이고 5월은 여름철이다`() {
        // 같은 입력(기온 20도, 풍속 2m/s, 습도 50%)이 4월에는 산출 조건 밖이고 5월에는 여름 식으로 계산된다.
        assertNull(calculateFeelsLike(4, 20.0, 2.0, 50))
        assertEquals(19.80, calculateFeelsLike(5, 20.0, 2.0, 50)!!, TOLERANCE)
    }

    @Test
    fun `9월은 여름철이고 10월은 겨울철이다`() {
        assertEquals(19.80, calculateFeelsLike(9, 20.0, 2.0, 50)!!, TOLERANCE)
        assertNull(calculateFeelsLike(10, 20.0, 2.0, 50))
    }

    @Test
    fun `기온이 없으면 계절과 상관없이 null이다`() {
        assertNull(calculateFeelsLike(JANUARY, null, 5.0, 50))
        assertNull(calculateFeelsLike(JULY, null, 5.0, 50))
    }

    private companion object {
        const val JANUARY = 1
        const val JULY = 7
        const val TOLERANCE = 0.01
    }
}
