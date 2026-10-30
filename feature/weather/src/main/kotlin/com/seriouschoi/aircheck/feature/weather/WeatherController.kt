package com.seriouschoi.aircheck.feature.weather

import com.seriouschoi.aircheck.core.domain.model.AirQualityResponse
import com.seriouschoi.aircheck.core.domain.model.CombinedWeatherResult
import com.seriouschoi.aircheck.core.domain.model.WeatherResponse
import com.seriouschoi.aircheck.core.domain.usecase.GetAirQualityUseCase
import com.seriouschoi.aircheck.core.domain.usecase.GetCombinedWeatherUseCase
import com.seriouschoi.aircheck.core.domain.usecase.GetWeatherForecastUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Weather", description = "날씨 및 대기질 API")
@RestController
@RequestMapping("/api/v1")
class WeatherController(
    private val getCombinedWeatherUseCase: GetCombinedWeatherUseCase,
    private val getWeatherForecastUseCase: GetWeatherForecastUseCase,
    private val getAirQualityUseCase: GetAirQualityUseCase
) {

    @Operation(
        summary = "통합 날씨/대기질 조회",
        description = "좌표 기반으로 날씨와 대기질 정보를 함께 조회합니다. " +
                "미세먼지는 밀도(μg/m³) 값과 등급을 함께 반환합니다."
    )
    @ApiResponses(value = [
        ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = [Content(schema = Schema(implementation = CombinedWeatherResult::class))]
        )
    ])
    @GetMapping("/weather")
    fun getWeather(
        @Parameter(description = "위도 (예: 37.5665)", example = "37.5665")
        @RequestParam lat: Double,
        @Parameter(description = "경도 (예: 126.9780)", example = "126.9780")
        @RequestParam lng: Double
    ): ResponseEntity<CombinedWeatherResult> =
        ResponseEntity.ok(getCombinedWeatherUseCase.execute(lat, lng))

    @Operation(
        summary = "날씨 예보 조회",
        description = "좌표 기반으로 현재 날씨와 48시간 예보를 조회합니다."
    )
    @ApiResponses(value = [
        ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = [Content(schema = Schema(implementation = WeatherResponse::class))]
        )
    ])
    @GetMapping("/weather/forecast")
    fun getForecast(
        @Parameter(description = "위도", example = "37.5665")
        @RequestParam lat: Double,
        @Parameter(description = "경도", example = "126.9780")
        @RequestParam lng: Double
    ): ResponseEntity<WeatherResponse?> =
        ResponseEntity.ok(getWeatherForecastUseCase.execute(lat, lng))

    @Operation(
        summary = "대기질 조회",
        description = "좌표 기반으로 대기질 정보를 조회합니다. " +
                "PM2.5, PM10 밀도(μg/m³)와 등급을 반환합니다."
    )
    @ApiResponses(value = [
        ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = [Content(schema = Schema(implementation = AirQualityResponse::class))]
        )
    ])
    @GetMapping("/weather/air")
    fun getAirQuality(
        @Parameter(description = "위도", example = "37.5665")
        @RequestParam lat: Double,
        @Parameter(description = "경도", example = "126.9780")
        @RequestParam lng: Double
    ): ResponseEntity<AirQualityResponse?> =
        ResponseEntity.ok(getAirQualityUseCase.execute(lat, lng))

    @Operation(
        summary = "다중 위치 일괄 조회",
        description = "여러 위치의 날씨/대기질 정보를 한 번에 조회합니다. " +
                "최대 10개 위치까지 지원합니다."
    )
    @ApiResponses(value = [
        ApiResponse(
            responseCode = "200",
            description = "조회 성공"
        )
    ])
    @PostMapping("/weather/batch")
    fun getBatchWeather(
        @RequestBody request: BatchWeatherRequest
    ): ResponseEntity<List<BatchWeatherItem>> {
        val results = runBlocking {
            request.locations.take(10).map { location ->
                async(Dispatchers.IO) {
                    val result = getCombinedWeatherUseCase.execute(location.lat, location.lng)
                    BatchWeatherItem(
                        lat = location.lat,
                        lng = location.lng,
                        weather = result.weather,
                        airQuality = result.airQuality
                    )
                }
            }.awaitAll()
        }
        return ResponseEntity.ok(results)
    }
}

// ── 배치 API 모델 ─────────────────────────────────────────────────────────

data class BatchWeatherRequest(
    val locations: List<LocationRequest>
)

data class LocationRequest(
    val lat: Double,
    val lng: Double
)

data class BatchWeatherItem(
    val lat: Double,
    val lng: Double,
    val weather: WeatherResponse?,
    val airQuality: AirQualityResponse?
)
