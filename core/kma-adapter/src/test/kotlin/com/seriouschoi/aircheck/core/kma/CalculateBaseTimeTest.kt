package com.seriouschoi.aircheck.core.kma

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

/**
 * 발표시각 계산 테스트. 시각은 인자로 고정해서 넘기고, 발표가 바뀌는 경계와 날짜가 넘어가는 경계를 확인한다.
 */
class CalculateBaseTimeTest {

    // ── 초단기실황 (매시 정각 발표, 40분 이후 제공) ──

    @Test
    fun `초단기실황은, 40분 전이면 한 시간 전 발표를 쓴다`() {
        val result = calculateBaseDateTime(at(2026, 11, 12, 12, 39))

        assertEquals("20261112" to "1100", result)
    }

    @Test
    fun `초단기실황은, 40분 이후면 이번 시각 발표를 쓴다`() {
        val result = calculateBaseDateTime(at(2026, 11, 12, 12, 40))

        assertEquals("20261112" to "1200", result)
    }

    @Test
    fun `초단기실황은, 자정 직후 40분 전이면 전날 23시 발표를 쓴다`() {
        val result = calculateBaseDateTime(at(2026, 11, 13, 0, 10))

        assertEquals("20261112" to "2300", result)
    }

    // ── 단기예보 (02·05·08·11·14·17·20·23시 발표) ──

    @Test
    fun `단기예보는, 발표 후 한 시간이 지나면 그 발표를 쓴다`() {
        val result = calculateVilageFcstBaseDateTime(at(2026, 11, 12, 15, 0))

        assertEquals("20261112" to "1400", result)
    }

    @Test
    fun `단기예보는, 발표 후 한 시간이 지나기 전이면 이전 발표를 쓴다`() {
        val result = calculateVilageFcstBaseDateTime(at(2026, 11, 12, 14, 59))

        assertEquals("20261112" to "1100", result)
    }

    @Test
    fun `단기예보는, 새벽 02시 발표 전이면 전날 23시 발표를 쓴다`() {
        val result = calculateVilageFcstBaseDateTime(at(2026, 11, 13, 0, 30))

        assertEquals("20261112" to "2300", result)
    }

    @Test
    fun `단기예보는, 23시 발표 후 한 시간이 지나기 전이면 20시 발표를 쓴다`() {
        val result = calculateVilageFcstBaseDateTime(at(2026, 11, 12, 23, 30))

        assertEquals("20261112" to "2000", result)
    }

    // ── 중기예보 (06시·18시 발표) ──

    @Test
    fun `중기예보는, 06시 전이면 전날 18시 발표를 쓴다`() {
        val result = calculateMidFcstBase(at(2026, 11, 13, 5, 59))

        assertEquals(12, result.dayOfMonth)
        assertEquals(18, result.hour)
    }

    @Test
    fun `중기예보는, 06시 이후 18시 전이면 오늘 06시 발표를 쓴다`() {
        val result = calculateMidFcstBase(at(2026, 11, 12, 17, 59))

        assertEquals(6, result.hour)
        assertEquals(12, result.dayOfMonth)
    }

    @Test
    fun `중기예보는, 18시 이후면 오늘 18시 발표를 쓴다`() {
        val result = calculateMidFcstBase(at(2026, 11, 12, 18, 0))

        assertEquals(18, result.hour)
        assertEquals(12, result.dayOfMonth)
    }

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int) =
        LocalDateTime.of(year, month, day, hour, minute)
}
