package com.seriouschoi.aircheck.core.kma.common

import com.seriouschoi.aircheck.core.domain.port.ApiUsagePort
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import java.time.ZoneId

internal val KST_ZONE: ZoneId = ZoneId.of("Asia/Seoul")

internal val forecastJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
}

private val usageLogger = KotlinLogging.logger {}

internal fun <T> runWithUsage(apiType: String, apiUsagePort: ApiUsagePort?, block: () -> T): T {
    val startTime = System.currentTimeMillis()
    return try {
        val result = block()
        val elapsed = System.currentTimeMillis() - startTime
        apiUsagePort?.recordSuccess(apiType, elapsed)
        usageLogger.debug { "$apiType success (${elapsed}ms)" }
        result
    } catch (e: Exception) {
        apiUsagePort?.recordFailure(apiType, e.message)
        usageLogger.error(e) { "$apiType failed: ${e.message}" }
        throw e
    }
}
