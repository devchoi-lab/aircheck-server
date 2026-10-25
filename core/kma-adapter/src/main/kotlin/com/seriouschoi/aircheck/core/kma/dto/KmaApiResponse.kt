package com.seriouschoi.aircheck.core.kma.dto

import kotlinx.serialization.Serializable

/**
 * 기상청 API 공통 응답 구조
 */
@Serializable
data class KmaApiResponse<T>(
    val response: KmaResponse<T>
)

@Serializable
data class KmaResponse<T>(
    val header: KmaHeader,
    val body: KmaBody<T>? = null
)

@Serializable
data class KmaHeader(
    val resultCode: String,
    val resultMsg: String
)

@Serializable
data class KmaBody<T>(
    val dataType: String? = null,
    val items: KmaItems<T>? = null,
    val pageNo: Int? = null,
    val numOfRows: Int? = null,
    val totalCount: Int? = null
)

@Serializable
data class KmaItems<T>(
    val item: List<T>
)
