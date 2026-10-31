package com.seriouschoi.aircheck.core.bjd

import kotlin.math.pow

/**
 * 위경도 → 법정동(法定洞) 매칭
 *
 * CSV 데이터 출처: 국토교통부_전국 법정동 공공데이터
 * CSV 재생성: ./gradlew :core:bjd-matcher:generateBjdCsv
 */
object BjdMatcher {

    /**
     * 위경도와 가장 가까운 법정동 반환.
     * latLng가 있는 말단 단위(읍면동) 항목 중 유클리드 최근접 매칭.
     */
    fun match(lat: Double, lng: Double): BjdData? =
        bjdList.filter { it.latLng != null }
            .minByOrNull { dist(it.latLng!!.first, it.latLng.second, lat, lng) }

    private fun dist(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double =
        (lat1 - lat2).pow(2) + (lng1 - lng2).pow(2)

    private const val CSV = "/bjd-lat-lng.csv"

    val bjdList: List<BjdData> by lazy { loadBjdList() }

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
