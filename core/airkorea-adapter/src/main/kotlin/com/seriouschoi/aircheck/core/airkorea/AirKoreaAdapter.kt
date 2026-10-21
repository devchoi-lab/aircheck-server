package com.seriouschoi.aircheck.core.airkorea

import com.seriouschoi.aircheck.core.domain.model.AirQualityResponse
import com.seriouschoi.aircheck.core.domain.port.AirQualityPort
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

// ── 에어코리아 API 응답 모델 ───────────────────────────────────────────────

@Serializable
data class AirKoreaResponse(val response: AirKoreaBody? = null)

@Serializable
data class AirKoreaBody(val body: AirKoreaItems? = null)

@Serializable
data class AirKoreaItems(val items: List<AirKoreaItem> = emptyList())

/**
 * 에어코리아 실시간 대기질 측정 데이터
 *
 * 측정값이 없거나 기기 이상 시 "-"로 내려옴.
 * toIntOrNull() / toDoubleOrNull() 으로 파싱하면 null 처리 가능.
 *
 * 등급 기준 (1:좋음 / 2:보통 / 3:나쁨 / 4:매우나쁨)
 *
 * 미매핑 필드 (API에는 존재, 현재 미사용):
 *   - pm10Grade1h, pm25Grade1h : 1시간 등급 (ver 1.3 이상)
 *   - pm10Flag, pm25Flag, so2Flag, coFlag, o3Flag, no2Flag : 측정기기 상태 (null이면 정상, 값 있으면 이상)
 *   - mangName : 측정망 이름
 */
@Serializable
data class AirKoreaItem(
    /** 측정소명 (예: "중구", "강남구") */
    val stationName: String = "",
    /** 시도명 (예: "서울", "경기") */
    val sidoName: String = "",
    /** 측정일시 (yyyy-MM-dd HH:mm) */
    val dataTime: String = "",

    // ── 미세먼지 ──────────────────────────────────────────────────────────────
    /** PM10 농도 (㎍/㎥) */
    val pm10Value: String = "-",
    /** PM10 등급. AirQualityResponse에 미전달 (현재 미사용) */
    val pm10Grade: String = "-",
    /** PM2.5 농도 (㎍/㎥) */
    val pm25Value: String = "-",
    /** PM2.5 등급. AirQualityResponse에 미전달 (현재 미사용) */
    val pm25Grade: String = "-",

    // ── 통합지수 ──────────────────────────────────────────────────────────────
    /** 통합대기환경지수 (0~500) */
    val khaiValue: String = "-",
    /** 통합대기환경지수 등급 */
    val khaiGrade: String = "-",

    // ── 가스류 ────────────────────────────────────────────────────────────────
    /** 아황산가스 농도 (ppm) */
    val so2Value: String = "-",
    /** 아황산가스 등급. AirQualityResponse에 미전달 (현재 미사용) */
    val so2Grade: String = "-",
    /** 일산화탄소 농도 (ppm) */
    val coValue: String = "-",
    /** 일산화탄소 등급. AirQualityResponse에 미전달 (현재 미사용) */
    val coGrade: String = "-",
    /** 오존 농도 (ppm) */
    val o3Value: String = "-",
    /** 오존 등급. AirQualityResponse에 미전달 (현재 미사용) */
    val o3Grade: String = "-",
    /** 이산화질소 농도 (ppm) */
    val no2Value: String = "-",
    /** 이산화질소 등급. AirQualityResponse에 미전달 (현재 미사용) */
    val no2Grade: String = "-"
)

// ── Adapter 구현 ───────────────────────────────────────────────────────────

@Component
class AirKoreaAdapter(
    @Value("\${airkorea.api-key}") private val apiKey: String,
    private val airKoreaApi: AirKoreaApi,
    private val apiUsagePort: com.seriouschoi.aircheck.core.service.port.ApiUsagePort?,
    private val stationCacheService: StationCacheService
) : AirQualityPort {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val API_TYPE = "AIR_KOREA"
    }

    private val json = Json {
        ignoreUnknownKeys = true 
        coerceInputValues = true
    }

    /**
     * 가까운 측정소 N개를 거리순으로 반환
     */
    private fun findNearestStations(lat: Double, lng: Double, limit: Int = 5): List<Pair<String, StationInfo>> {
        val stations = stationCacheService.loadStations()
        
        return stations.entries
            .sortedBy { (_, info) ->
                Math.sqrt(Math.pow(lat - info.lat, 2.0) + Math.pow(lng - info.lng, 2.0))
            }
            .take(limit)
            .map { it.key to it.value }
    }

    override fun getAirQuality(lat: Double, lng: Double): AirQualityResponse? {
        val nearbyStations = findNearestStations(lat, lng, limit = 5)
        if (nearbyStations.isEmpty()) return null
        
        // 메인 측정소 (가장 가까운 곳)
        val (mainStation, mainInfo) = nearbyStations.first()
        val mainData = fetchRawData(mainStation)
        
        // 결과 변수 (기본값: 메인 측정소)
        var pm10: Int? = mainData?.pm10Value?.toIntOrNull()
        var pm10Station: String = mainStation
        var pm25: Int? = mainData?.pm25Value?.toIntOrNull()
        var pm25Station: String = mainStation
        var dataTime = mainData?.dataTime ?: ""
        
        // PM10이 없으면 주변 측정소에서 채우기
        if (pm10 == null) {
            for ((station, _) in nearbyStations.drop(1)) {
                val data = fetchRawData(station)
                val value = data?.pm10Value?.toIntOrNull()
                if (value != null) {
                    pm10 = value
                    pm10Station = station
                    if (dataTime.isEmpty()) dataTime = data.dataTime
                    log.debug("PM10: {} 측정소에서 가져옴 (값: {})", station, value)
                    break
                }
            }
        }
        
        // PM25가 없으면 주변 측정소에서 채우기
        if (pm25 == null) {
            for ((station, _) in nearbyStations.drop(1)) {
                val data = fetchRawData(station)
                val value = data?.pm25Value?.toIntOrNull()
                if (value != null) {
                    pm25 = value
                    pm25Station = station
                    if (dataTime.isEmpty()) dataTime = data.dataTime
                    log.debug("PM25: {} 측정소에서 가져옴 (값: {})", station, value)
                    break
                }
            }
        }
        
        // 둘 다 없으면 실패
        if (pm10 == null && pm25 == null) {
            log.warn("주변 {}개 측정소 모두 PM 데이터 없음 (lat={}, lng={})", nearbyStations.size, lat, lng)
            return null
        }
        
        return AirQualityResponse(
            stationName = mainStation,
            sidoName = mainInfo.sidoName,
            dataTime = dataTime,
            pm10 = pm10,
            pm10Station = pm10Station,
            pm25 = pm25,
            pm25Station = pm25Station,
            // 통합지수 (메인 측정소)
            khaiValue = mainData?.khaiValue?.toIntOrNull(),
            khaiGrade = mainData?.khaiGrade?.toIntOrNull(),
            // 가스류 (메인 측정소)
            so2 = mainData?.so2Value?.toDoubleOrNull(),
            co = mainData?.coValue?.toDoubleOrNull(),
            o3 = mainData?.o3Value?.toDoubleOrNull(),
            no2 = mainData?.no2Value?.toDoubleOrNull()
        )
    }
    
    /**
     * 측정소에서 원시 데이터 조회 (AirKoreaItem 반환)
     */
    private fun fetchRawData(stationName: String): AirKoreaItem? {
        val startTime = System.currentTimeMillis()

        return try {
            val response = airKoreaApi.getMeasurementData(
                stationName = stationName,
                serviceKey = apiKey,
                dataTerm = "DAILY",
                pageNo = 1,
                numOfRows = 1,
                returnType = "json",
                ver = "1.0"
            )

            val parsed = json.decodeFromString<AirKoreaResponse>(response)
            apiUsagePort?.recordSuccess(API_TYPE, System.currentTimeMillis() - startTime)
            parsed.response?.body?.items?.firstOrNull()
        } catch (e: Exception) {
            log.error("측정소 {} 조회 실패: {}", stationName, e.message)
            apiUsagePort?.recordFailure(API_TYPE, e.message)
            null
        }
    }
}
