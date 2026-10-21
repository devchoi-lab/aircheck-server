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
 * 에어코리아 실시간 대기질 측정 데이터 (ver=1.5 기준)
 *
 * 측정값이 없거나 기기 이상 시 "-"로 내려옴.
 * toIntOrNull() / toDoubleOrNull() 으로 파싱하면 null 처리 가능.
 *
 * 등급 기준 (1:좋음 / 2:보통 / 3:나쁨 / 4:매우나쁨)
 *
 * pm10Grade/pm25Grade = 24시간 등급, pm10Grade1h/pm25Grade1h = 1시간 등급.
 * 현재 화면에 1시간 농도(pm10Value)와 24시간 등급(pm10Grade)을 함께 쓰고 있어 시간 기준이 다름 — 추후 검토 필요.
 *
 * Flag 필드: 값이 있으면 비정상 상태 (점검및교정/장비점검/자료이상/통신장애). null이면 정상.
 * 현재 Flag를 확인하지 않아 비정상 데이터가 정상처럼 처리될 수 있음 — 추후 개선 필요.
 */
@Serializable
data class AirKoreaItem(
    /** 측정소명 (예: "중구", "강남구"). ver≥1.4부터 응답에 포함 */
    val stationName: String = "",
    /** 측정소 코드. ver≥1.4부터 응답에 포함. 현재 AirQualityResponse에 미전달 */
    val stationCode: String = "",
    /** 시도명 (예: "서울", "경기"). API 응답이 아닌 StationInfo에서 채워짐 */
    val sidoName: String = "",
    /** 측정일시 (yyyy-MM-dd HH:mm) */
    val dataTime: String = "",
    /** 측정망 정보 (도시대기/도로변대기/국가배경농도/교외대기/항만). ver≥1.2. 현재 미사용 */
    val mangName: String = "",

    // ── 미세먼지 ──────────────────────────────────────────────────────────────
    /** PM10 농도 (㎍/㎥, 1시간값) */
    val pm10Value: String = "-",
    /** PM10 24시간 등급. 현재 AirQualityResponse에 미전달 */
    val pm10Grade: String = "-",
    /** PM10 24시간 예측이동농도 (㎍/㎥). ver≥1.1. 미세먼지 경보 발령 기준값 (#46 연관). 현재 미사용 */
    val pm10Value24: String = "-",
    /** PM10 1시간 등급. ver≥1.3. 현재 미사용 */
    val pm10Grade1h: String = "-",
    /** PM10 측정기기 상태 플래그. null이면 정상. 현재 미사용 */
    val pm10Flag: String? = null,

    /** PM2.5 농도 (㎍/㎥, 1시간값). ver≥1.0 */
    val pm25Value: String = "-",
    /** PM2.5 24시간 등급. 현재 AirQualityResponse에 미전달 */
    val pm25Grade: String = "-",
    /** PM2.5 24시간 예측이동농도 (㎍/㎥). ver≥1.1. 미세먼지 경보 발령 기준값 (#46 연관). 현재 미사용 */
    val pm25Value24: String = "-",
    /** PM2.5 1시간 등급. ver≥1.3. 현재 미사용 */
    val pm25Grade1h: String = "-",
    /** PM2.5 측정기기 상태 플래그. null이면 정상. 현재 미사용 */
    val pm25Flag: String? = null,

    // ── 통합지수 ──────────────────────────────────────────────────────────────
    /** 통합대기환경지수 (0~500) */
    val khaiValue: String = "-",
    /** 통합대기환경지수 등급 */
    val khaiGrade: String = "-",

    // ── 가스류 ────────────────────────────────────────────────────────────────
    /** 아황산가스 농도 (ppm, ver≥1.5부터 소수점 4자리) */
    val so2Value: String = "-",
    /** 아황산가스 등급. 현재 AirQualityResponse에 미전달 */
    val so2Grade: String = "-",
    /** 아황산가스 측정기기 상태 플래그. null이면 정상. 현재 미사용 */
    val so2Flag: String? = null,

    /** 일산화탄소 농도 (ppm, ver≥1.5부터 소수점 2자리) */
    val coValue: String = "-",
    /** 일산화탄소 등급. 현재 AirQualityResponse에 미전달 */
    val coGrade: String = "-",
    /** 일산화탄소 측정기기 상태 플래그. null이면 정상. 현재 미사용 */
    val coFlag: String? = null,

    /** 오존 농도 (ppm, ver≥1.5부터 소수점 4자리) */
    val o3Value: String = "-",
    /** 오존 등급. 현재 AirQualityResponse에 미전달 */
    val o3Grade: String = "-",
    /** 오존 측정기기 상태 플래그. null이면 정상. 현재 미사용 */
    val o3Flag: String? = null,

    /** 이산화질소 농도 (ppm, ver≥1.5부터 소수점 4자리) */
    val no2Value: String = "-",
    /** 이산화질소 등급. 현재 AirQualityResponse에 미전달 */
    val no2Grade: String = "-",
    /** 이산화질소 측정기기 상태 플래그. null이면 정상. 현재 미사용 */
    val no2Flag: String? = null
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
