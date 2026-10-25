package com.seriouschoi.aircheck.core.persistence

import com.seriouschoi.aircheck.core.domain.port.ApiUsagePort
import com.seriouschoi.aircheck.core.domain.port.ApiUsageStats
import com.seriouschoi.aircheck.core.domain.port.HourlyStats
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Component
class ApiCallLogAdapter(
    private val repository: ApiCallLogRepository
) : ApiUsagePort {
    
    @Transactional
    override fun recordSuccess(apiType: String, responseTimeMs: Long) {
        val entity = ApiCallLogEntity(
            apiType = ApiType.valueOf(apiType),
            success = true,
            responseTimeMs = responseTimeMs
        )
        repository.save(entity)
    }
    
    @Transactional
    override fun recordFailure(apiType: String, errorMessage: String?) {
        val entity = ApiCallLogEntity(
            apiType = ApiType.valueOf(apiType),
            success = false,
            errorMessage = errorMessage?.take(500)
        )
        repository.save(entity)
    }
    
    @Transactional(readOnly = true)
    override fun getTodayCount(apiType: String): Long {
        val type = ApiType.valueOf(apiType)
        val startOfDay = LocalDate.now().atStartOfDay()
        return repository.countTodayByApiType(type, startOfDay)
    }
    
    @Transactional(readOnly = true)
    override fun getTodayStats(): List<ApiUsageStats> {
        val today = LocalDate.now()
        return getStats(today, today)
    }
    
    @Transactional(readOnly = true)
    override fun getStats(startDate: LocalDate, endDate: LocalDate): List<ApiUsageStats> {
        val start = startDate.atStartOfDay()
        val end = endDate.plusDays(1).atStartOfDay()
        
        val logs = repository.findByCalledAtBetween(start, end)
        
        // 날짜 + API 타입별로 그룹핑
        return logs
            .groupBy { it.calledAt.toLocalDate() to it.apiType }
            .map { (key, items) ->
                val (date, apiType) = key
                val successItems = items.filter { it.success }
                
                ApiUsageStats(
                    apiType = apiType.name,
                    date = date,
                    callCount = items.size.toLong(),
                    successCount = successItems.size.toLong(),
                    failCount = items.count { !it.success }.toLong(),
                    avgResponseTimeMs = if (successItems.isNotEmpty()) {
                        successItems.map { it.responseTimeMs }.average().toLong()
                    } else 0L
                )
            }
            .sortedWith(compareBy({ it.date }, { it.apiType }))
    }
    
    @Transactional(readOnly = true)
    override fun getHourlyStats(date: LocalDate): List<HourlyStats> {
        val start = date.atStartOfDay()
        val end = date.plusDays(1).atStartOfDay()
        
        return repository.getHourlyStats(start, end).map { row ->
            HourlyStats(
                hour = (row[0] as Number).toInt(),
                callCount = (row[1] as Number).toLong(),
                successCount = (row[2] as Number).toLong(),
                avgResponseTimeMs = (row[3] as Number).toDouble()
            )
        }
    }
    
    @Transactional
    override fun cleanupOldLogs(retentionDays: Int): Int {
        val cutoff = LocalDateTime.now().minusDays(retentionDays.toLong())
        return repository.deleteByCalledAtBefore(cutoff)
    }
}
