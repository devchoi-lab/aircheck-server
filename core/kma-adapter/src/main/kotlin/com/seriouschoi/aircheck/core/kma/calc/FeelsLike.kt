package com.seriouschoi.aircheck.core.kma.calc

/**
 * 체감온도. 기상청 기상자료개방포털의 산출식을 따른다.
 * https://data.kma.go.kr/climate/windChill/selectWindChillChart.do
 *
 * 계절은 월로 나눈다. 여름철은 5~9월, 겨울철은 10~익년 4월이다.
 * - 여름철: 기온과 습도로 계산한다. (습구온도는 Stull의 추정식) 기온이나 습도가 없으면 null
 * - 겨울철: 기온 10℃ 이하, 풍속 1.3m/s 이상일 때만 산출한다. 그 외에는 null이다.
 *   풍속이 없으면 산출 조건을 판단할 수 없어 null이다.
 *
 * 기상청 값은 관측값 기반의 일 최고(여름), 일 최저(겨울) 체감온도다. 시간별 값에 적용하는 것은 근사다.
 *
 * @param month 체감온도를 구할 시각의 월(1~12, KST)
 * @param windSpeedMs 풍속 (m/s). 공식에서는 km/h로 바꿔서 쓴다.
 */
internal fun calculateFeelsLike(month: Int, temp: Double?, windSpeedMs: Double?, humidity: Int?): Double? {
    if (temp == null) return null
    return if (month in 5..9) summerFeelsLike(temp, humidity) else winterFeelsLike(temp, windSpeedMs)
}

/**
 * 여름철 체감온도. 기온이나 습도가 없으면 null.
 * ```
 * 체감온도 = -0.2442 + 0.55399·Tw + 0.45535·Ta - 0.0022·Tw² + 0.00278·Tw·Ta + 3.0
 *   Ta: 기온(℃), Tw: 습구온도(℃, [wetBulbTemperature])
 * ```
 */
private fun summerFeelsLike(temp: Double, humidity: Int?): Double? {
    if (humidity == null) return null
    val tw = wetBulbTemperature(temp, humidity)
    return -0.2442 + 0.55399 * tw + 0.45535 * temp - 0.0022 * tw * tw + 0.00278 * tw * temp + 3.0
}

/**
 * 겨울철 체감온도. 기온 10℃ 이하, 풍속 1.3m/s 이상일 때만 산출하고, 그 외에는 null.
 * ```
 * 체감온도 = 13.12 + 0.6215·Ta - 11.37·V^0.16 + 0.3965·V^0.16·Ta
 *   Ta: 기온(℃), V: 풍속(km/h) = 풍속(m/s) × 3.6
 * ```
 */
private fun winterFeelsLike(temp: Double, windSpeedMs: Double?): Double? {
    if (temp > WINTER_MAX_TEMP) return null
    if (windSpeedMs == null || windSpeedMs < WINTER_MIN_WIND_MS) return null
    val windKmh = windSpeedMs * MS_TO_KMH
    val v016 = Math.pow(windKmh, 0.16)
    return 13.12 + 0.6215 * temp - 11.37 * v016 + 0.3965 * v016 * temp
}

/**
 * 습구온도(℃). Stull(2011)의 추정식이다.
 * ```
 * Tw = Ta·atan(0.151977·√(RH + 8.313659)) + atan(Ta + RH) - atan(RH - 1.676331)
 *      + 0.00391838·RH^1.5·atan(0.023101·RH) - 4.686035
 *   Ta: 기온(℃), RH: 상대습도(%)
 * ```
 * 원식의 상수는 1.676331이고, 기상청 안내에는 1.67633으로 적혀 있다.
 */
private fun wetBulbTemperature(temp: Double, humidity: Int): Double {
    val rh = humidity.toDouble()
    return temp * Math.atan(0.151977 * Math.sqrt(rh + 8.313659)) +
        Math.atan(temp + rh) - Math.atan(rh - 1.676331) +
        0.00391838 * Math.pow(rh, 1.5) * Math.atan(0.023101 * rh) - 4.686035
}

private const val WINTER_MAX_TEMP = 10.0
private const val WINTER_MIN_WIND_MS = 1.3
private const val MS_TO_KMH = 3.6
