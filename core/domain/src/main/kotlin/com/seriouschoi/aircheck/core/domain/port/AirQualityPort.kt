package com.seriouschoi.aircheck.core.domain.port

import com.seriouschoi.aircheck.core.domain.model.AirQuality

/**
 * 대기질 데이터 조회 Port (아웃바운드)
 *
 * 구현체: AirKoreaAdapter 등
 * 실패 시 Result.failure로 반환 — 호출부에서 에러 원인을 구분할 수 있도록 한다.
 * 쓸 수 있는 데이터가 없는 경우는 성공이 아니라 [com.seriouschoi.aircheck.core.domain.exception.AirQualityNotFoundException]으로 실패한다.
 */
interface AirQualityPort {
    fun getAirQuality(lat: Double, lng: Double): Result<AirQuality>
}
