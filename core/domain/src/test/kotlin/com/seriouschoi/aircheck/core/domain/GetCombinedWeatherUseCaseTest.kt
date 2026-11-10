package com.seriouschoi.aircheck.core.domain

import com.seriouschoi.aircheck.core.domain.model.AirQualityResponse
import com.seriouschoi.aircheck.core.domain.model.BjdLocation
import com.seriouschoi.aircheck.core.domain.model.CombinedWeatherResult
import com.seriouschoi.aircheck.core.domain.model.DataErrorCode
import com.seriouschoi.aircheck.core.domain.model.KmaWeatherResponse
import com.seriouschoi.aircheck.core.domain.port.AirQualityPort
import com.seriouschoi.aircheck.core.domain.port.BjdPort
import com.seriouschoi.aircheck.core.domain.port.KmaWeatherPort
import com.seriouschoi.aircheck.core.domain.usecase.GetCombinedWeatherUseCase
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class GetCombinedWeatherUseCaseTest {

    @Test
    fun `장소를 가져오지 못했을경우, 결과에 위치 조회 실패 오류를 포함한다`() {
        val useCase = createUseCase(location = null)

        val result = useCase.execute(LAT, LNG)

        assertTrue(result.errorCodes().contains(DataErrorCode.LOCATION_NOT_FOUND))
    }

    @Test
    fun `공기질 정보 불러오기를 실패해도, 날씨 정보에 영향을 주지 않는다`() {
        val weather = completeWeather()
        val useCase = createUseCase(
            weather = Result.success(weather),
            air = Result.failure(RuntimeException())
        )

        val result = useCase.execute(LAT, LNG)

        assertTrue(result.errorCodes().contains(DataErrorCode.API_UNAVAILABLE))
        assertEquals(weather, result.kmaWeather)
        assertNull(result.airQuality)
    }

    @Test
    fun `날씨 정보 불러오기를 실패해도, 공기질 정보에 영향을 주지 않는다`() {
        val airQuality = mockk<AirQualityResponse>()
        val useCase = createUseCase(
            weather = Result.failure(RuntimeException()),
            air = Result.success(airQuality)
        )

        val result = useCase.execute(LAT, LNG)

        assertTrue(result.errorCodes().contains(DataErrorCode.API_UNAVAILABLE))
        assertNull(result.kmaWeather)
        assertEquals(airQuality, result.airQuality)
    }

    @Test
    fun `단기예보불러오기를 실패해도, 나머지 정보에 영향을 주지 않는다`() {
        val weather = completeWeather().copy(shortForecast = null)
        val airQuality = mockk<AirQualityResponse>()
        val useCase = createUseCase(
            weather = Result.success(weather),
            air = Result.success(airQuality)
        )

        val result = useCase.execute(LAT, LNG)

        // 실패한 단기예보 하나만 오류로 담기고, 나머지 값은 그대로 내려온다.
        assertEquals(listOf(DataErrorCode.API_UNAVAILABLE), result.errorCodes())
        assertEquals(weather, result.kmaWeather)
        assertEquals(airQuality, result.airQuality)
    }

    @Test
    fun `중기육상예보 불러오기를 실패해도, 나머지 정보에 영향을 주지 않는다`() {
        val weather = completeWeather().copy(midLandForecast = null)
        val airQuality = mockk<AirQualityResponse>()
        val useCase = createUseCase(
            weather = Result.success(weather),
            air = Result.success(airQuality)
        )

        val result = useCase.execute(LAT, LNG)

        assertEquals(listOf(DataErrorCode.API_UNAVAILABLE), result.errorCodes())
        assertEquals(weather, result.kmaWeather)
        assertEquals(airQuality, result.airQuality)
    }

    @Test
    fun `중기기온예보 불러오기를 실패해도, 나머지 정보에 영향을 주지 않는다`() {
        val weather = completeWeather().copy(midTaForecast = null)
        val airQuality = mockk<AirQualityResponse>()
        val useCase = createUseCase(
            weather = Result.success(weather),
            air = Result.success(airQuality)
        )

        val result = useCase.execute(LAT, LNG)

        assertEquals(listOf(DataErrorCode.API_UNAVAILABLE), result.errorCodes())
        assertEquals(weather, result.kmaWeather)
        assertEquals(airQuality, result.airQuality)
    }

    /**
     * 기본값은 모두 정상인 상태이고, 각 테스트는 확인하려는 조건만 바꿔서 넘긴다.
     */
    private fun createUseCase(
        location: BjdLocation? = BjdLocation(
            doCode = 1100000000L,
            sigunguCode = 1168000000L,
            lat = LAT,
            lng = LNG
        ),
        weather: Result<KmaWeatherResponse> = Result.success(completeWeather()),
        air: Result<AirQualityResponse?> = Result.success(mockk<AirQualityResponse>())
    ): GetCombinedWeatherUseCase {
        val bjdPort = mockk<BjdPort>()
        val kmaWeatherPort = mockk<KmaWeatherPort>()
        val airQualityPort = mockk<AirQualityPort>()

        every { bjdPort.locate(any(), any()) } returns location
        every { kmaWeatherPort.getWeather(any()) } returns weather
        every { airQualityPort.getAirQuality(any(), any()) } returns air

        return GetCombinedWeatherUseCase(
            bjdPort = bjdPort,
            kmaWeatherPort = kmaWeatherPort,
            airQualityPort = airQualityPort
        )
    }

    /** 유스케이스는 내용을 열어 보지 않으므로 하위 예보는 mock으로 둔다. */
    private fun completeWeather() = KmaWeatherResponse(
        shortForecast = mockk(),
        midLandForecast = mockk(),
        midTaForecast = mockk()
    )

    private fun CombinedWeatherResult.errorCodes() = errors.map { it.errorCode }

    private companion object {
        const val LAT = 37.5
        const val LNG = 127.0
    }
}
