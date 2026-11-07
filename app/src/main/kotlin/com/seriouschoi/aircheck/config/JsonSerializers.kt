package com.seriouschoi.aircheck.config

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import org.springframework.boot.jackson.JsonComponent
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * API 응답의 시각 JSON 형식을 코드로 고정한다.
 *
 * Spring Boot/Jackson 기본 설정(WRITE_DATES_AS_TIMESTAMPS 등)이 바뀌어도 형식이 달라지지 않도록
 * 기본 시각 모듈에 기대지 않고 직렬화기를 직접 정의한다. 전역(모든 응답)에 적용된다.
 * (@JsonComponent는 Spring Boot가 ObjectMapper에 자동 등록한다)
 */

/** Instant → ISO 8601 UTC 문자열. 예: "2026-03-20T00:00:00Z" */
@JsonComponent
class InstantJsonSerializer : JsonSerializer<Instant>() {
    override fun serialize(value: Instant, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeString(DateTimeFormatter.ISO_INSTANT.format(value))
    }
}

/** LocalDate → ISO 8601 날짜 문자열. 예: "2026-03-20" */
@JsonComponent
class LocalDateJsonSerializer : JsonSerializer<LocalDate>() {
    override fun serialize(value: LocalDate, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeString(DateTimeFormatter.ISO_LOCAL_DATE.format(value))
    }
}
