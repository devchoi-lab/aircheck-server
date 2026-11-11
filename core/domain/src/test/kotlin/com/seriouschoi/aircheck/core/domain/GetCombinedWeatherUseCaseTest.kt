package com.seriouschoi.aircheck.core.domain

import com.seriouschoi.aircheck.core.domain.exception.LocationNotFoundException
import com.seriouschoi.aircheck.core.domain.model.AirQuality
import com.seriouschoi.aircheck.core.domain.model.BjdLocation
import com.seriouschoi.aircheck.core.domain.model.CombinedWeather
import com.seriouschoi.aircheck.core.domain.model.DataErrorTarget
import com.seriouschoi.aircheck.core.domain.model.MidLandForecast
import com.seriouschoi.aircheck.core.domain.model.MidTaForecast
import com.seriouschoi.aircheck.core.domain.model.ShortForecast
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
    fun `장소를 가져오지 못했을경우, 위치 조회 실패를 리턴한다`() {
        val useCase = createUseCase(location = null)

        val result = useCase.execute(LAT, LNG)

        assertTrue(
            result.exceptionOrNull() is LocationNotFoundException,
            "법정동을 찾지 못하면 LocationNotFoundException으로 실패해야 하는데, ${result}를 리턴했습니다."
        )
    }

    @Test
    fun `장소를 가져오지 못했을경우, 날씨와 대기질을 조회하지 않는다`() {
        val bjdPort = mockk<BjdPort>()
        val kmaWeatherPort = mockk<KmaWeatherPort>()
        val airQualityPort = mockk<AirQualityPort>()
        every { bjdPort.locate(any(), any()) } returns null

        // 외부 API 포트가 호출됐는지 확인하려고 호출된 이름을 기록한다.
        val calls = mutableListOf<String>()
        every { kmaWeatherPort.getShortForecast(any()) } answers { calls.add("단기예보"); Result.success(mockk()) }
        every { kmaWeatherPort.getMidLandForecast(any()) } answers { calls.add("중기육상예보"); Result.success(mockk()) }
        every { kmaWeatherPort.getMidTaForecast(any()) } answers { calls.add("중기기온예보"); Result.success(mockk()) }
        every { airQualityPort.getAirQuality(any(), any()) } answers { calls.add("대기질"); Result.success(mockk()) }
        val useCase = GetCombinedWeatherUseCase(bjdPort, kmaWeatherPort, airQualityPort)

        useCase.execute(LAT, LNG)

        assertTrue(
            calls.isEmpty(),
            "법정동을 찾지 못했는데 ${calls}를 조회했습니다."
        )
    }

    @Test
    fun `공기질 정보 불러오기를 실패해도, 날씨 정보에 영향을 주지 않는다`() {
        val short = mockk<ShortForecast>()
        val useCase = createUseCase(
            short = Result.success(short),
            air = Result.failure(RuntimeException())
        )

        val result = useCase.execute(LAT, LNG).getOrThrow()

        assertEquals(listOf(DataErrorTarget.AIR_QUALITY), result.errorTargets())
        assertSame(short, result.kmaWeather.shortForecast)
        assertNull(result.airQuality)
    }

    @Test
    fun `날씨 정보 불러오기를 모두 실패해도, 공기질 정보에 영향을 주지 않는다`() {
        val airQuality = mockk<AirQuality>()
        val useCase = createUseCase(
            short = Result.failure(RuntimeException()),
            midLand = Result.failure(RuntimeException()),
            midTa = Result.failure(RuntimeException()),
            air = Result.success(airQuality)
        )

        val result = useCase.execute(LAT, LNG).getOrThrow()

        assertEquals(
            listOf(
                DataErrorTarget.SHORT_FORECAST,
                DataErrorTarget.MID_LAND_FORECAST,
                DataErrorTarget.MID_TA_FORECAST
            ),
            result.errorTargets()
        )
        assertSame(airQuality, result.airQuality)
    }

    @Test
    fun `단기예보 불러오기를 실패해도, 나머지 정보에 영향을 주지 않는다`() {
        val result = createUseCase(short = Result.failure(RuntimeException()))
            .execute(LAT, LNG).getOrThrow()

        // 실패한 단기예보 하나만 오류로 담기고, 나머지 값은 그대로 내려온다.
        assertEquals(listOf(DataErrorTarget.SHORT_FORECAST), result.errorTargets())
        assertNull(result.kmaWeather.shortForecast)
        assertNotNull(result.kmaWeather.midLandForecast)
        assertNotNull(result.kmaWeather.midTaForecast)
        assertNotNull(result.airQuality)
    }

    @Test
    fun `중기육상예보 불러오기를 실패해도, 나머지 정보에 영향을 주지 않는다`() {
        val result = createUseCase(midLand = Result.failure(RuntimeException()))
            .execute(LAT, LNG).getOrThrow()

        assertEquals(listOf(DataErrorTarget.MID_LAND_FORECAST), result.errorTargets())
        assertNotNull(result.kmaWeather.shortForecast)
        assertNull(result.kmaWeather.midLandForecast)
        assertNotNull(result.kmaWeather.midTaForecast)
        assertNotNull(result.airQuality)
    }

    @Test
    fun `중기기온예보 불러오기를 실패해도, 나머지 정보에 영향을 주지 않는다`() {
        val result = createUseCase(midTa = Result.failure(RuntimeException()))
            .execute(LAT, LNG).getOrThrow()

        assertEquals(listOf(DataErrorTarget.MID_TA_FORECAST), result.errorTargets())
        assertNotNull(result.kmaWeather.shortForecast)
        assertNotNull(result.kmaWeather.midLandForecast)
        assertNull(result.kmaWeather.midTaForecast)
        assertNotNull(result.airQuality)
    }

    @Test
    fun `모든 항목이 실패해도, 실패가 아니라 항목별 오류를 담아 리턴한다`() {
        val useCase = createUseCase(
            short = Result.failure(RuntimeException()),
            midLand = Result.failure(RuntimeException()),
            midTa = Result.failure(RuntimeException()),
            air = Result.failure(RuntimeException())
        )

        val result = useCase.execute(LAT, LNG)

        assertTrue(result.isSuccess, "모든 항목이 실패해도 성공으로 리턴해야 하는데, ${result}를 리턴했습니다.")
        assertEquals(4, result.getOrThrow().errors.size)
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
        short: Result<ShortForecast> = Result.success(mockk()),
        midLand: Result<MidLandForecast> = Result.success(mockk()),
        midTa: Result<MidTaForecast> = Result.success(mockk()),
        air: Result<AirQuality> = Result.success(mockk())
    ): GetCombinedWeatherUseCase {
        val bjdPort = mockk<BjdPort>()
        val kmaWeatherPort = mockk<KmaWeatherPort>()
        val airQualityPort = mockk<AirQualityPort>()

        every { bjdPort.locate(any(), any()) } returns location
        every { kmaWeatherPort.getShortForecast(any()) } returns short
        every { kmaWeatherPort.getMidLandForecast(any()) } returns midLand
        every { kmaWeatherPort.getMidTaForecast(any()) } returns midTa
        every { airQualityPort.getAirQuality(any(), any()) } returns air

        return GetCombinedWeatherUseCase(
            bjdPort = bjdPort,
            kmaWeatherPort = kmaWeatherPort,
            airQualityPort = airQualityPort
        )
    }

    private fun CombinedWeather.errorTargets() = errors.map { it.target }

    private companion object {
        const val LAT = 37.5
        const val LNG = 127.0
    }
}
