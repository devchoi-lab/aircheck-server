package com.seriouschoi.aircheck.core.kma

import com.seriouschoi.aircheck.core.domain.model.MidLandForecast
import com.seriouschoi.aircheck.core.domain.model.MidLandForecastDay
import com.seriouschoi.aircheck.core.domain.model.MidLandWeather
import com.seriouschoi.aircheck.core.domain.model.MidLandWeatherType
import com.seriouschoi.aircheck.core.domain.port.ApiUsagePort
import com.seriouschoi.aircheck.core.kma.dto.KmaApiResponse
import com.seriouschoi.aircheck.core.kma.dto.MidLandFcstItem
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import java.time.LocalDate

/**
 * 중기육상예보 조회 캐시 빈.
 *
 * 캐시 키: doCode — 법정동 도 코드 기준이므로 lat/lng 정규화 없이도 캐시 효율이 높음.
 */
@Component
@ConditionalOnProperty(name = ["weather.provider"], havingValue = "kma")
class KmaMidLandForecastCache(
    @Value("\${kma.api.key:}") private val apiKey: String,
    private val kmaMidApi: KmaMidApi,
    private val apiUsagePort: ApiUsagePort?
) {
    @Cacheable("kma-mid-land-forecast", key = "#sigunguCode")
    fun fetch(doCode: Long, sigunguCode: Long): MidLandForecast {
        check(apiKey.isNotBlank()) { "KMA API key not configured" }
        val regId = doCodeToLandRegId(doCode, sigunguCode)
        val base = calculateMidFcstBase()
        return runWithUsage("KMA_MID_LAND_FCST", apiUsagePort) {
            val response = kmaMidApi.getMidLandFcst(
                serviceKey = apiKey, pageNo = 1, numOfRows = 10, dataType = "JSON",
                regId = regId, tmFc = base.toMidFcstTmFc()
            )
            check(response.isNotEmpty()) { "KMA mid-land fcst: empty response" }
            val parsed = forecastJson.decodeFromString<KmaApiResponse<MidLandFcstItem>>(response)
            check(parsed.response.header.resultCode == "00") {
                "KMA mid-land fcst error: ${parsed.response.header.resultMsg}"
            }
            val item = parsed.response.body?.items?.item?.firstOrNull()
                ?: error("KMA mid-land fcst: no data")
            parseMidLandForecast(item, base.toLocalDate())
        }
    }

    /**
     * 법정동 도/시군구 코드 → 중기육상예보 예보구역코드(landRegId)
     *
     * 강원특별자치도(5100000000)는 시군구 코드로 영동/영서를 구분.
     * 영동: 강릉·동해·태백·속초·삼척·고성·양양
     * 영서: 그 외 (춘천·원주·홍천·횡성·영월·평창·정선·철원·화천·양구·인제)
     */
    private fun doCodeToLandRegId(doCode: Long, sigunguCode: Long): String = when (doCode) {
        1100000000L -> "11B00000" // 서울특별시
        2800000000L -> "11B00000" // 인천광역시
        4100000000L -> "11B00000" // 경기도
        4300000000L -> "11C10000" // 충청북도
        3000000000L -> "11C20000" // 대전광역시
        3600000000L -> "11C20000" // 세종특별자치시
        4400000000L -> "11C20000" // 충청남도
        5100000000L -> gangwonLandRegId(sigunguCode)
        5200000000L -> "11F10000" // 전북특별자치도
        1200000000L -> "11F20000" // 전남광주통합특별시
        5000000000L -> "11G00000" // 제주특별자치도
        2700000000L -> "11H10000" // 대구광역시
        4700000000L -> "11H10000" // 경상북도
        2600000000L -> "11H20000" // 부산광역시
        3100000000L -> "11H20000" // 울산광역시
        4800000000L -> "11H20000" // 경상남도
        else         -> "11B00000" // fallback
    }

    private fun gangwonLandRegId(sigunguCode: Long): String {
        val yeongdong = setOf(
            5115000000L, // 강릉시
            5117000000L, // 동해시
            5119000000L, // 태백시
            5121000000L, // 속초시
            5123000000L, // 삼척시
            5182000000L, // 고성군
            5183000000L  // 양양군
        )
        return if (sigunguCode in yeongdong) "11D20000" else "11D10000"
    }
}

private val midLandLogger = KotlinLogging.logger {}

/**
 * 기상청 날씨 문구를 타입과 원문으로 만든다.
 * 값이 없거나 비어 있으면 null, 문서에 없는 문구이면 UNKNOWN(경고 로그에 원문을 남김). 원문은 가공하지 않는다.
 */
private fun String?.toMidLandWeather(): MidLandWeather? {
    if (isNullOrBlank()) return null
    val type = MidLandWeatherType.fromKmaText(this)
    if (type == MidLandWeatherType.UNKNOWN) midLandLogger.warn { "정의되지 않은 중기육상예보 날씨 문구: $this" }
    return MidLandWeather(type, this)
}

/**
 * 중기육상예보 항목에서 3~10일 예보를 만든다.
 * 값이 없으면 기본값(0, "")으로 채우지 않고 null로 둔다. 8~10일은 단일값을 오전/오후에 공통으로 쓴다.
 * 날씨는 기상청 문구 원문과 타입을 함께 담는다.
 */
internal fun parseMidLandForecast(item: MidLandFcstItem, baseDate: LocalDate): MidLandForecast {
    val days = (3..10).map { offset ->
        val date = baseDate.plusDays(offset.toLong())
        when (offset) {
            3  -> MidLandForecastDay(date, item.rnSt3Am, item.rnSt3Pm, item.wf3Am.toMidLandWeather(), item.wf3Pm.toMidLandWeather())
            4  -> MidLandForecastDay(date, item.rnSt4Am, item.rnSt4Pm, item.wf4Am.toMidLandWeather(), item.wf4Pm.toMidLandWeather())
            5  -> MidLandForecastDay(date, item.rnSt5Am, item.rnSt5Pm, item.wf5Am.toMidLandWeather(), item.wf5Pm.toMidLandWeather())
            6  -> MidLandForecastDay(date, item.rnSt6Am, item.rnSt6Pm, item.wf6Am.toMidLandWeather(), item.wf6Pm.toMidLandWeather())
            7  -> MidLandForecastDay(date, item.rnSt7Am, item.rnSt7Pm, item.wf7Am.toMidLandWeather(), item.wf7Pm.toMidLandWeather())
            8  -> item.wf8.toMidLandWeather().let { MidLandForecastDay(date, item.rnSt8, item.rnSt8, it, it) }
            9  -> item.wf9.toMidLandWeather().let { MidLandForecastDay(date, item.rnSt9, item.rnSt9, it, it) }
            10 -> item.wf10.toMidLandWeather().let { MidLandForecastDay(date, item.rnSt10, item.rnSt10, it, it) }
            else -> MidLandForecastDay(date, null, null, null, null)
        }
    }
    return MidLandForecast(days)
}
