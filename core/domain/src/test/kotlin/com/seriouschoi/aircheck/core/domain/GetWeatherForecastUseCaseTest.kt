package com.seriouschoi.aircheck.core.domain

import com.seriouschoi.aircheck.core.domain.exception.LocationNotFoundException
import com.seriouschoi.aircheck.core.domain.model.BjdLocation
import com.seriouschoi.aircheck.core.domain.model.ShortForecast
import com.seriouschoi.aircheck.core.domain.port.BjdPort
import com.seriouschoi.aircheck.core.domain.port.KmaWeatherPort
import com.seriouschoi.aircheck.core.domain.usecase.GetWeatherForecastUseCase
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class GetWeatherForecastUseCaseTest {

    @Test
    fun `법정동을 가져오지 못할 경우, 날씨를 조회하지 않고 위치 조회 실패를 리턴한다`() {
        val bjdPort = mockk<BjdPort>()
        val kmaWeatherPort = mockk<KmaWeatherPort>()
        every { bjdPort.locate(any(), any()) } returns null

        // 날씨 포트가 호출됐는지 확인하려고 호출된 법정동을 기록한다.
        val requested = mutableListOf<BjdLocation>()
        every { kmaWeatherPort.getShortForecast(capture(requested)) } returns Result.success(mockk<ShortForecast>())
        every { kmaWeatherPort.getMidLandForecast(capture(requested)) } returns Result.success(mockk())
        every { kmaWeatherPort.getMidTaForecast(capture(requested)) } returns Result.success(mockk())
        val useCase = GetWeatherForecastUseCase(bjdPort, kmaWeatherPort)

        val result = useCase.execute(37.5, 127.0)

        assertTrue(
            requested.isEmpty(),
            "법정동을 찾지 못했는데 날씨를 ${requested}로 조회했습니다."
        )
        assertTrue(
            result.exceptionOrNull() is LocationNotFoundException,
            "법정동을 찾지 못하면 LocationNotFoundException으로 실패해야 하는데, ${result}를 리턴했습니다."
        )
    }

    @Test
    fun `예보 하나가 실패해도, 나머지 예보는 그대로 리턴한다`() {
        val bjdPort = mockk<BjdPort>()
        val kmaWeatherPort = mockk<KmaWeatherPort>()
        val shortForecast = mockk<ShortForecast>()
        every { bjdPort.locate(any(), any()) } returns BjdLocation(
            doCode = 1100000000L,
            sigunguCode = 1168000000L,
            lat = 37.0,
            lng = 127.0
        )
        every { kmaWeatherPort.getShortForecast(any()) } returns Result.success(shortForecast)
        every { kmaWeatherPort.getMidLandForecast(any()) } returns Result.failure(RuntimeException("api down"))
        every { kmaWeatherPort.getMidTaForecast(any()) } returns Result.success(mockk())
        val useCase = GetWeatherForecastUseCase(bjdPort, kmaWeatherPort)

        val weather = useCase.execute(37.5, 127.0).getOrThrow()

        assertSame(shortForecast, weather.shortForecast)
        assertNull(weather.midLandForecast)
        assertNotNull(weather.midTaForecast)
    }
}
