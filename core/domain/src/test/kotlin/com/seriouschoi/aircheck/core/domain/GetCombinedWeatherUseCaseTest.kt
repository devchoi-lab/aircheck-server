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
import io.mockk.slot
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class GetCombinedWeatherUseCaseTest {

    @Test
    fun `장소를 가져오지 못했을경우, 날씨와 대기질을 조회하지 않고 위치 조회 실패를 리턴한다`() {
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

        val result = useCase.execute(LAT, LNG)

        assertTrue(
            result.exceptionOrNull() is LocationNotFoundException,
            "법정동을 찾지 못하면 LocationNotFoundException으로 실패해야 하는데, ${result}를 리턴했습니다."
        )
        assertTrue(calls.isEmpty(), "법정동을 찾지 못했는데 ${calls}를 조회했습니다.")
    }

    @Test
    fun `법정동을 찾으면, 법정동 대표 좌표로 대기질을 조회한다`() {
        val bjdPort = mockk<BjdPort>()
        val kmaWeatherPort = mockk<KmaWeatherPort>()
        val airQualityPort = mockk<AirQualityPort>()
        // 입력 좌표(LAT, LNG)와 다른 대표 좌표를 준다.
        every { bjdPort.locate(any(), any()) } returns BjdLocation(
            doCode = 1100000000L,
            sigunguCode = 1168000000L,
            lat = BJD_LAT,
            lng = BJD_LNG
        )
        every { kmaWeatherPort.getShortForecast(any()) } returns Result.success(mockk())
        every { kmaWeatherPort.getMidLandForecast(any()) } returns Result.success(mockk())
        every { kmaWeatherPort.getMidTaForecast(any()) } returns Result.success(mockk())

        // 어떤 좌표로 호출돼도 같은 값을 돌려주므로, 틀린 좌표여도 MockK 예외가 아니라 아래 단언의 메시지로 실패한다.
        val requestedLat = slot<Double>()
        val requestedLng = slot<Double>()
        every {
            airQualityPort.getAirQuality(capture(requestedLat), capture(requestedLng))
        } returns Result.success(mockk())
        val useCase = GetCombinedWeatherUseCase(bjdPort, kmaWeatherPort, airQualityPort)

        useCase.execute(LAT, LNG)

        val requested = requestedLat.captured to requestedLng.captured
        assertEquals(
            BJD_LAT to BJD_LNG,
            requested,
            "대기질은 법정동 대표좌표($BJD_LAT, $BJD_LNG)로 조회해야 하는데, ${requested}로 조회했습니다."
        )
    }

    @Test
    fun `어떤 항목 하나가 실패해도, 그 항목만 오류로 담기고 나머지 정보는 그대로 리턴한다`() {
        val failures = mapOf(
            DataErrorTarget.SHORT_FORECAST to createUseCase(short = Result.failure(RuntimeException())),
            DataErrorTarget.MID_LAND_FORECAST to createUseCase(midLand = Result.failure(RuntimeException())),
            DataErrorTarget.MID_TA_FORECAST to createUseCase(midTa = Result.failure(RuntimeException())),
            DataErrorTarget.AIR_QUALITY to createUseCase(air = Result.failure(RuntimeException()))
        )

        failures.forEach { (failed, useCase) ->
            val result = useCase.execute(LAT, LNG).getOrThrow()

            assertEquals(listOf(failed), result.errorTargets(), "$failed 하나만 실패했는데 오류 목록이 다릅니다.")
            DataErrorTarget.entries.forEach { target ->
                val value = result.valueOf(target)
                if (target == failed) assertNull(value, "$failed 는 실패했으니 값이 비어 있어야 합니다.")
                else assertNotNull(value, "$failed 만 실패했는데 $target 값이 비었습니다.")
            }
        }
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
        assertEquals(DataErrorTarget.entries.toSet(), result.getOrThrow().errorTargets().toSet())
    }

    /**
     * 기본값은 모두 정상인 상태이고, 각 테스트는 확인하려는 조건만 바꿔서 넘긴다.
     */
    private fun createUseCase(
        short: Result<ShortForecast> = Result.success(mockk()),
        midLand: Result<MidLandForecast> = Result.success(mockk()),
        midTa: Result<MidTaForecast> = Result.success(mockk()),
        air: Result<AirQuality> = Result.success(mockk())
    ): GetCombinedWeatherUseCase {
        val bjdPort = mockk<BjdPort>()
        val kmaWeatherPort = mockk<KmaWeatherPort>()
        val airQualityPort = mockk<AirQualityPort>()

        every { bjdPort.locate(any(), any()) } returns BjdLocation(
            doCode = 1100000000L,
            sigunguCode = 1168000000L,
            lat = LAT,
            lng = LNG
        )
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

    /** 오류 대상에 해당하는 값. 실패했으면 null이다. */
    private fun CombinedWeather.valueOf(target: DataErrorTarget): Any? = when (target) {
        DataErrorTarget.SHORT_FORECAST -> kmaWeather.shortForecast
        DataErrorTarget.MID_LAND_FORECAST -> kmaWeather.midLandForecast
        DataErrorTarget.MID_TA_FORECAST -> kmaWeather.midTaForecast
        DataErrorTarget.AIR_QUALITY -> airQuality
    }

    private companion object {
        const val LAT = 37.5
        const val LNG = 127.0

        /** 입력 좌표와 다른 법정동 대표 좌표 */
        const val BJD_LAT = 37.0
        const val BJD_LNG = 127.5
    }
}
