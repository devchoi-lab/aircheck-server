package com.seriouschoi.aircheck.core.kma.dto

import kotlinx.serialization.Serializable

/**
 * 기상청 API 공통 응답 구조
 */
@Serializable
data class KmaApiDto<T>(
    val response: KmaPayloadDto<T>
)

@Serializable
data class KmaPayloadDto<T>(
    val header: KmaHeaderDto,
    val body: KmaBodyDto<T>? = null
)

@Serializable
data class KmaHeaderDto(
    val resultCode: String,
    val resultMsg: String
)

@Serializable
data class KmaBodyDto<T>(
    val dataType: String? = null,
    val items: KmaItemsDto<T>? = null,
    val pageNo: Int? = null,
    val numOfRows: Int? = null,
    val totalCount: Int? = null
)

@Serializable
data class KmaItemsDto<T>(
    val item: List<T>
)
