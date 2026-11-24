package com.seriouschoi.aircheck.core.airkorea

import com.seriouschoi.aircheck.core.airkorea.api.AirKoreaApi
import com.seriouschoi.aircheck.core.airkorea.cache.AirQualityCache
import com.seriouschoi.aircheck.core.airkorea.cache.StationCacheService
import com.seriouschoi.aircheck.core.airkorea.cache.StationInfo
import com.seriouschoi.aircheck.core.domain.exception.AirQualityNotFoundException
import com.seriouschoi.aircheck.core.domain.model.AirQuality
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

/**
 * AirKoreaAdapter.getAirQuality 계약 테스트.
 *
 * 외부 의존(측정소 목록, 에어코리아 API)만 mock으로 대체하고, 결과(Result)만 검증한다.
 * API가 호출되지 않은 측정소는 stub하지 않는다. 어댑터가 엉뚱한 측정소를 부르면 데이터가 없어 테스트가 실패한다.
 * 사용량 기록(apiUsagePort)은 이 계약의 관심사가 아니므로 null로 둔다.
 */
class AirKoreaAdapterTest {

    private val airKoreaApi = mockk<AirKoreaApi>()

    @Test
    fun `위경도를 받아서, 가장 가까운 측정소의 공기질을 리턴한다`() {
        // 거리순으로 정렬해야 찾을 수 있도록 가까운 측정소를 목록 맨 뒤에 둔다.
        val adapter = createAdapter(
            "먼측정소" to StationInfo(36.0, 128.0, "경북"),
            "중간측정소" to StationInfo(37.6, 127.0, "경기"),
            "가까운측정소" to StationInfo(37.5, 127.01, "서울")
        )
        stubStation("가까운측정소", pm10 = "45", pm25 = "23")

        val result = adapter.getAirQuality(37.5, 127.0)

        val airQuality = result.getOrThrow()
        assertEquals("가까운측정소", airQuality.stationName)
        assertEquals("서울", airQuality.sidoName)
        assertEquals(45, airQuality.pm10)
        assertEquals(23, airQuality.pm25)
        // 측정시각은 KST로 내려오고 UTC로 변환해서 리턴한다. (KST 09:00 = UTC 00:00)
        assertEquals(Instant.parse("2026-03-20T00:00:00Z"), airQuality.dataTime)
    }

    @Test
    fun `측정소가 하나도 없으면, 공기질을 찾지 못해 AirQualityNotFoundException으로 실패한다`() {
        val adapter = createAdapter()

        val result = adapter.getAirQuality(37.5, 127.0)

        assertTrue(
            result.exceptionOrNull() is AirQualityNotFoundException,
            "측정소가 없으면 AirQualityNotFoundException으로 실패해야 하는데, ${result}를 리턴했습니다."
        )
    }

    @Test
    fun `가장 가까운 측정소에 PM10이 없으면, 그다음 가까운 측정소의 PM10으로 채우고 그 측정소를 알려준다`() {
        val adapter = createAdapter(
            "측정소A" to StationInfo(37.5, 127.0, "서울"),
            "측정소B" to StationInfo(37.51, 127.0, "서울"),
            "측정소C" to StationInfo(37.9, 127.0, "서울")
        )
        stubStation("측정소A", pm10 = "-", pm25 = "23")
        stubStation("측정소B", pm10 = "50", pm25 = "99")

        val airQuality = adapter.getAirQuality(37.5, 127.0).getOrThrow()

        assertEquals(50, airQuality.pm10)
        assertEquals("측정소B", airQuality.pm10Station)
        // PM2.5는 가장 가까운 측정소 값을 그대로 쓴다.
        assertEquals(23, airQuality.pm25)
        assertEquals("측정소A", airQuality.pm25Station)
        assertEquals("측정소A", airQuality.stationName)
    }

    @Test
    fun `가장 가까운 측정소에 PM2_5가 없으면, 그다음 가까운 측정소의 PM2_5로 채우고 그 측정소를 알려준다`() {
        val adapter = createAdapter(
            "측정소A" to StationInfo(37.5, 127.0, "서울"),
            "측정소B" to StationInfo(37.51, 127.0, "서울"),
            "측정소C" to StationInfo(37.9, 127.0, "서울")
        )
        stubStation("측정소A", pm10 = "45", pm25 = "-")
        stubStation("측정소B", pm10 = "99", pm25 = "30")

        val airQuality = adapter.getAirQuality(37.5, 127.0).getOrThrow()

        assertEquals(30, airQuality.pm25)
        assertEquals("측정소B", airQuality.pm25Station)
        // PM10은 가장 가까운 측정소 값을 그대로 쓴다.
        assertEquals(45, airQuality.pm10)
        assertEquals("측정소A", airQuality.pm10Station)
    }

    @Test
    fun `주변 측정소 모두 PM10과 PM2_5가 없으면, AirQualityNotFoundException으로 실패한다`() {
        val adapter = createAdapter(
            "측정소A" to StationInfo(37.5, 127.0, "서울"),
            "측정소B" to StationInfo(37.51, 127.0, "서울")
        )
        stubStation("측정소A", pm10 = "-", pm25 = "-")
        stubStation("측정소B", pm10 = "-", pm25 = "-")

        val result = adapter.getAirQuality(37.5, 127.0)

        assertTrue(
            result.exceptionOrNull() is AirQualityNotFoundException,
            "PM 데이터가 하나도 없으면 AirQualityNotFoundException으로 실패해야 하는데, ${result}를 리턴했습니다."
        )
    }

    @Test
    fun `PM10과 PM2_5 중 하나만 구할 수 있으면, 실패하지 않고 구한 값만 담아 리턴한다`() {
        val adapter = createAdapter(
            "측정소A" to StationInfo(37.5, 127.0, "서울"),
            "측정소B" to StationInfo(37.51, 127.0, "서울")
        )
        stubStation("측정소A", pm10 = "-", pm25 = "23")
        stubStation("측정소B", pm10 = "-", pm25 = "-")

        val airQuality: AirQuality = adapter.getAirQuality(37.5, 127.0).getOrThrow()

        assertNull(airQuality.pm10)
        assertEquals(23, airQuality.pm25)
    }

    // ── 테스트 도구 ────────────────────────────────────────────────────────────

    private fun createAdapter(vararg stations: Pair<String, StationInfo>): AirKoreaAdapter {
        val stationCacheService = mockk<StationCacheService>()
        every { stationCacheService.loadStations() } returns stations.toMap()
        return AirKoreaAdapter(
            AirQualityCache(
                apiKey = "TEST",
                airKoreaApi = airKoreaApi,
                apiUsagePort = null,
                stationCacheService = stationCacheService
            )
        )
    }

    /** 해당 측정소를 조회하면 PM 값만 담긴 에어코리아 응답 JSON을 돌려준다. 측정값이 없으면 "-"로 내려온다. */
    private fun stubStation(stationName: String, pm10: String, pm25: String) {
        every {
            airKoreaApi.getMeasurementData(
                stationName = stationName,
                serviceKey = any(),
                dataTerm = any(),
                pageNo = any(),
                numOfRows = any(),
                returnType = any(),
                ver = any()
            )
        } returns airKoreaResponse(pm10, pm25)
    }

    /**
     * PM 값만 담긴 에어코리아 응답을 만든다.
     * DTO를 직렬화하지 않고 JSON을 직접 쓴다. DTO 필드 이름이 실제 API 응답과 맞는지도 함께 확인하기 위해서다.
     */
    private fun airKoreaResponse(pm10: String, pm25: String): String = """
        {
          "response": {
            "body": {
              "items": [
                {
                  "dataTime": "2026-03-20 09:00",
                  "pm10Value": "$pm10",
                  "pm25Value": "$pm25"
                }
              ]
            }
          }
        }
    """.trimIndent()
}
