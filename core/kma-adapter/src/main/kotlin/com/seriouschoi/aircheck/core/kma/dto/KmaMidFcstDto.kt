package com.seriouschoi.aircheck.core.kma.dto

import kotlinx.serialization.Serializable

/**
 * 중기예보 아이템 (육상)
 */
@Serializable
data class MidLandFcstItem(
    val regId: String,         // 예보구역코드
    val rnSt3Am: Int? = null,  // 3일 후 오전 강수확률
    val rnSt3Pm: Int? = null,  // 3일 후 오후 강수확률
    val rnSt4Am: Int? = null,
    val rnSt4Pm: Int? = null,
    val rnSt5Am: Int? = null,
    val rnSt5Pm: Int? = null,
    val rnSt6Am: Int? = null,
    val rnSt6Pm: Int? = null,
    val rnSt7Am: Int? = null,
    val rnSt7Pm: Int? = null,
    val rnSt8: Int? = null,
    val rnSt9: Int? = null,
    val rnSt10: Int? = null,
    val wf3Am: String? = null, // 3일 후 오전 날씨
    val wf3Pm: String? = null,
    val wf4Am: String? = null,
    val wf4Pm: String? = null,
    val wf5Am: String? = null,
    val wf5Pm: String? = null,
    val wf6Am: String? = null,
    val wf6Pm: String? = null,
    val wf7Am: String? = null,
    val wf7Pm: String? = null,
    val wf8: String? = null,
    val wf9: String? = null,
    val wf10: String? = null
)

/**
 * 중기기온예보 아이템
 */
@Serializable
data class MidTaItem(
    val regId: String,         // 예보구역코드
    val taMin3: Int? = null,   // 3일 후 최저기온
    val taMax3: Int? = null,   // 3일 후 최고기온
    val taMin4: Int? = null,
    val taMax4: Int? = null,
    val taMin5: Int? = null,
    val taMax5: Int? = null,
    val taMin6: Int? = null,
    val taMax6: Int? = null,
    val taMin7: Int? = null,
    val taMax7: Int? = null,
    val taMin8: Int? = null,
    val taMax8: Int? = null,
    val taMin9: Int? = null,
    val taMax9: Int? = null,
    val taMin10: Int? = null,
    val taMax10: Int? = null
)
