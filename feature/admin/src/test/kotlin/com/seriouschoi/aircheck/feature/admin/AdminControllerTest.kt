package com.seriouschoi.aircheck.feature.admin

import com.seriouschoi.aircheck.core.domain.port.ApiUsagePort
import com.seriouschoi.aircheck.core.domain.port.ApiUsageStats
import com.seriouschoi.aircheck.core.domain.port.HourlyStats
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDate

/**
 * AdminController 단위 테스트
 * 
 * 계약:
 * - 유효한 API 키로만 접근 가능
 * - 잘못된 키 → 401 Unauthorized
 * - 각 엔드포인트가 올바른 데이터 반환
 */
class AdminControllerTest {

    private lateinit var apiUsagePort: ApiUsagePort
    private lateinit var controller: AdminController

    private val validApiKey = "test-api-key"

    @BeforeEach
    fun setup() {
        apiUsagePort = mockk()
        controller = AdminController(apiUsagePort, validApiKey)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // API 키 검증 테스트
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `유효하지 않은 API 키 - 401 에러`() {
        // Given
        every { apiUsagePort.getTodayStats() } returns emptyList()

        // When & Then
        val exception = assertThrows<ResponseStatusException> {
            controller.getTodayUsage("wrong-key")
        }
        assertEquals(401, exception.statusCode.value())
    }

    @Test
    fun `null API 키 - 401 에러`() {
        // When & Then
        val exception = assertThrows<ResponseStatusException> {
            controller.getTodayUsage(null)
        }
        assertEquals(401, exception.statusCode.value())
    }

    @Test
    fun `유효한 API 키 - 정상 처리`() {
        // Given
        every { apiUsagePort.getTodayStats() } returns emptyList()

        // When
        val result = controller.getTodayUsage(validApiKey)

        // Then
        assertNotNull(result)
        assertEquals(LocalDate.now(), result.date)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // getTodayUsage 테스트
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `getTodayUsage - 통계 포함`() {
        // Given
        val stats = listOf(
            ApiUsageStats(
                apiType = "AIR_KOREA",
                date = LocalDate.now(),
                callCount = 100,
                successCount = 95,
                failCount = 5,
                avgResponseTimeMs = 200
            )
        )
        every { apiUsagePort.getTodayStats() } returns stats

        // When
        val result = controller.getTodayUsage(validApiKey)

        // Then
        assertEquals(1, result.stats.size)
        assertEquals("AIR_KOREA", result.stats[0].apiType)
        assertEquals(100, result.stats[0].callCount)
    }

    @Test
    fun `getTodayUsage - API 한도 포함`() {
        // Given
        every { apiUsagePort.getTodayStats() } returns emptyList()

        // When
        val result = controller.getTodayUsage(validApiKey)

        // Then
        assertTrue(result.limits.containsKey("AIR_KOREA"))
        assertTrue(result.limits.containsKey("OPEN_METEO"))
        assertEquals(500L, result.limits["AIR_KOREA"])
        assertEquals(10_000L, result.limits["OPEN_METEO"])
    }

    // ══════════════════════════════════════════════════════════════════════════
    // getRemainingCalls 테스트
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `getRemainingCalls - 잔여 호출 계산`() {
        // Given
        val stats = listOf(
            ApiUsageStats(
                apiType = "AIR_KOREA",
                date = LocalDate.now(),
                callCount = 100,
                successCount = 100,
                failCount = 0,
                avgResponseTimeMs = 150
            )
        )
        every { apiUsagePort.getTodayStats() } returns stats

        // When
        val result = controller.getRemainingCalls(validApiKey)

        // Then
        val airKorea = result["AIR_KOREA"]!!
        assertEquals(500L, airKorea.limit)
        assertEquals(100L, airKorea.used)
        assertEquals(400L, airKorea.remaining)
        assertEquals(20.0, airKorea.usagePercent, 0.1)
    }

    @Test
    fun `getRemainingCalls - 사용량 없으면 0`() {
        // Given
        every { apiUsagePort.getTodayStats() } returns emptyList()

        // When
        val result = controller.getRemainingCalls(validApiKey)

        // Then
        val airKorea = result["AIR_KOREA"]!!
        assertEquals(0L, airKorea.used)
        assertEquals(500L, airKorea.remaining)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // getHourlyStats 테스트
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `getHourlyStats - 날짜 지정`() {
        // Given
        val date = LocalDate.of(2026, 3, 19)
        val hourlyStats = listOf(
            HourlyStats(hour = 9, callCount = 50, successCount = 48, avgResponseTimeMs = 100.0)
        )
        every { apiUsagePort.getHourlyStats(date) } returns hourlyStats

        // When
        val result = controller.getHourlyStats(validApiKey, date)

        // Then
        assertEquals(1, result.size)
        assertEquals(9, result[0].hour)
        verify { apiUsagePort.getHourlyStats(date) }
    }

    @Test
    fun `getHourlyStats - 날짜 미지정시 오늘`() {
        // Given
        every { apiUsagePort.getHourlyStats(LocalDate.now()) } returns emptyList()

        // When
        controller.getHourlyStats(validApiKey, null)

        // Then
        verify { apiUsagePort.getHourlyStats(LocalDate.now()) }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // cleanupLogs 테스트
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `cleanupLogs - 삭제 결과 반환`() {
        // Given
        every { apiUsagePort.cleanupOldLogs(30) } returns 150

        // When
        val result = controller.cleanupLogs(validApiKey, 30)

        // Then
        assertEquals(150, result["deletedCount"])
        assertEquals(30, result["retentionDays"])
    }

    @Test
    fun `cleanupLogs - 기본 보관 기간 30일`() {
        // Given
        every { apiUsagePort.cleanupOldLogs(30) } returns 0

        // When
        controller.cleanupLogs(validApiKey, 30)

        // Then
        verify { apiUsagePort.cleanupOldLogs(30) }
    }
}
