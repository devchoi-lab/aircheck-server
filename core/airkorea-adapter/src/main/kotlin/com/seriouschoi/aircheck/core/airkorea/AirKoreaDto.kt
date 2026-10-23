package com.seriouschoi.aircheck.core.airkorea

import com.seriouschoi.aircheck.core.domain.model.MeasurementFlag
import kotlinx.serialization.Serializable

// ── 에어코리아 API 응답 모델 ───────────────────────────────────────────────

@Serializable
internal data class AirKoreaResponse(val response: AirKoreaBody? = null)

@Serializable
internal data class AirKoreaBody(val body: AirKoreaItems? = null)

@Serializable
internal data class AirKoreaItems(val items: List<AirKoreaItem> = emptyList())

/**
 * 에어코리아 실시간 대기질 측정 데이터 (ver=1.5 기준)
 *
 * 측정값이 없거나 기기 이상 시 "-"로 내려옴.
 * toIntOrNull() / toDoubleOrNull() 으로 파싱하면 null 처리 가능.
 *
 * 등급 기준 (1:좋음 / 2:보통 / 3:나쁨 / 4:매우나쁨)
 *
 * pm10Grade/pm25Grade = 24시간 등급, pm10Grade1h/pm25Grade1h = 1시간 등급.
 * 현재 화면에 1시간 농도(pm10Value)와 24시간 등급(pm10Grade)을 함께 쓰고 있어 시간 기준이 다름 — 추후 검토 필요.
 *
 * Flag 필드: 값이 있으면 비정상 상태 (점검및교정/장비점검/자료이상/통신장애). null이면 정상.
 * AirQualityResponse에 MeasurementFlag enum으로 변환하여 전달.
 */
@Serializable
internal data class AirKoreaItem(
    /** 측정소명 (예: "중구", "강남구"). ver≥1.4부터 응답에 포함 */
    val stationName: String = "",
    /** 측정소 코드. ver≥1.4부터 응답에 포함. 현재 AirQualityResponse에 미전달 */
    val stationCode: String = "",
    /** 시도명 (예: "서울", "경기"). API 응답이 아닌 StationInfo에서 채워짐 */
    val sidoName: String = "",
    /**
     * 측정일시 (yyyy-MM-dd HH:mm, KST 기준)
     * UTC 변환 필요 시 ZoneId("Asia/Seoul") 사용
     */
    val dataTime: String = "",
    /**
     * 측정망 정보. ver≥1.2. 현재 미사용.
     * 가능한 값: 도시대기 / 도로변대기 / 국가배경농도 / 교외대기 / 항만 (문서 v1.4 기준 고정)
     */
    val mangName: String = "",

    // ── 미세먼지 ──────────────────────────────────────────────────────────────
    /** PM10 농도 (㎍/㎥, 1시간값). 파싱: toIntOrNull() */
    val pm10Value: String = "-",
    /**
     * PM10 24시간 등급 (1:좋음 / 2:보통 / 3:나쁨 / 4:매우나쁨).
     * 주의: pm10Value(1시간 농도)와 시간 기준이 다름. AirQualityResponse에 미전달 (1시간 등급 pm10Grade1h 사용).
     * 파싱: toIntOrNull()
     */
    val pm10Grade: String = "-",
    /** PM10 24시간 예측이동농도 (㎍/㎥). ver≥1.1. 미세먼지 경보 발령 기준값 (#46 연관). 현재 미사용. 파싱: toIntOrNull() */
    val pm10Value24: String = "-",
    /**
     * PM10 1시간 등급 (1:좋음 / 2:보통 / 3:나쁨 / 4:매우나쁨). ver≥1.3.
     * pm10Value(1시간 농도)와 시간 기준이 일치. → AirQualityResponse.pm10GradeAirKorea로 전달.
     * 파싱: toIntOrNull()
     */
    val pm10Grade1h: String = "-",
    /**
     * PM10 측정기기 상태 플래그. null이면 정상.
     * 가능한 값: 점검및교정 / 장비점검 / 자료이상 / 통신장애
     * 값이 있으면 해당 시간대 측정값 신뢰 불가. → AirQualityResponse.pm10Flag(MeasurementFlag)로 전달.
     */
    val pm10Flag: String? = null,

    /** PM2.5 농도 (㎍/㎥, 1시간값). ver≥1.0. 파싱: toIntOrNull() */
    val pm25Value: String = "-",
    /**
     * PM2.5 24시간 등급 (1:좋음 / 2:보통 / 3:나쁨 / 4:매우나쁨).
     * 주의: pm25Value(1시간 농도)와 시간 기준이 다름. AirQualityResponse에 미전달 (1시간 등급 pm25Grade1h 사용).
     * 파싱: toIntOrNull()
     */
    val pm25Grade: String = "-",
    /** PM2.5 24시간 예측이동농도 (㎍/㎥). ver≥1.1. 미세먼지 경보 발령 기준값 (#46 연관). 현재 미사용. 파싱: toIntOrNull() */
    val pm25Value24: String = "-",
    /**
     * PM2.5 1시간 등급 (1:좋음 / 2:보통 / 3:나쁨 / 4:매우나쁨). ver≥1.3.
     * pm25Value(1시간 농도)와 시간 기준이 일치. → AirQualityResponse.pm25GradeAirKorea로 전달.
     * 파싱: toIntOrNull()
     */
    val pm25Grade1h: String = "-",
    /**
     * PM2.5 측정기기 상태 플래그. null이면 정상.
     * 가능한 값: 점검및교정 / 장비점검 / 자료이상 / 통신장애
     * 값이 있으면 해당 시간대 측정값 신뢰 불가. → AirQualityResponse.pm25Flag(MeasurementFlag)로 전달.
     */
    val pm25Flag: String? = null,

    // ── 통합지수 ──────────────────────────────────────────────────────────────
    /**
     * 통합대기환경수치 (CAI, Comprehensive Air-quality Index. khai = Korea Haze Air Index)
     * 모든 오염물질을 종합한 대기질 수치. 예: "75"
     * 등급 범위: 0~50(좋음), 51~100(보통), 101~250(나쁨), 251~(매우나쁨)
     * 파싱: toIntOrNull()
     */
    val khaiValue: String = "-",
    /**
     * 통합대기환경지수 등급 (1:좋음 / 2:보통 / 3:나쁨 / 4:매우나쁨)
     * khaiValue를 등급으로 변환한 값. 파싱: toIntOrNull()
     */
    val khaiGrade: String = "-",

    // ── 가스류 ────────────────────────────────────────────────────────────────
    /** 아황산가스 농도 (ppm, ver≥1.5부터 소수점 4자리). 파싱: toDoubleOrNull() */
    val so2Value: String = "-",
    /** 아황산가스 등급 (1:좋음 / 2:보통 / 3:나쁨 / 4:매우나쁨). → AirQualityResponse.so2GradeAirKorea로 전달. 파싱: toIntOrNull() */
    val so2Grade: String = "-",
    /**
     * 아황산가스 측정기기 상태 플래그. null이면 정상.
     * 가능한 값: 점검및교정 / 장비점검 / 자료이상 / 통신장애. → AirQualityResponse.so2Flag(MeasurementFlag)로 전달.
     */
    val so2Flag: String? = null,

    /** 일산화탄소 농도 (ppm, ver≥1.5부터 소수점 2자리). 파싱: toDoubleOrNull() */
    val coValue: String = "-",
    /** 일산화탄소 등급 (1:좋음 / 2:보통 / 3:나쁨 / 4:매우나쁨). → AirQualityResponse.coGradeAirKorea로 전달. 파싱: toIntOrNull() */
    val coGrade: String = "-",
    /**
     * 일산화탄소 측정기기 상태 플래그. null이면 정상.
     * 가능한 값: 점검및교정 / 장비점검 / 자료이상 / 통신장애. → AirQualityResponse.coFlag(MeasurementFlag)로 전달.
     */
    val coFlag: String? = null,

    /** 오존 농도 (ppm, ver≥1.5부터 소수점 4자리). 파싱: toDoubleOrNull() */
    val o3Value: String = "-",
    /** 오존 등급 (1:좋음 / 2:보통 / 3:나쁨 / 4:매우나쁨). → AirQualityResponse.o3GradeAirKorea로 전달. 파싱: toIntOrNull() */
    val o3Grade: String = "-",
    /**
     * 오존 측정기기 상태 플래그. null이면 정상.
     * 가능한 값: 점검및교정 / 장비점검 / 자료이상 / 통신장애. → AirQualityResponse.o3Flag(MeasurementFlag)로 전달.
     */
    val o3Flag: String? = null,

    /** 이산화질소 농도 (ppm, ver≥1.5부터 소수점 4자리). 파싱: toDoubleOrNull() */
    val no2Value: String = "-",
    /** 이산화질소 등급 (1:좋음 / 2:보통 / 3:나쁨 / 4:매우나쁨). → AirQualityResponse.no2GradeAirKorea로 전달. 파싱: toIntOrNull() */
    val no2Grade: String = "-",
    /**
     * 이산화질소 측정기기 상태 플래그. null이면 정상.
     * 가능한 값: 점검및교정 / 장비점검 / 자료이상 / 통신장애. → AirQualityResponse.no2Flag(MeasurementFlag)로 전달.
     */
    val no2Flag: String? = null
)

// ── 변환 유틸 ──────────────────────────────────────────────────────────────

internal fun String?.toMeasurementFlag(): MeasurementFlag =
    MeasurementFlag.entries.firstOrNull { it.apiValue == (this ?: "") } ?: MeasurementFlag.NORMAL
