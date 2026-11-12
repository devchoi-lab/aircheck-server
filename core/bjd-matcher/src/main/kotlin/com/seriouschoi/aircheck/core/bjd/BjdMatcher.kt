package com.seriouschoi.aircheck.core.bjd

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 위경도 → 법정동(法定洞) 매칭
 *
 * CSV 데이터 출처: 국토교통부_전국 법정동 공공데이터
 * CSV 재생성: ./gradlew :core:bjd-matcher:generateBjdCsv
 */
object BjdMatcher {

    /**
     * 위경도와 가장 가까운 법정동 반환.
     * latLng가 있는 말단 단위(읍면동) 항목 중 최근접 매칭.
     *
     * 가장 가까운 법정동이 [maxDistanceKm]보다 멀면 null (한국 밖 좌표 등).
     */
    fun match(lat: Double, lng: Double, maxDistanceKm: Double): BjdData? {
        // 후보마다 삼각함수를 쓰지 않도록 cos은 한 번만 계산하고, km 변환은 최근접 한 곳에만 한다.
        val cosLat = cos(Math.toRadians(lat))
        val nearest = points.minByOrNull { scaledSquaredDistance(it.latLng!!, lat, lng, cosLat) }
            ?: return null
        return nearest.takeIf { distanceKm(nearest.latLng!!, lat, lng) <= maxDistanceKm }
    }

    /**
     * 최근접 비교용 거리의 제곱 (단위: 도). 경도 1도는 위도 1도보다 짧아서 cos(위도)로 보정한다.
     */
    private fun scaledSquaredDistance(point: Pair<Double, Double>, lat: Double, lng: Double, cosLat: Double): Double =
        (point.first - lat).pow(2) + ((point.second - lng) * cosLat).pow(2)

    /** 두 위경도 사이의 거리 (km, 하버사인) */
    private fun distanceKm(point: Pair<Double, Double>, lat: Double, lng: Double): Double {
        val dLat = Math.toRadians(lat - point.first)
        val dLng = Math.toRadians(lng - point.second)
        val h = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(point.first)) * cos(Math.toRadians(lat)) * sin(dLng / 2).pow(2)
        return 2 * EARTH_RADIUS_KM * asin(sqrt(h))
    }

    private const val EARTH_RADIUS_KM = 6371.0088

    private const val CSV = "/bjd-lat-lng.csv"

    private val bjdList: List<BjdData> by lazy { loadBjdList() }

    /** 매칭 대상(latLng가 있는 항목). 호출마다 걸러내지 않도록 한 번만 만든다. */
    private val points: List<BjdData> by lazy { bjdList.filter { it.latLng != null } }

    /** bjd-lat-lng.csv: code, doCode, doName, guCode, guName, dongCode, dongName, lat, lng */
    private fun loadBjdList(): List<BjdData> {
        val stream = BjdMatcher::class.java.getResourceAsStream(CSV)
            ?: error("리소스를 찾을 수 없음: $CSV")
        return stream.bufferedReader().useLines { lines ->
            lines.drop(1)
                .filter { it.isNotBlank() }
                .mapNotNull { line ->
                    val p = line.split(",")
                    if (p.size < 9) return@mapNotNull null
                    val lat = p[7].toDoubleOrNull()
                    val lng = p[8].toDoubleOrNull()
                    BjdData(
                        code = p[0],
                        doGroup = BjdGroup(name = p[2], code = p[1].toLongOrNull() ?: return@mapNotNull null),
                        guGroup = if (p[3].isNotBlank()) BjdGroup(name = p[4], code = p[3].toLongOrNull() ?: return@mapNotNull null) else null,
                        dongGroup = if (p[5].isNotBlank()) BjdGroup(name = p[6], code = p[5].toLongOrNull() ?: return@mapNotNull null) else null,
                        latLng = if (lat != null && lng != null) Pair(lat, lng) else null
                    )
                }
                .toList()
        }
    }
}
