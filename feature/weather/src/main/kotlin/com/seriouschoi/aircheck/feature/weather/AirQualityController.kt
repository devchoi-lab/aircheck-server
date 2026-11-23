package com.seriouschoi.aircheck.feature.weather

import com.seriouschoi.aircheck.core.domain.model.AirQuality
import com.seriouschoi.aircheck.core.domain.usecase.GetAirQualityUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Weather", description = "날씨 및 대기질 API")
@RestController
@RequestMapping("/api/v1")
class AirQualityController(
    private val getAirQualityUseCase: GetAirQualityUseCase
) {
    @Operation(
        summary = "대기질 조회",
        description = "좌표 기반으로 대기질 정보를 조회합니다. " +
                "PM2.5, PM10 밀도(μg/m³)와 등급을 반환합니다."
    )
    @ApiResponses(value = [
        ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = [Content(schema = Schema(implementation = AirQuality::class))]
        ),
        ApiResponse(
            responseCode = "404",
            description = "해당 좌표의 법정동을 찾을 수 없거나, 쓸 수 있는 대기질 데이터가 없음"
        )
    ])
    @GetMapping("/weather/air")
    fun getAirQuality(
        @Parameter(description = "위도", example = "37.5665")
        @RequestParam lat: Double,
        @Parameter(description = "경도", example = "126.9780")
        @RequestParam lng: Double
    ): ResponseEntity<AirQuality> =
        ResponseEntity.ok(getAirQualityUseCase.execute(lat, lng).getOrThrow())
}
