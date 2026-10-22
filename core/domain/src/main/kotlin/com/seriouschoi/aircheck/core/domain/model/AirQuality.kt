package com.seriouschoi.aircheck.core.domain.model

import io.swagger.v3.oas.annotations.media.Schema
import kotlinx.serialization.Serializable

/**
 * 측정기기 상태 플래그.
 * null이면 정상. 값이 있으면 해당 시간대 측정값 신뢰 불가.
 *
 * @param apiValue 에어코리아 API에서 내려오는 한글 문자열
 */
enum class MeasurementFlag(val apiValue: String) {
    INSPECTION_AND_CALIBRATION("점검및교정"),
    EQUIPMENT_CHECK("장비점검"),
    DATA_ABNORMAL("자료이상"),
    COMMUNICATION_FAILURE("통신장애")
}

/**
 * 대기질 응답 (앱으로 전달)
 *
 * 밀도 값과 가스 농도를 반환합니다.
 * GradeAirKorea: 에어코리아 1시간 등급 (1:좋음 / 2:보통 / 3:나쁨 / 4:매우나쁨).
 * WHO 기준 등급은 앱에서 자체 계산합니다.
 *
 * PM10/PM25 값이 메인 측정소에 없으면 주변 측정소에서 가져오며,
 * 이 경우 pm10Station/pm25Station에 해당 측정소 정보가 표시됩니다.
 */
@Schema(description = "대기질 정보")
@Serializable
data class AirQualityResponse(
    @Schema(description = "메인 측정소 이름", example = "중구")
    val stationName: String,

    @Schema(description = "시도명", example = "서울")
    val sidoName: String,

    @Schema(description = "측정 시간", example = "2024-03-17 21:00")
    val dataTime: String,

    // 미세먼지
    @Schema(description = "미세먼지(PM10) 밀도 (μg/m³)", example = "45")
    val pm10: Int?,

    @Schema(description = "PM10 측정소", example = "중구")
    val pm10Station: String,

    @Schema(description = "PM10 에어코리아 1시간 등급 (1:좋음/2:보통/3:나쁨/4:매우나쁨)", example = "2")
    val pm10GradeAirKorea: Int? = null,

    @Schema(description = "PM10 측정기기 상태 플래그. null=정상")
    val pm10Flag: MeasurementFlag? = null,

    @Schema(description = "초미세먼지(PM2.5) 밀도 (μg/m³)", example = "23")
    val pm25: Int?,

    @Schema(description = "PM2.5 측정소", example = "소사본동")
    val pm25Station: String,

    @Schema(description = "PM2.5 에어코리아 1시간 등급 (1:좋음/2:보통/3:나쁨/4:매우나쁨)", example = "2")
    val pm25GradeAirKorea: Int? = null,

    @Schema(description = "PM2.5 측정기기 상태 플래그. null=정상")
    val pm25Flag: MeasurementFlag? = null,

    // 통합지수
    @Schema(description = "통합대기환경지수 (CAI)", example = "68")
    val khaiValue: Int? = null,

    @Schema(description = "통합대기환경지수 등급 (1~4)", example = "2")
    val khaiGrade: Int? = null,

    // 가스류 (ppm)
    @Schema(description = "아황산가스 SO2 (ppm)", example = "0.003")
    val so2: Double? = null,

    @Schema(description = "SO2 에어코리아 등급 (1:좋음/2:보통/3:나쁨/4:매우나쁨)", example = "1")
    val so2GradeAirKorea: Int? = null,

    @Schema(description = "SO2 측정기기 상태 플래그. null=정상")
    val so2Flag: MeasurementFlag? = null,

    @Schema(description = "일산화탄소 CO (ppm)", example = "0.4")
    val co: Double? = null,

    @Schema(description = "CO 에어코리아 등급 (1:좋음/2:보통/3:나쁨/4:매우나쁨)", example = "1")
    val coGradeAirKorea: Int? = null,

    @Schema(description = "CO 측정기기 상태 플래그. null=정상")
    val coFlag: MeasurementFlag? = null,

    @Schema(description = "오존 O3 (ppm)", example = "0.045")
    val o3: Double? = null,

    @Schema(description = "O3 에어코리아 등급 (1:좋음/2:보통/3:나쁨/4:매우나쁨)", example = "1")
    val o3GradeAirKorea: Int? = null,

    @Schema(description = "O3 측정기기 상태 플래그. null=정상")
    val o3Flag: MeasurementFlag? = null,

    @Schema(description = "이산화질소 NO2 (ppm)", example = "0.025")
    val no2: Double? = null,

    @Schema(description = "NO2 에어코리아 등급 (1:좋음/2:보통/3:나쁨/4:매우나쁨)", example = "1")
    val no2GradeAirKorea: Int? = null,

    @Schema(description = "NO2 측정기기 상태 플래그. null=정상")
    val no2Flag: MeasurementFlag? = null
)
