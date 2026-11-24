package com.seriouschoi.aircheck.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.servers.Server
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun openAPI(): OpenAPI = OpenAPI()
        .info(
            Info()
                .title("오늘공기 API")
                .description("""
                    날씨 및 대기질 정보를 제공하는 API입니다.
                    
                    ## 주요 기능
                    - 현재 날씨 및 48시간 예보
                    - 대기질 정보 (PM2.5, PM10 밀도 및 등급)
                    
                    ## 대기질 밀도 단위
                    - PM2.5, PM10: μg/m³ (마이크로그램/세제곱미터)
                    - 등급은 한국 환경부 기준 적용
                    
                    ## 데이터 출처
                    - 날씨: 기상청
                    - 대기질: 에어코리아 (한국환경공단)
                """.trimIndent())
                .version("1.0.0")
                .contact(
                    Contact()
                        .name("오늘공기")
                        .email("seriouschoi@gmail.com")
                )
                .license(
                    License()
                        .name("Private")
                )
        )
        .servers(
            listOf(
                Server().url("https://api.todaygonggi.com").description("Production"),
                Server().url("http://localhost:8080").description("Local")
            )
        )
}
