package com.seriouschoi.aircheck.integration

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 기상청/에어코리아를 대신하는 가짜 HTTP 서버.
 *
 * 어댑터의 base URL 설정만 이 서버 주소로 바꾸고, 어댑터 코드는 진짜를 그대로 쓴다.
 * 서버 기동 시점에 측정소 API를 부르므로(CacheRefreshScheduler), 스프링 컨텍스트보다 먼저 떠 있어야 한다.
 */
object FakeExternalApis {

    val server: WireMockServer = WireMockServer(options().dynamicPort()).also { it.start() }

    val kmaShortUrl get() = "${server.baseUrl()}/kma/short"
    val kmaMidUrl get() = "${server.baseUrl()}/kma/mid"
    val airKoreaUrl get() = "${server.baseUrl()}/airkorea/air"
    val stationUrl get() = "${server.baseUrl()}/airkorea/station"

    private val kst = ZoneId.of("Asia/Seoul")

    /** 측정소 목록. 서버 기동 시 한 번 호출된다. */
    fun stubStations() = stubJson("/airkorea/station/getMsrstnList", FakeResponses.stations())

    /** 모든 외부 API가 정상 응답하는 상태. */
    fun stubAllSuccess() {
        val now = LocalDateTime.now(kst)
        stubStations()
        stubJson("/kma/short/getUltraSrtNcst", FakeResponses.ultraSrtNcst(now))
        stubJson("/kma/short/getVilageFcst", FakeResponses.vilageFcst(now))
        stubJson("/kma/mid/getMidLandFcst", FakeResponses.midLand())
        stubJson("/kma/mid/getMidTa", FakeResponses.midTa())
        stubAirQualitySuccess()
    }

    /**
     * 대기질 조회가 정상 응답하는 상태. 실패 뒤 복구를 만들 때도 쓴다.
     * 측정소 이름(stationName 파라미터)별로 다른 값을 돌려준다.
     */
    fun stubAirQualitySuccess() {
        val now = LocalDateTime.now(kst)
        FakeResponses.PM10_BY_STATION.forEach { (stationName, pm10) ->
            server.stubFor(
                get(urlPathEqualTo("/airkorea/air/getMsrstnAcctoRltmMesureDnsty"))
                    .withQueryParam("stationName", equalTo(stationName))
                    .willReturn(
                        aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                            .withBody(FakeResponses.airQuality(now, stationName, pm10))
                    )
            )
        }
    }

    /** 대기질 조회만 서버 오류(500)로 응답하는 상태. 나머지는 정상. */
    fun stubAirQualityFailure() {
        server.stubFor(
            get(urlPathEqualTo("/airkorea/air/getMsrstnAcctoRltmMesureDnsty"))
                .willReturn(aResponse().withStatus(500))
        )
    }

    /** 가짜 서버가 해당 경로로 받은 요청 수. */
    fun requestCount(path: String): Int =
        server.findAll(getRequestedFor(urlPathEqualTo(path))).size

    fun reset() = server.resetAll()

    private fun stubJson(path: String, body: String) {
        server.stubFor(
            get(urlPathEqualTo(path)).willReturn(
                aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(body)
            )
        )
    }
}

/**
 * 외부 API 응답 형식을 따라 만든 가짜 응답.
 *
 * 형식은 어댑터 DTO(kma/dto, airkorea/dto)와 KDoc을 기준으로 만들었다. 실제 응답을 저장한 것이 아니다.
 * 시각은 호출 시점 기준으로 만든다 (고정 날짜면 "지난 예보"로 걸러질 수 있다).
 */
object FakeResponses {

    /** 테스트가 기대값으로 쓰는 값. 응답 생성과 테스트가 같은 상수를 공유한다. */
    const val CURRENT_TEMPERATURE = 20.7
    const val SEOUL_STATION = "중구"
    const val BUSAN_STATION = "광복동"

    /** 측정소별 PM10. 서로 달라서, 어느 측정소가 뽑혔는지 값으로도 구별된다. */
    val PM10_BY_STATION = mapOf(SEOUL_STATION to 45, BUSAN_STATION to 80)
    val PM10 = PM10_BY_STATION.getValue(SEOUL_STATION)

    private val dateFmt = DateTimeFormatter.ofPattern("yyyyMMdd")
    private val timeFmt = DateTimeFormatter.ofPattern("HHmm")

    /** 기상청 공통 응답 껍데기. items에는 항목 JSON들을 쉼표로 이어서 넣는다. */
    private fun kmaEnvelope(items: String, total: Int) = """
        {
          "response": {
            "header": { "resultCode": "00", "resultMsg": "NORMAL_SERVICE" },
            "body": {
              "dataType": "JSON",
              "items": { "item": [$items] },
              "pageNo": 1,
              "numOfRows": $total,
              "totalCount": $total
            }
          }
        }
    """.trimIndent()

    fun ultraSrtNcst(now: LocalDateTime): String {
        val values = listOf(
            "T1H" to "$CURRENT_TEMPERATURE",
            "RN1" to "0",
            "UUU" to "0.5",
            "VVV" to "-1.7",
            "REH" to "49",
            "PTY" to "0",
            "VEC" to "158",
            "WSD" to "1.8"
        )
        val items = values.joinToString(",") { (category, value) ->
            """
            {
              "baseDate": "${now.format(dateFmt)}",
              "baseTime": "${now.format(timeFmt)}",
              "category": "$category",
              "obsrValue": "$value",
              "nx": 60,
              "ny": 127
            }
            """.trimIndent()
        }
        return kmaEnvelope(items, values.size)
    }

    /** 현재 시각 다음 정시부터 48시간. 날짜마다 최저/최고기온(TMN/TMX) 포함. */
    fun vilageFcst(now: LocalDateTime): String {
        val start = now.withMinute(0).withSecond(0).withNano(0).plusHours(1)
        val items = mutableListOf<String>()
        fun item(at: LocalDateTime, category: String, value: String) = items.add(
            """
            {
              "baseDate": "${now.format(dateFmt)}",
              "baseTime": "0500",
              "category": "$category",
              "fcstDate": "${at.format(dateFmt)}",
              "fcstTime": "${at.format(timeFmt)}",
              "fcstValue": "$value",
              "nx": 60,
              "ny": 127
            }
            """.trimIndent()
        )
        (0 until 48).forEach { h ->
            val at = start.plusHours(h.toLong())
            item(at, "TMP", "20")
            item(at, "REH", "50")
            item(at, "WSD", "2.0")
            item(at, "SKY", "1")
            item(at, "PTY", "0")
            item(at, "POP", "10")
            item(at, "PCP", "강수없음")
            item(at, "SNO", "적설없음")
        }
        (0..2).forEach { d ->
            val day = now.toLocalDate().plusDays(d.toLong())
            item(day.atTime(6, 0), "TMN", "12.0")
            item(day.atTime(15, 0), "TMX", "24.0")
        }
        return kmaEnvelope(items.joinToString(","), items.size)
    }

    fun midLand(): String {
        val days3to7 = (3..7).joinToString(",") {
            """
            "rnSt${it}Am": 20,
            "rnSt${it}Pm": 30,
            "wf${it}Am": "맑음",
            "wf${it}Pm": "구름많음"
            """.trimIndent()
        }
        val days8to10 = (8..10).joinToString(",") {
            """
            "rnSt$it": 40,
            "wf$it": "흐림"
            """.trimIndent()
        }
        return kmaEnvelope("""{ "regId": "11B00000", $days3to7, $days8to10 }""", 1)
    }

    fun midTa(): String {
        val days = (3..10).joinToString(",") {
            """
            "taMin$it": 10,
            "taMax$it": 20
            """.trimIndent()
        }
        return kmaEnvelope("""{ "regId": "11B10101", $days }""", 1)
    }

    /**
     * 서로 먼 측정소 둘: 서울 중구(시청 근처), 부산 중구(광복동).
     * ver=1.1 기준이라 dmX=경도, dmY=위도다. 한 곳뿐이면 좌표를 뒤바꿔 읽어도 그 곳이 뽑혀서 해석 오류를 못 잡는다. (#151)
     */
    fun stations(): String = """
        {
          "response": {
            "header": { "resultCode": "00", "resultMsg": "NORMAL_SERVICE" },
            "body": {
              "items": [
                {
                  "stationName": "$SEOUL_STATION",
                  "addr": "서울 중구 덕수궁길 15",
                  "dmX": "126.975961",
                  "dmY": "37.564639"
                },
                {
                  "stationName": "$BUSAN_STATION",
                  "addr": "부산 중구 중앙대로 1",
                  "dmX": "129.032000",
                  "dmY": "35.098000"
                }
              ]
            }
          }
        }
    """.trimIndent()

    fun airQuality(now: LocalDateTime, stationName: String, pm10: Int): String {
        val dataTime = now.withMinute(0).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        return """
            {
              "response": {
                "header": { "resultCode": "00", "resultMsg": "NORMAL_SERVICE" },
                "body": {
                  "items": [
                    {
                      "stationName": "$stationName",
                      "dataTime": "$dataTime",
                      "mangName": "도시대기",
                      "pm10Value": "$pm10", "pm10Grade": "2", "pm10Grade1h": "2",
                      "pm25Value": "22", "pm25Grade": "2", "pm25Grade1h": "2",
                      "khaiValue": "70", "khaiGrade": "2",
                      "so2Value": "0.003", "so2Grade": "1",
                      "coValue": "0.5", "coGrade": "1",
                      "o3Value": "0.030", "o3Grade": "2",
                      "no2Value": "0.020", "no2Grade": "1"
                    }
                  ]
                }
              }
            }
        """.trimIndent()
    }
}
