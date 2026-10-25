package com.seriouschoi.aircheck.core.kma.dto

import kotlinx.serialization.Serializable

/**
 * 초단기예보/단기예보 아이템
 */
@Serializable
data class FcstItem(
    val baseDate: String,      // 발표일자
    val baseTime: String,      // 발표시각
    val category: String,      // 자료구분코드
    val fcstDate: String,      // 예보일자
    val fcstTime: String,      // 예보시각
    val fcstValue: String,     // 예보값
    val nx: Int,               // 격자 X
    val ny: Int                // 격자 Y
)
