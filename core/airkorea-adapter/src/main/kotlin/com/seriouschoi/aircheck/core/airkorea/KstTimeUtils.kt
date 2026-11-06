package com.seriouschoi.aircheck.core.airkorea

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val KST = ZoneId.of("Asia/Seoul")
private val KST_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

/**
 * 에어코리아 측정일시(KST, "yyyy-MM-dd HH:mm")를 Instant(UTC)로 변환.
 * 에어코리아 API는 자정을 "24:00"으로 표기하는 경우가 있음 (예: "2026-10-20 24:00").
 * 이 경우 다음날 "00:00"으로 보정 후 파싱.
 * 파싱 실패 시 예외 발생 — 원문을 그대로 반환하면 잘못된 형식이 정상처럼 내려가므로 허용하지 않음.
 */
internal fun String.kstToInstant(): Instant {
    val kstDateTime = if (endsWith(" 24:00")) {
        LocalDateTime.parse("${removeSuffix(" 24:00")} 00:00", KST_FORMATTER).plusDays(1)
    } else {
        LocalDateTime.parse(this, KST_FORMATTER)
    }
    return kstDateTime.atZone(KST).toInstant()
}
