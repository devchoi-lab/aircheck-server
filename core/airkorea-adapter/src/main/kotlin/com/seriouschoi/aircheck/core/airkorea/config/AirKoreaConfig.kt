package com.seriouschoi.aircheck.core.airkorea.config

import com.seriouschoi.aircheck.core.airkorea.api.StationApi
import com.seriouschoi.aircheck.core.airkorea.api.AirKoreaApi
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestClient
import org.springframework.web.client.support.RestClientAdapter
import org.springframework.web.service.invoker.HttpServiceProxyFactory

@Configuration
class AirKoreaConfig(
    @Value("\${airkorea.base-url}") private val baseUrl: String,
    @Value("\${airkorea.station-url}") private val stationUrl: String
) {

    @Bean
    fun airKoreaApi(): AirKoreaApi {
        val restClient = RestClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader("User-Agent", "AirCheckServer/1.0")
            .requestFactory(SimpleClientHttpRequestFactory().apply {
                setConnectTimeout(10_000)
                setReadTimeout(30_000)
            })
            .build()
        return HttpServiceProxyFactory
            .builderFor(RestClientAdapter.create(restClient))
            .build()
            .createClient(AirKoreaApi::class.java)
    }

    @Bean
    fun stationApi(): StationApi {
        val restClient = RestClient.builder()
            .baseUrl(stationUrl)
            .defaultHeader("User-Agent", "AirCheckServer/1.0")
            .requestFactory(SimpleClientHttpRequestFactory().apply {
                setConnectTimeout(10_000)
                setReadTimeout(60_000)
            })
            .build()
        return HttpServiceProxyFactory
            .builderFor(RestClientAdapter.create(restClient))
            .build()
            .createClient(StationApi::class.java)
    }
}
