package com.seriouschoi.aircheck.core.domain.serializer

import com.seriouschoi.aircheck.core.domain.model.DailyForecast
import com.seriouschoi.aircheck.core.domain.model.WeatherCondition
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class LocalDateSerializerTest {

    private val forecast = DailyForecast(
        date = LocalDate.of(2026, 3, 20),
        dayOfWeek = "금",
        minTemp = 1.0,
        maxTemp = 12.0,
        amPrecipProb = 10,
        pmPrecipProb = 20,
        amWeatherCondition = WeatherCondition.CLEAR,
        pmWeatherCondition = WeatherCondition.CLOUDY
    )

    @Test
    fun `date는 YYYY-MM-DD 문자열로 직렬화된다`() {
        val json = Json.encodeToString(DailyForecast.serializer(), forecast)
        assertEquals(true, json.contains("\"date\":\"2026-03-20\""), json)
    }

    @Test
    fun `직렬화 후 역직렬화하면 원래 값과 같다`() {
        val json = Json.encodeToString(DailyForecast.serializer(), forecast)
        assertEquals(forecast, Json.decodeFromString(DailyForecast.serializer(), json))
    }
}
