package com.seriouschoi.aircheck.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.seriouschoi.aircheck.core.domain.model.AirQualityResponse
import com.seriouschoi.aircheck.core.domain.model.DailyForecast
import com.seriouschoi.aircheck.core.domain.model.PrecipitationType
import com.seriouschoi.aircheck.core.domain.model.SkyCondition
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.test.autoconfigure.json.JsonTest
import org.springframework.context.annotation.Import
import java.time.Instant
import java.time.LocalDate

/**
 * 응답 JSON의 시각 형식 테스트.
 * 형식은 [InstantJsonSerializer], [LocalDateJsonSerializer]가 정한다.
 */
@JsonTest
@Import(InstantJsonSerializer::class, LocalDateJsonSerializer::class)
class ResponseJsonTest(@Autowired private val objectMapper: ObjectMapper) {

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
            minTemp = 1.0,
            maxTemp = 12.0,
            amPrecipProb = 10,
            pmPrecipProb = 20,
            amSky = SkyCondition.CLEAR,
            pmSky = SkyCondition.OVERCAST,
            amPrecipitationType = PrecipitationType.NONE,
            pmPrecipitationType = PrecipitationType.SHOWER
        )

        val node = objectMapper.readTree(objectMapper.writeValueAsString(forecast))

        assertEquals("2026-03-20", node["date"].asText())
    }

    @Test
    fun `Jackson 타임스탬프 설정이 켜져 있어도 문자열 형식이 유지된다`() {
        // Boot 기본 설정에 기대지 않고 직렬화기가 형식을 정한다는 것을 확인
        val mapper = ObjectMapper()
            .registerModule(JavaTimeModule())
            .registerModule(
                SimpleModule()
                    .addSerializer(Instant::class.java, InstantJsonSerializer())
                    .addSerializer(LocalDate::class.java, LocalDateJsonSerializer())
            )
            .enable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

        assertEquals("\"2026-03-20T00:00:00Z\"", mapper.writeValueAsString(Instant.parse("2026-03-20T00:00:00Z")))
        assertEquals("\"2026-03-20\"", mapper.writeValueAsString(LocalDate.of(2026, 3, 20)))
    }
}
