package com.seriouschoi.aircheck.core.service

import com.seriouschoi.aircheck.core.domain.model.AirQualityResponse
import com.seriouschoi.aircheck.core.domain.model.CurrentWeather
import com.seriouschoi.aircheck.core.domain.model.WeatherCondition
import com.seriouschoi.aircheck.core.domain.model.WeatherResponse
import com.seriouschoi.aircheck.core.domain.port.AirQualityPort
import com.seriouschoi.aircheck.core.domain.port.WeatherPort
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * WeatherService 단위 테스트
 * 
 * 계약:
 * - getWeather: WeatherPort 호출 → WeatherResponse 반환
 * - getAirQuality: AirQualityPort 호출 → AirQualityResponse 반환
 * - getCombined: 둘 다 호출 → CombinedWeatherResult 반환
 */
class WeatherServiceTest {

    private lateinit var weatherPort: WeatherPort
    private lateinit var airQualityPort: AirQualityPort
    private lateinit var service: WeatherService

    private val testLat = 37.5
    private val testLng = 127.0

    @BeforeEach
    fun setup() {
        weatherPort = mockk()
        airQualityPort = mockk()
        service = WeatherService(weatherPort, airQualityPort)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // getWeather 테스트
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `getWeather - 정상 응답`() {
        // Given
        val expected = createWeatherResponse()
        every { weatherPort.getWeather(testLat, testLng) } returns expected

        // When
        val result = service.getWeather(testLat, testLng)

        // Then
        assertEquals(expected, result)
        verify(exactly = 1) { weatherPort.getWeather(testLat, testLng) }
    }

    @Test
    fun `getWeather - null 응답`() {
        // Given
        every { weatherPort.getWeather(testLat, testLng) } returns null

        // When
        val result = service.getWeather(testLat, testLng)

        // Then
        assertNull(result)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // getAirQuality 테스트
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `getAirQuality - 정상 응답`() {
        // Given
        val expected = createAirQualityResponse()
        every { airQualityPort.getAirQuality(testLat, testLng) } returns expected

        // When
        val result = service.getAirQuality(testLat, testLng)

        // Then
        assertEquals(expected, result)
        verify(exactly = 1) { airQualityPort.getAirQuality(testLat, testLng) }
    }

    @Test
    fun `getAirQuality - null 응답`() {
        // Given
        every { airQualityPort.getAirQuality(testLat, testLng) } returns null

        // When
        val result = service.getAirQuality(testLat, testLng)

        // Then
        assertNull(result)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // getCombined 테스트
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `getCombined - 둘 다 성공`() {
        // Given
        val weather = createWeatherResponse()
        val airQuality = createAirQualityResponse()
        every { weatherPort.getWeather(testLat, testLng) } returns weather
        every { airQualityPort.getAirQuality(testLat, testLng) } returns airQuality

        // When
        val result = service.getCombined(testLat, testLng)

        // Then
        assertEquals(weather, result.weather)
        assertEquals(airQuality, result.airQuality)
    }

    @Test
    fun `getCombined - 날씨만 성공`() {
        // Given
        val weather = createWeatherResponse()
        every { weatherPort.getWeather(testLat, testLng) } returns weather
        every { airQualityPort.getAirQuality(testLat, testLng) } returns null

        // When
        val result = service.getCombined(testLat, testLng)

        // Then
        assertEquals(weather, result.weather)
        assertNull(result.airQuality)
    }

    @Test
    fun `getCombined - 대기질만 성공`() {
        // Given
        val airQuality = createAirQualityResponse()
        every { weatherPort.getWeather(testLat, testLng) } returns null
        every { airQualityPort.getAirQuality(testLat, testLng) } returns airQuality

        // When
        val result = service.getCombined(testLat, testLng)

        // Then
        assertNull(result.weather)
        assertEquals(airQuality, result.airQuality)
    }

    @Test
    fun `getCombined - 둘 다 실패`() {
        // Given
        every { weatherPort.getWeather(testLat, testLng) } returns null
        every { airQualityPort.getAirQuality(testLat, testLng) } returns null

        // When
        val result = service.getCombined(testLat, testLng)

        // Then
        assertNull(result.weather)
        assertNull(result.airQuality)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Helper
    // ══════════════════════════════════════════════════════════════════════════

    private fun createWeatherResponse() = WeatherResponse(
        current = CurrentWeather(
            temperature = 20.0,
            feelsLike = 18.0,
            precipitation = 0.0,
            humidity = 50,
            windSpeed = 1.5,
            windDirection = 180,
            weatherCondition = WeatherCondition.CLEAR,
            isDay = true
        ),
        hourlyForecast = emptyList()
    )

    private fun createAirQualityResponse() = AirQualityResponse(
        stationName = "중구",
        sidoName = "서울",
        dataTime = "2026-03-20 07:00",
        pm10 = 45,
        pm10Station = "중구",
        pm25 = 23,
        pm25Station = "중구"
    )
}
