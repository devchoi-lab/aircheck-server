package com.seriouschoi.aircheck.feature.weather

import com.fasterxml.jackson.databind.ObjectMapper
import com.seriouschoi.aircheck.core.domain.model.AirQualityResponse
import com.seriouschoi.aircheck.core.domain.model.DailyForecast
import com.seriouschoi.aircheck.core.domain.model.WeatherCondition
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.test.autoconfigure.json.JsonTest
import java.time.Instant
import java.time.LocalDate

/**
 * 컨트롤러 응답은 Jackson(Spring Boot 기본 설정)으로 직렬화된다.
 * 시각 타입 변경 후에도 JSON 출력 형식이 기존과 같은지 고정한다.
 *
 * 주의: Spring Boot 기본 Jackson 설정 기준이다. 앱에서 Jackson 설정을 바꾸면 이 테스트가 반영하지 못한다.
 */
@JsonTest
class WeatherResponseJsonTest(@Autowired private val objectMapper: ObjectMapper) {

    @SpringBootConfiguration
    class TestConfig

    @Test
    fun `AirQualityResponse dataTime은 UTC ISO 8601 문자열이다`() {
        val response = AirQualityResponse(
            stationName = "중구",
            sidoName = "서울",
            dataTime = Instant.parse("2026-03-20T00:00:00Z"),
            pm10 = 45,
            pm10Station = "중구",
            pm25 = 23,
            pm25Station = "중구"
        )

        val node = objectMapper.readTree(objectMapper.writeValueAsString(response))

        assertEquals("2026-03-20T00:00:00Z", node["dataTime"].asText())
    }

    @Test
    fun `DailyForecast date는 YYYY-MM-DD 문자열이다`() {
        val forecast = DailyForecast(
            date = LocalDate.of(2026, 3, 20),
            dayOfWeek = "금",
            minTemp = 1.0,
            maxTemp = 12.0,
            amPrecipProb = 10,
            pmPrecipProb = 20,
            amWeatherCondition = WeatherCondition.CLEAR,
            pmWeatherCondition = WeatherCondition.CLOUDY
        )

        val node = objectMapper.readTree(objectMapper.writeValueAsString(forecast))

        assertEquals("2026-03-20", node["date"].asText())
    }
}
