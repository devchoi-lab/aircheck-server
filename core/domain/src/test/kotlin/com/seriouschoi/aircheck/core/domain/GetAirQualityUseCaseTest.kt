package com.seriouschoi.aircheck.core.domain

import com.seriouschoi.aircheck.core.domain.exception.LocationNotFoundException
import com.seriouschoi.aircheck.core.domain.model.AirQualityResponse
import com.seriouschoi.aircheck.core.domain.model.BjdLocation
import com.seriouschoi.aircheck.core.domain.port.AirQualityPort
import com.seriouschoi.aircheck.core.domain.port.BjdPort
import com.seriouschoi.aircheck.core.domain.usecase.GetAirQualityUseCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class GetAirQualityUseCaseTest {

    @Test
    fun `법정동을 가져오지 못할 경우, 대기질을 호출하지 않고 위치 조회 실패를 리턴한다`() {
        val bjdPort = mockk<BjdPort>()
        val airQualityPort = mockk<AirQualityPort>()
        every { bjdPort.locate(any(), any()) } returns null

        // 대기질 포트가 호출됐는지 확인하려고 호출된 좌표를 기록한다.
        val requestedLats = mutableListOf<Double>()
        every {
            airQualityPort.getAirQuality(capture(requestedLats), any())
        } returns Result.success(mockk<AirQualityResponse>())
        val useCase = GetAirQualityUseCase(bjdPort, airQualityPort)

        val result = useCase.execute(37.5, 127.0)

        assertTrue(
            requestedLats.isEmpty(),
            "법정동을 찾지 못했는데 대기질을 ${requestedLats}로 조회했습니다."
        )
        assertTrue(
            result.exceptionOrNull() is LocationNotFoundException,
            "법정동을 찾지 못하면 LocationNotFoundException으로 실패해야 하는데, ${result}를 리턴했습니다."
        )
    }

    @Test
    fun `법정동을 찾으면, 법정동 대표 좌표로 대기질을 조회한다`() {
        val bjdPort = mockk<BjdPort>()
        val airQualityPort = mockk<AirQualityPort>()
        val airQuality = mockk<AirQualityResponse>()
        every { bjdPort.locate(any(), any()) } returns BjdLocation(
            doCode = 1100000000L,
            sigunguCode = 1168000000L,
            lat = 37.0,
            lng = 127.0
        )

        // 대기질 포트가 어떤 좌표로 호출됐는지 slot에 받아 둔다.
        // 어떤 좌표로 호출돼도 같은 값을 돌려주므로, 틀린 좌표여도 MockK 예외가 아니라 아래 단언의 메시지로 실패한다.
        val requestedLat = slot<Double>()
        val requestedLng = slot<Double>()
        every {
            airQualityPort.getAirQuality(capture(requestedLat), capture(requestedLng))
        } returns Result.success(airQuality)
        val useCase = GetAirQualityUseCase(bjdPort, airQualityPort)

        // 입력 좌표(37.5, 127.5)는 법정동 대표좌표(37.0, 127.0)와 다르게 준다.
        useCase.execute(37.5, 127.5)

        val requested = requestedLat.captured to requestedLng.captured
        assertEquals(
            37.0 to 127.0,
            requested,
            "대기질은 법정동 대표좌표(37.0, 127.0)로 조회해야 하는데, ${requested}로 조회했습니다."
        )
    }

    @Test
    fun `대기질 조회가 실패하면, 그 실패를 그대로 리턴한다`() {
        val bjdPort = mockk<BjdPort>()
        val airQualityPort = mockk<AirQualityPort>()
        val failure = RuntimeException("api down")

        //법정동 조회를 모킹
        every { bjdPort.locate(any(), any()) } returns BjdLocation(
            doCode = 1100000000L,
            sigunguCode = 1168000000L,
            lat = 37.0,
            lng = 127.0
        )
        //대기질 조회 실패를 모킹.
        every { airQualityPort.getAirQuality(any(), any()) } returns Result.failure(failure)

        val useCase = GetAirQualityUseCase(bjdPort, airQualityPort)

        val result = useCase.execute(37.5, 127.0)

        assertSame(failure, result.exceptionOrNull())
    }
}
