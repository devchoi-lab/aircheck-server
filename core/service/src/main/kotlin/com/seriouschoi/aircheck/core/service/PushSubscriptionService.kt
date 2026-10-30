package com.seriouschoi.aircheck.core.service

import com.seriouschoi.aircheck.core.domain.port.PushSubscriptionResult
import com.seriouschoi.aircheck.core.domain.port.PushSubscriptionUseCase
import com.seriouschoi.aircheck.core.domain.port.PushNotificationPort
import com.seriouschoi.aircheck.core.domain.port.PushSubscriptionData
import com.seriouschoi.aircheck.core.domain.port.PushSubscriptionPort
import com.seriouschoi.aircheck.core.domain.usecase.GetAirQualityUseCase
import com.seriouschoi.aircheck.core.domain.usecase.GetWeatherForecastUseCase
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalTime

@Service
class PushSubscriptionService(
    private val pushSubscriptionPort: PushSubscriptionPort,
    private val pushNotificationPort: PushNotificationPort,
    private val getWeatherForecastUseCase: GetWeatherForecastUseCase,
    private val getAirQualityUseCase: GetAirQualityUseCase
) : PushSubscriptionUseCase {
    
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun subscribe(
        fcmToken: String,
        latitude: Double,
        longitude: Double,
        address: String?,
        pushTime: LocalTime,
        enabled: Boolean
    ): PushSubscriptionResult {
        val existing = pushSubscriptionPort.findByFcmToken(fcmToken)
        
        val data = PushSubscriptionData(
            id = existing?.id ?: 0,
            fcmToken = fcmToken,
            latitude = latitude,
            longitude = longitude,
            address = address,
            pushTime = pushTime,
            enabled = enabled
        )
        
        val saved = pushSubscriptionPort.save(data)
        return saved.toResult()
    }

    @Transactional
    override fun unsubscribe(fcmToken: String): Boolean {
        val subscription = pushSubscriptionPort.findByFcmToken(fcmToken) ?: return false
        pushSubscriptionPort.delete(subscription)
        return true
    }

    @Transactional
    override fun setEnabled(fcmToken: String, enabled: Boolean): PushSubscriptionResult? {
        val subscription = pushSubscriptionPort.findByFcmToken(fcmToken) ?: return null
        val updated = pushSubscriptionPort.save(subscription.copy(enabled = enabled))
        return updated.toResult()
    }

    override fun sendScheduledPush(time: LocalTime) {
        val subscriptions = pushSubscriptionPort.findEnabledByPushTime(time)
        log.info("Sending scheduled push for $time to ${subscriptions.size} subscribers")

        subscriptions.forEach { sub ->
            try {
                val weather = getWeatherForecastUseCase.execute(sub.latitude, sub.longitude)
                val air = getAirQualityUseCase.execute(sub.latitude, sub.longitude)

                if (weather != null) {
                    val recommendation = buildRecommendation(weather.current.temperature, air?.pm25)
                    
                    val pm25Status = air?.pm25?.let { getPm25Status(it) } ?: "❓ 알 수 없음"
                    
                    pushNotificationPort.sendWeatherSummary(
                        token = sub.fcmToken,
                        temperature = weather.current.temperature,
                        weatherCondition = weather.current.weatherCondition.emoji + " " + weather.current.weatherCondition.label,
                        pm25Grade = pm25Status,
                        recommendation = recommendation
                    )
                }
            } catch (e: Exception) {
                log.error("Failed to send push to ${sub.id}: ${e.message}")
            }
        }
    }

    /**
     * PM2.5 밀도를 상태 문자열로 변환 (WHO 2021 기준)
     */
    private fun getPm25Status(pm25: Int): String = when {
        pm25 <= 15 -> "😊 좋음"
        pm25 <= 25 -> "🙂 보통"
        pm25 <= 37 -> "😐 민감군 주의"
        pm25 <= 50 -> "😷 나쁨"
        pm25 <= 75 -> "🤢 매우 나쁨"
        else -> "☠️ 위험"
    }

    private fun buildRecommendation(temp: Double, pm25: Int?): String {
        val clothes = when {
            temp >= 28 -> "민소매/반바지"
            temp >= 23 -> "반팔/얇은 옷"
            temp >= 17 -> "긴팔/가디건"
            temp >= 12 -> "자켓/니트"
            temp >= 6 -> "코트/히트텍"
            temp >= 0 -> "패딩"
            else -> "두꺼운 패딩"
        }

        val mask = if ((pm25 ?: 0) > 35) "마스크 챙기세요!" else ""
        return "👕 $clothes $mask".trim()
    }

    private fun PushSubscriptionData.toResult() = PushSubscriptionResult(
        id = id,
        fcmToken = fcmToken,
        pushTime = pushTime,
        enabled = enabled
    )
}
