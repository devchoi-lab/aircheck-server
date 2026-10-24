package com.seriouschoi.aircheck.core.airkorea

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.format.DateTimeParseException

class KstTimeUtilsTest {

    @Test
    fun `일반 시각 KST to UTC 변환`() {
        // KST 2026-03-20 09:00 = UTC 2026-03-20 00:00
        val result = "2026-03-20 09:00".kstToUtcIso()
        assertEquals("2026-03-20T00:00:00Z", result)
    }

    @Test
    fun `자정 표기 24-00 보정 후 변환`() {
        // 에어코리아 "2026-10-20 24:00" = KST 2026-10-21 00:00 = UTC 2026-10-20 15:00
        val result = "2026-10-20 24:00".kstToUtcIso()
        assertEquals("2026-10-20T15:00:00Z", result)
    }

    @Test
    fun `월말 24-00 날짜 이월`() {
        // KST "2026-01-31 24:00" = KST 2026-02-01 00:00 = UTC 2026-01-31 15:00
        val result = "2026-01-31 24:00".kstToUtcIso()
        assertEquals("2026-01-31T15:00:00Z", result)
    }

    @Test
    fun `잘못된 형식은 예외 발생`() {
        assertThrows<DateTimeParseException> {
            "2026-03-20 25:00".kstToUtcIso()
        }
    }
}
