package com.seriouschoi.aircheck.core.kma

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * calculateFeelsLike 테스트. 계산에 필요한 값이 없으면 기본값을 가정하지 않고 null이어야 한다.
 */
class CalculateFeelsLikeTest {

    @Test
    fun `기온이 없으면 null이다`() {
        assertNull(calculateFeelsLike(null, 2.0, 50))
    }

    @Test
    fun `추울 때 풍속이 있으면 Wind Chill을 계산한다`() {
        val result = calculateFeelsLike(0.0, 5.0, null)

        assertNotNull(result)
        assert(result!! < 0.0)
    }

    @Test
    fun `추울 때 바람이 약하면 기온 그대로다`() {
        assertEquals(5.0, calculateFeelsLike(5.0, 1.0, null))
    }

    @Test
    fun `추울 때 풍속이 없으면 판단할 수 없어 null이다`() {
        assertNull(calculateFeelsLike(5.0, null, 50))
    }

    @Test
    fun `더울 때 습도가 있으면 Heat Index를 계산한다`() {
        val result = calculateFeelsLike(32.0, null, 70)

        assertNotNull(result)
        assert(result!! > 32.0)
    }

    @Test
    fun `더울 때 습도가 없으면 null이다`() {
        assertNull(calculateFeelsLike(32.0, 2.0, null))
    }

    @Test
    fun `보통 기온이면 풍속 습도 없이도 기온 그대로다`() {
        assertEquals(18.0, calculateFeelsLike(18.0, null, null))
    }
}
