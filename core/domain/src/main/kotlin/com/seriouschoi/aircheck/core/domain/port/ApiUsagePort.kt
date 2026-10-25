package com.seriouschoi.aircheck.core.domain.port

import java.time.LocalDate
import java.time.LocalDateTime

/**
 * API 사용량 추적 포트
 */
interface ApiUsagePort {

    /**
     * 성공한 API 호출 기록
     */
    fun recordSuccess(apiType: String, responseTimeMs: Long)

    /**
     * 실패한 API 호출 기록
     */
    fun recordFailure(apiType: String, errorMessage: String? = null)

    /**
     * 오늘 호출 수 조회
     */
    fun getTodayCount(apiType: String): Long

    /**
     * 오늘 통계 조회
     */
    fun getTodayStats(): List<ApiUsageStats>

    /**
     * 기간별 통계 조회
     */
    fun getStats(startDate: LocalDate, endDate: LocalDate): List<ApiUsageStats>

    /**
     * 시간대별 통계
     */
    fun getHourlyStats(date: LocalDate): List<HourlyStats>

    /**
     * 오래된 로그 삭제
     */
    fun cleanupOldLogs(retentionDays: Int): Int
}

/**
 * API 사용량 통계
 */
data class ApiUsageStats(
    val apiType: String,
    val date: LocalDate,
    val callCount: Long,
    val successCount: Long,
    val failCount: Long,
    val avgResponseTimeMs: Long
) {
    val successRate: Double
        get() = if (callCount > 0) successCount.toDouble() / callCount * 100 else 0.0
}

/**
 * 시간대별 통계
 */
data class HourlyStats(
    val hour: Int,
    val callCount: Long,
    val successCount: Long,
    val avgResponseTimeMs: Double
)
