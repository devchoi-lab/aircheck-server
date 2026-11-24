package com.seriouschoi.aircheck.core.airkorea.cache

import com.seriouschoi.aircheck.core.airkorea.api.StationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

/**
 * 측정소 정보 캐시 서비스
 *
 * 측정소 목록은 거의 변하지 않으므로 장기 캐싱
 */
@Service
class StationCacheService(
    @Value("\${airkorea.api-key}") private val apiKey: String,
    private val stationApi: StationApi
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * 전국 측정소 정보 로드 (캐시 적용)
     *
     * @return 측정소명 → StationInfo 맵
     */
    @Cacheable("stations", unless = "#result.isEmpty()")
    fun loadStations(): Map<String, StationInfo> {
        log.info("측정소 정보 로드 시작 (API 호출)")

        return try {
            val response = stationApi.getStationList(
                serviceKey = apiKey,
                pageNo = 1,
                numOfRows = 700,
                returnType = "json",
                ver = "1.1"
            )
            val parsed = json.decodeFromString<StationDto>(response)

            val stations = parsed.response?.body?.items?.mapNotNull { station ->
                val lng = station.dmX.toDoubleOrNull()
                val lat = station.dmY.toDoubleOrNull()
                if (lat != null && lng != null) {
                    val sidoName = station.addr.split(" ").firstOrNull() ?: ""
                    station.stationName to StationInfo(lat, lng, sidoName)
                } else null
            }?.toMap() ?: emptyMap()
            
            log.info("측정소 ${stations.size}개 로드 완료")
            stations
        } catch (e: Exception) {
            log.error("측정소 로드 실패: ${e.message}")
            emptyMap()
        }
    }
}

// ── 모델 ───────────────────────────────────────────────────────────────────

data class StationInfo(
    val lat: Double,
    val lng: Double,
    val sidoName: String
)

@Serializable
data class StationDto(val response: StationBodyDto? = null)

@Serializable
data class StationBodyDto(val body: StationItemsDto? = null)

@Serializable
data class StationItemsDto(val items: List<StationItemDto> = emptyList())

/**
 * 측정소 목록 조회(getMsrstnList) 응답의 측정소 한 건.
 * 출처: 에어코리아 측정소정보 기술문서 v1.2
 *
 * ver=1.1로 호출한다 (StationCacheService.loadStations).
 * 1.1 이전 버전은 dmX와 dmY가 뒤바뀌어 제공되므로, ver을 바꾸면 좌표 해석이 달라진다.
 */
@Serializable
data class StationItemDto(
    /** 측정소명. 대기질 조회의 키로 쓴다. 예: "종로구" */
    val stationName: String = "",
    /** 측정소 주소. 맨 앞 단어를 시도명으로 쓴다. 예: "서울 종로구 종로35가길 19종로5,6가 동 주민센터" */
    val addr: String = "",
    /** 경도 (WGS84)(longitude). ver=1.1 기준. 예: "127.005028" */
    val dmX: String = "",
    /** 위도 (WGS84)(latitude). ver=1.1 기준. 예: "37.572025" */
    val dmY: String = ""
)
