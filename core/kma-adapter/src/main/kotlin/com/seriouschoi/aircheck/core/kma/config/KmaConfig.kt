package com.seriouschoi.aircheck.core.kma.config

import com.seriouschoi.aircheck.core.kma.api.KmaMidApi
import com.seriouschoi.aircheck.core.kma.api.KmaApi
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient
import org.springframework.web.client.support.RestClientAdapter
import org.springframework.web.service.invoker.HttpServiceProxyFactory

@Configuration
class KmaConfig(
    @Value("\${kma.api.base-url}") private val baseUrl: String,
    @Value("\${kma.api.mid-url}") private val midUrl: String
) {

    @Bean
    fun kmaApi(): KmaApi {
        val restClient = RestClient.builder().baseUrl(baseUrl).build()
        return HttpServiceProxyFactory
            .builderFor(RestClientAdapter.create(restClient))
            .build()
            .createClient(KmaApi::class.java)
    }

    @Bean
    fun kmaMidApi(): KmaMidApi {
        val restClient = RestClient.builder().baseUrl(midUrl).build()
        return HttpServiceProxyFactory
            .builderFor(RestClientAdapter.create(restClient))
            .build()
            .createClient(KmaMidApi::class.java)
    }
}
