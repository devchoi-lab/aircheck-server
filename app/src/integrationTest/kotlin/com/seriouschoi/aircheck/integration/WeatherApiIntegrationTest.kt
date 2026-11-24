package com.seriouschoi.aircheck.integration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.cache.CacheManager
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.MariaDBContainer

/**
 * GET /api/v1/weather 통합 테스트.
 *
 * 우리 서버(필터~어댑터~캐시~DB)는 진짜로 띄우고, 바깥(기상청/에어코리아)만 가짜 서버로 대체한다.
 * DB는 운영과 같은 MariaDB 11 컨테이너다. Docker가 필요하다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WeatherApiIntegrationTest(
    @Autowired private val rest: TestRestTemplate,
    @Autowired private val objectMapper: ObjectMapper,
    @Autowired private val cacheManager: CacheManager
) {

    companion object {
        private val mariaDb = MariaDBContainer("mariadb:11").also { it.start() }

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            // 서버 기동 시 측정소 API를 부르므로, 컨텍스트가 뜨기 전에 측정소 응답을 준비한다.
            FakeExternalApis.stubStations()

            registry.add("spring.datasource.url") { mariaDb.jdbcUrl }
            registry.add("spring.datasource.username") { mariaDb.username }
            registry.add("spring.datasource.password") { mariaDb.password }

            registry.add("weather.provider") { "kma" }
            registry.add("appcheck.enabled") { "false" }
            registry.add("airkorea.api-key") { "test-key" }
            registry.add("kma.api.key") { "test-key" }
            registry.add("kma.api.base-url") { FakeExternalApis.kmaShortUrl }
            registry.add("kma.api.mid-url") { FakeExternalApis.kmaMidUrl }
            registry.add("airkorea.base-url") { FakeExternalApis.airKoreaUrl }
            registry.add("airkorea.station-url") { FakeExternalApis.stationUrl }
        }
    }

    @BeforeEach
    fun setUp() {
        // 테스트끼리 캐시와 가짜 서버 요청 기록이 남지 않게 한다.
        cacheManager.cacheNames.forEach { cacheManager.getCache(it)?.clear() }
        FakeExternalApis.reset()
        FakeExternalApis.stubAllSuccess()
    }

    @Test
    fun `외부 API가 모두 성공하면 날씨와 대기질을 함께 돌려준다`() {
        val response = rest.getForEntity("/api/v1/weather?lat=37.5665&lng=126.9780", String::class.java)

        assertEquals(200, response.statusCode.value())
        val body: JsonNode = objectMapper.readTree(response.body)
        assertEquals(
            FakeResponses.CURRENT_TEMPERATURE,
            body.path("kmaWeather")
                .path("shortForecast")
                .path("current")
                .path("temperature")
                .asDouble()
        )
        assertEquals(
            FakeResponses.PM10,
            body.path("airQuality")
                .path("pm10")
                .asInt()
        )
        assertEquals(
            0,
            body.path("errors")
                .size()
        )
    }

    @Test
    fun `대기질만 실패하면 날씨는 그대로 주고 대기질은 null, errors에 이유를 담는다`() {
        FakeExternalApis.stubAirQualityFailure()

        val response = rest.getForEntity("/api/v1/weather?lat=37.5665&lng=126.9780", String::class.java)

        assertEquals(200, response.statusCode.value())
        val body: JsonNode = objectMapper.readTree(response.body)
        assertEquals(
            FakeResponses.CURRENT_TEMPERATURE,
            body.path("kmaWeather")
                .path("shortForecast")
                .path("current")
                .path("temperature")
                .asDouble()
        )
        assertTrue(body.path("airQuality").isNull)
        assertEquals(1, body.path("errors").size())
        assertEquals(
            "AIR_QUALITY",
            body.path("errors").path(0).path("target").asText()
        )
        assertEquals(
            "API_UNAVAILABLE",
            body.path("errors").path(0).path("errorCode").asText()
        )
    }

    @Test
    fun `서비스 지역 밖 좌표는 404이고 외부 API를 부르지 않는다`() {
        // 도쿄: 가장 가까운 법정동이 50km보다 멀다.
        val response = rest.getForEntity("/api/v1/weather?lat=35.6762&lng=139.6503", String::class.java)

        assertEquals(404, response.statusCode.value())
        assertTrue(FakeExternalApis.server.allServeEvents.isEmpty())
    }

    @Test
    fun `같은 좌표로 두 번 부르면 외부 API는 한 번만 호출된다`() {
        val url = "/api/v1/weather?lat=37.5665&lng=126.9780"

        val first = rest.getForEntity(url, String::class.java)
        val second = rest.getForEntity(url, String::class.java)

        assertEquals(200, first.statusCode.value())
        assertEquals(first.body, second.body)
        listOf(
            "/kma/short/getUltraSrtNcst",
            "/kma/short/getVilageFcst",
            "/kma/mid/getMidLandFcst",
            "/kma/mid/getMidTa",
            "/airkorea/air/getMsrstnAcctoRltmMesureDnsty"
        ).forEach { path ->
            assertEquals(1, FakeExternalApis.requestCount(path), path)
        }
    }

    @Test
    fun `대기질 조회가 실패한 결과는 캐시하지 않아, 복구되면 다음 요청에서 받아온다`() {
        val url = "/api/v1/weather?lat=37.5665&lng=126.9780"
        FakeExternalApis.stubAirQualityFailure()
        val failed: JsonNode = objectMapper.readTree(rest.getForEntity(url, String::class.java).body)
        assertTrue(failed.path("airQuality").isNull)

        FakeExternalApis.stubAirQualitySuccess()
        val recovered: JsonNode = objectMapper.readTree(rest.getForEntity(url, String::class.java).body)

        assertEquals(
            FakeResponses.PM10,
            recovered.path("airQuality")
                .path("pm10")
                .asInt()
        )
        assertEquals(0, recovered.path("errors").size())
    }
}
