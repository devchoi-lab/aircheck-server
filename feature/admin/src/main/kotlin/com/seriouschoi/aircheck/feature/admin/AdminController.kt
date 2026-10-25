package com.seriouschoi.aircheck.feature.admin

import com.seriouschoi.aircheck.core.domain.model.ServiceVersion
import com.seriouschoi.aircheck.core.domain.port.ApiUsagePort
import com.seriouschoi.aircheck.core.domain.port.ApiUsageStats
import com.seriouschoi.aircheck.core.domain.port.HourlyStats
import com.seriouschoi.aircheck.core.domain.port.ServiceConfigUseCase
import com.seriouschoi.aircheck.core.domain.port.ServiceVersionUseCase
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.CacheManager
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDate

@RestController
@RequestMapping("/admin")
class AdminController(
    private val apiUsagePort: ApiUsagePort,
    private val serviceVersionUseCase: ServiceVersionUseCase,
    private val serviceConfigUseCase: ServiceConfigUseCase,
    private val cacheManager: CacheManager,
    @Value("\${admin.api-key}") private val adminApiKey: String
) {
    
    private fun validateApiKey(key: String?) {
        if (key != adminApiKey) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid API Key")
        }
    }
    
    /**
     * 오늘 API 사용량 조회
     */
    @GetMapping("/api-usage/today")
    fun getTodayUsage(
        @RequestHeader("X-Admin-Key") apiKey: String?
    ): ApiUsageResponse {
        validateApiKey(apiKey)
        val stats = apiUsagePort.getTodayStats()
        return ApiUsageResponse(
            date = LocalDate.now(),
            stats = stats,
            limits = API_LIMITS
        )
    }
    
    /**
     * 기간별 API 사용량 조회
     */
    @GetMapping("/api-usage")
    fun getUsage(
        @RequestHeader("X-Admin-Key") apiKey: String?,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) 
        startDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) 
        endDate: LocalDate
    ): List<ApiUsageStats> {
        validateApiKey(apiKey)
        return apiUsagePort.getStats(startDate, endDate)
    }
    
    /**
     * API 잔여 호출 수 조회
     */
    @GetMapping("/api-usage/remaining")
    fun getRemainingCalls(
        @RequestHeader("X-Admin-Key") apiKey: String?
    ): Map<String, RemainingCalls> {
        validateApiKey(apiKey)
        val stats = apiUsagePort.getTodayStats()
        
        return API_LIMITS.mapValues { (apiType, limit) ->
            val used = stats.find { it.apiType == apiType }?.callCount ?: 0
            RemainingCalls(
                limit = limit,
                used = used,
                remaining = limit - used,
                usagePercent = if (limit > 0) (used.toDouble() / limit * 100) else 0.0
            )
        }
    }
    
    /**
     * 시간대별 통계 조회
     */
    @GetMapping("/api-usage/hourly")
    fun getHourlyStats(
        @RequestHeader("X-Admin-Key") apiKey: String?,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        date: LocalDate?
    ): List<HourlyStats> {
        validateApiKey(apiKey)
        return apiUsagePort.getHourlyStats(date ?: LocalDate.now())
    }
    
    /**
     * 수동 로그 정리
     */
    @DeleteMapping("/api-usage/cleanup")
    fun cleanupLogs(
        @RequestHeader("X-Admin-Key") apiKey: String?,
        @RequestParam(defaultValue = "30") retentionDays: Int
    ): Map<String, Any> {
        validateApiKey(apiKey)
        val deleted = apiUsagePort.cleanupOldLogs(retentionDays)
        return mapOf(
            "deletedCount" to deleted,
            "retentionDays" to retentionDays
        )
    }
    
    // ─────────────────────────────────────────────────────────────────────────
    // Service Version Management (강제 업데이트)
    // ─────────────────────────────────────────────────────────────────────────
    
    /**
     * 서비스 버전 조회
     */
    @GetMapping("/service-version/{osType}")
    fun getServiceVersion(
        @RequestHeader("X-Admin-Key") apiKey: String?,
        @PathVariable osType: String
    ): ServiceVersion? {
        validateApiKey(apiKey)
        return serviceVersionUseCase.getServiceVersion(osType)
    }
    
    /**
     * 서비스 버전 설정/업데이트
     */
    @PostMapping("/service-version")
    fun updateServiceVersion(
        @RequestHeader("X-Admin-Key") apiKey: String?,
        @RequestBody request: ServiceVersionRequest
    ): ServiceVersion {
        validateApiKey(apiKey)
        return serviceVersionUseCase.updateServiceVersion(
            osType = request.osType,
            minVersionCode = request.minVersionCode,
            updateUrl = request.updateUrl
        )
    }
    
    // ─────────────────────────────────────────────────────────────────────────
    // App Check Management
    // ─────────────────────────────────────────────────────────────────────────
    
    /**
     * App Check 상태 조회
     */
    @GetMapping("/appcheck/status")
    fun getAppCheckStatus(
        @RequestHeader("X-Admin-Key") apiKey: String?
    ): AppCheckStatusResponse {
        validateApiKey(apiKey)
        return AppCheckStatusResponse(
            enabled = serviceConfigUseCase.isAppCheckEnabled()
        )
    }
    
    /**
     * App Check 활성화/비활성화
     */
    @PostMapping("/appcheck/toggle")
    fun toggleAppCheck(
        @RequestHeader("X-Admin-Key") apiKey: String?,
        @RequestBody request: AppCheckToggleRequest
    ): AppCheckStatusResponse {
        validateApiKey(apiKey)
        serviceConfigUseCase.setAppCheckEnabled(request.enabled)
        return AppCheckStatusResponse(enabled = request.enabled)
    }
    
    // ─────────────────────────────────────────────────────────────────────────
    // KMA API Settings
    // ─────────────────────────────────────────────────────────────────────────
    
    /**
     * KMA 병렬 호출 상태 조회
     */
    @GetMapping("/kma/parallel")
    fun getKmaParallelStatus(
        @RequestHeader("X-Admin-Key") apiKey: String?
    ): KmaParallelResponse {
        validateApiKey(apiKey)
        return KmaParallelResponse(
            parallel = serviceConfigUseCase.isKmaParallelEnabled()
        )
    }
    
    /**
     * KMA 병렬/순차 호출 전환
     */
    @PostMapping("/kma/parallel")
    fun toggleKmaParallel(
        @RequestHeader("X-Admin-Key") apiKey: String?,
        @RequestBody request: KmaParallelRequest
    ): KmaParallelResponse {
        validateApiKey(apiKey)
        serviceConfigUseCase.setKmaParallelEnabled(request.parallel)
        return KmaParallelResponse(parallel = request.parallel)
    }
    
    // ─────────────────────────────────────────────────────────────────────────
    // Cache Management
    // ─────────────────────────────────────────────────────────────────────────
    
    /**
     * 캐시 목록 조회
     */
    @GetMapping("/cache")
    fun getCacheNames(
        @RequestHeader("X-Admin-Key") apiKey: String?
    ): CacheStatusResponse {
        validateApiKey(apiKey)
        return CacheStatusResponse(
            caches = cacheManager.cacheNames.toList()
        )
    }
    
    /**
     * 전체 캐시 클리어
     */
    @DeleteMapping("/cache")
    fun clearAllCaches(
        @RequestHeader("X-Admin-Key") apiKey: String?
    ): CacheClearResponse {
        validateApiKey(apiKey)
        val clearedCaches = mutableListOf<String>()
        cacheManager.cacheNames.forEach { cacheName ->
            cacheManager.getCache(cacheName)?.clear()
            clearedCaches.add(cacheName)
        }
        return CacheClearResponse(
            cleared = clearedCaches,
            message = "${clearedCaches.size}개 캐시 클리어 완료"
        )
    }
    
    /**
     * 특정 캐시 클리어
     */
    @DeleteMapping("/cache/{cacheName}")
    fun clearCache(
        @RequestHeader("X-Admin-Key") apiKey: String?,
        @PathVariable cacheName: String
    ): CacheClearResponse {
        validateApiKey(apiKey)
        val cache = cacheManager.getCache(cacheName)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "캐시 '$cacheName' 없음")
        cache.clear()
        return CacheClearResponse(
            cleared = listOf(cacheName),
            message = "'$cacheName' 캐시 클리어 완료"
        )
    }
    
    companion object {
        val API_LIMITS = mapOf(
            "OPEN_METEO" to 10_000L,
            "AIR_KOREA" to 10_000L,
            "KMA_ULTRA_SRT_NCST" to 100_000L,
            "KMA_ULTRA_SRT_FCST" to 100_000L,
            "KMA_VILAGE_FCST" to 100_000L,
            "KMA_MID_FCST" to 100_000L,
        )
    }
}

data class AppCheckStatusResponse(
    val enabled: Boolean
)

data class AppCheckToggleRequest(
    val enabled: Boolean
)

data class ServiceVersionRequest(
    val osType: String,
    val minVersionCode: Int,
    val updateUrl: String? = null
)

data class ApiUsageResponse(
    val date: LocalDate,
    val stats: List<ApiUsageStats>,
    val limits: Map<String, Long>
)

data class RemainingCalls(
    val limit: Long,
    val used: Long,
    val remaining: Long,
    val usagePercent: Double
)

data class CacheStatusResponse(
    val caches: List<String>
)

data class CacheClearResponse(
    val cleared: List<String>,
    val message: String
)

data class KmaParallelRequest(
    val parallel: Boolean
)

data class KmaParallelResponse(
    val parallel: Boolean
)
