package com.seriouschoi.aircheck.core.kma.calc

import com.seriouschoi.aircheck.core.kma.KST_ZONE
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** 초단기실황 base_date/base_time 계산 ([kstNow]는 KST 기준 현재 시각. 테스트에서 시각을 고정하려고 인자로 받는다) */
internal fun calculateBaseDateTime(kstNow: LocalDateTime = LocalDateTime.now(KST_ZONE)): Pair<String, String> {
    val base = if (kstNow.minute < 40) kstNow.minusHours(1) else kstNow
    return base.format(DateTimeFormatter.ofPattern("yyyyMMdd")) to
            base.format(DateTimeFormatter.ofPattern("HH00"))
}

/** 단기예보 base_date/base_time 계산 (발표시각: 02·05·08·11·14·17·20·23시) */
internal fun calculateVilageFcstBaseDateTime(kstNow: LocalDateTime = LocalDateTime.now(KST_ZONE)): Pair<String, String> {
    val baseTimes = listOf(2, 5, 8, 11, 14, 17, 20, 23)
    val baseHour = baseTimes.lastOrNull { it <= kstNow.hour - 1 } ?: 23
    val base = if (baseHour == 23 && kstNow.hour < 23) kstNow.minusDays(1).withHour(23) else kstNow.withHour(baseHour)
    return base.format(DateTimeFormatter.ofPattern("yyyyMMdd")) to
            String.format("%02d00", baseHour)
}

/**
 * 중기예보 발표시각 계산 (KST, 06시·18시).
 * 중기예보의 "N일 후"는 이 발표일(날짜) 기준이다.
 */
internal fun calculateMidFcstBase(kstNow: LocalDateTime = LocalDateTime.now(KST_ZONE)): LocalDateTime {
    return when {
        kstNow.hour < 6  -> kstNow.minusDays(1).withHour(18)
        kstNow.hour < 18 -> kstNow.withHour(6)
        else           -> kstNow.withHour(18)
    }
}

/** 중기예보 API의 tmFc 파라미터 (yyyyMMddHH00) */
internal fun LocalDateTime.toMidFcstTmFc(): String =
    format(DateTimeFormatter.ofPattern("yyyyMMddHH00"))
