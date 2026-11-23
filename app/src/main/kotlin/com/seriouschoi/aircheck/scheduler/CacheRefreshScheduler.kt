package com.seriouschoi.aircheck.scheduler

import com.seriouschoi.aircheck.core.airkorea.cache.StationCacheService
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.cache.CacheManager
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class CacheRefreshScheduler(
    private val stationCacheService: StationCacheService,
    private val cacheManager: CacheManager
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @PostConstruct
    fun init() {
        log.info("측정소 정보 로딩 시작...")
        val stations = stationCacheService.loadStations()
        log.info("측정소 정보 로딩 완료: ${stations.size}개")
    }

    @Scheduled(fixedRate = 600_000) // 10분
    fun logCacheStats() {
        val airCache = cacheManager.getCache("airquality")
        val weatherCache = cacheManager.getCache("weather")
        val stationsCache = cacheManager.getCache("stations")
        log.debug("캐시 상태 - airquality: ${airCache != null}, weather: ${weatherCache != null}, stations: ${stationsCache != null}")
    }

    @Scheduled(cron = "0 0 6 * * *") // 매일 새벽 6시
    fun refreshStationList() {
        log.info("측정소 목록 캐시 갱신 중...")
        cacheManager.getCache("stations")?.clear()
        val stations = stationCacheService.loadStations()
        log.info("측정소 목록 갱신 완료: ${stations.size}개")
    }
}
