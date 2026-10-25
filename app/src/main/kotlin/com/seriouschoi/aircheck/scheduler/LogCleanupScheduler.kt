package com.seriouschoi.aircheck.scheduler

import com.seriouschoi.aircheck.core.domain.port.ApiUsagePort
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class LogCleanupScheduler(
    private val apiUsagePort: ApiUsagePort
) {
    private val log = LoggerFactory.getLogger(javaClass)
    
    companion object {
        const val RETENTION_DAYS = 30
    }
    
    /**
     * 매일 새벽 3시에 30일 지난 로그 삭제
     */
    @Scheduled(cron = "0 0 3 * * *")
    fun cleanupOldLogs() {
        log.info("API 로그 정리 시작 (보관 기간: ${RETENTION_DAYS}일)")
        
        try {
            val deletedCount = apiUsagePort.cleanupOldLogs(RETENTION_DAYS)
            log.info("API 로그 정리 완료: ${deletedCount}건 삭제")
        } catch (e: Exception) {
            log.error("API 로그 정리 실패: ${e.message}", e)
        }
    }
}
