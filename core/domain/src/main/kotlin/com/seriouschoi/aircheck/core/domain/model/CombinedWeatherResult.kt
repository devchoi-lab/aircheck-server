package com.seriouschoi.aircheck.core.domain.model

/**
 * 통합 날씨 결과. 모든 항목은 선택이며, 실패한 항목은 null로 두고 [errors]에 이유를 담는다.
 * 법정동을 찾지 못한 경우는 부분 결과가 아니라 전체 실패([com.seriouschoi.aircheck.core.domain.exception.LocationNotFoundException])다.
 */
data class CombinedWeatherResult(
    val kmaWeather: KmaWeatherResponse,
    val airQuality: AirQualityResponse?,
    val errors: List<DataError> = emptyList()
)

/**
 * CombinedWeatherResult 전용 에러 정보.
 * target: 어느 항목의 에러인지.
 * errorCode: 클라이언트가 UI 분기에 사용.
 * errorId: 서버 로그에서 grep으로 추적 가능한 UUID.
 */
data class DataError(
    val target: DataErrorTarget,
    val errorCode: DataErrorCode,
    val errorId: String
)

/** 에러가 난 항목. */
enum class DataErrorTarget {
    SHORT_FORECAST,     // 초단기실황 + 단기예보
    MID_LAND_FORECAST,  // 중기육상예보
    MID_TA_FORECAST,    // 중기기온예보
    AIR_QUALITY         // 대기질
}

/**
 * CombinedWeatherResult 전용 에러코드.
 */
enum class DataErrorCode {
    API_UNAVAILABLE     // 외부 API 호출 실패
}
