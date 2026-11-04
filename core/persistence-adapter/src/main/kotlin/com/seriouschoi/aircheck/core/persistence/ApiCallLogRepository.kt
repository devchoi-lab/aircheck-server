package com.seriouschoi.aircheck.core.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime

interface ApiCallLogRepository : JpaRepository<ApiCallLogEntity, Long> {
    
    // 오래된 로그 삭제 (30일 전)
    @Modifying
    @Query("DELETE FROM ApiCallLogEntity e WHERE e.calledAt < :before")
    fun deleteByCalledAtBefore(before: LocalDateTime): Int
    
    // 기간별 조회
    fun findByCalledAtBetween(start: LocalDateTime, end: LocalDateTime): List<ApiCallLogEntity>
    
    // API 타입별 + 기간별 조회
    fun findByApiTypeAndCalledAtBetween(
        apiType: String,
        start: LocalDateTime,
        end: LocalDateTime
    ): List<ApiCallLogEntity>
    
    // 오늘 호출 수
    @Query("""
        SELECT COUNT(e) FROM ApiCallLogEntity e 
        WHERE e.apiType = :apiType 
        AND e.calledAt >= :startOfDay
    """)
    fun countTodayByApiType(apiType: String, startOfDay: LocalDateTime): Long
    
    // 시간대별 통계 (네이티브 쿼리)
    @Query("""
        SELECT 
            HOUR(e.calledAt) as hour,
            COUNT(e) as count,
            SUM(CASE WHEN e.success = true THEN 1 ELSE 0 END) as successCount,
            AVG(e.responseTimeMs) as avgResponseTime
        FROM ApiCallLogEntity e
        WHERE e.calledAt >= :start AND e.calledAt < :end
        GROUP BY HOUR(e.calledAt)
        ORDER BY hour
    """)
    fun getHourlyStats(start: LocalDateTime, end: LocalDateTime): List<Array<Any>>
}
