package com.seriouschoi.aircheck.core.airkorea

import com.seriouschoi.aircheck.core.domain.model.AirQualityResponse
import com.seriouschoi.aircheck.core.domain.port.AirQualityPort
import com.seriouschoi.aircheck.core.service.port.ApiUsagePort
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

// ── Adapter 구현 ───────────────────────────────────────────────────────────

@Component
class AirKoreaAdapter(
    @Value("\${airkorea.api-key}") private val apiKey: String,
    private val airKoreaApi: AirKoreaApi,
    private val apiUsagePort: ApiUsagePort?,
    private val stationCacheService: StationCacheService
) : AirQualityPort {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val API_TYPE = "AIR_KOREA"
        private val KST = ZoneId.of("Asia/Seoul")
        private val KST_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    }

    private val json = Json {
        ignoreUnknownKeys = true 
        coerceInputValues = true
    }

    /**
     * 에어코리아 측정일시(KST, "yyyy-MM-dd HH:mm")를 UTC ISO 8601 문자열로 변환.
     * 파싱 실패 시 원문 반환.
     */
    private fun String.kstToUtcIso(): String = try {
        LocalDateTime.parse(this, KST_FORMATTER)
            .atZone(KST)
            .withZoneSameInstant(ZoneOffset.UTC)
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    } catch (e: Exception) {
        log.warn("dataTime 파싱 실패, 원문 반환: {}", this)
        this
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
        val mainData: AirKoreaItem? = fetchRawData(mainStation)
        
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
            dataTime = dataTime.kstToUtcIso(),
            // 미세먼지 (값 없으면 주변 측정소에서 채움)
            pm10 = pm10,
            pm10Station = pm10Station,
            pm10GradeAirKorea = mainData?.pm10Grade1h?.toIntOrNull(),
            pm10Flag = mainData?.pm10Flag.toMeasurementFlag(),
            pm25 = pm25,
            pm25Station = pm25Station,
            pm25GradeAirKorea = mainData?.pm25Grade1h?.toIntOrNull(),
            pm25Flag = mainData?.pm25Flag.toMeasurementFlag(),
            // 통합지수 (메인 측정소)
            khaiValue = mainData?.khaiValue?.toIntOrNull(),
            khaiGrade = mainData?.khaiGrade?.toIntOrNull(),
            // 가스류 (메인 측정소)
            so2 = mainData?.so2Value?.toDoubleOrNull(),
            so2GradeAirKorea = mainData?.so2Grade?.toIntOrNull(),
            so2Flag = mainData?.so2Flag.toMeasurementFlag(),
            co = mainData?.coValue?.toDoubleOrNull(),
            coGradeAirKorea = mainData?.coGrade?.toIntOrNull(),
            coFlag = mainData?.coFlag.toMeasurementFlag(),
            o3 = mainData?.o3Value?.toDoubleOrNull(),
            o3GradeAirKorea = mainData?.o3Grade?.toIntOrNull(),
            o3Flag = mainData?.o3Flag.toMeasurementFlag(),
            no2 = mainData?.no2Value?.toDoubleOrNull(),
            no2GradeAirKorea = mainData?.no2Grade?.toIntOrNull(),
            no2Flag = mainData?.no2Flag.toMeasurementFlag()
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
                ver = "1.5"
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
