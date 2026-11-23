package com.seriouschoi.aircheck.feature.weather

import com.seriouschoi.aircheck.core.domain.model.AirQuality
import com.seriouschoi.aircheck.core.domain.model.KmaWeather

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
    val kmaWeather: KmaWeather?,
    val airQuality: AirQuality?
)
