package com.seriouschoi.aircheck.core.bjd

import com.seriouschoi.aircheck.core.domain.model.BjdLocation
import com.seriouschoi.aircheck.core.domain.port.BjdPort
import org.springframework.stereotype.Component

/**
 * BjdPort 구현체 — BjdMatcher를 사용해 위경도를 법정동 대표 좌표로 정규화
 */
@Component
class BjdMatcherAdapter : BjdPort {

    /**
     * 위경도 lat, lng를 받아서,
     * 법정동을 리턴한다.
     *
     * 가장 가까운 법정동이 [MAX_DISTANCE_KM]보다 멀면(한국 밖 좌표 등) 법정동을 찾지 못한 것으로 보고 null리턴.
     */
    override fun locate(lat: Double, lng: Double): BjdLocation? {
        val bjd = BjdMatcher.match(lat, lng, MAX_DISTANCE_KM) ?: return null
        val latLng = bjd.latLng ?: return null
        return BjdLocation(
            doCode = bjd.doGroup.code,
            sigunguCode = bjd.guGroup?.code ?: bjd.doGroup.code,
            lat = latLng.first,
            lng = latLng.second
        )
    }

    private companion object {
        /**
         * 이 거리보다 법정동이 멀면 서비스 지역 밖으로 본다.
         * 한국 안 법정동의 99.9%는 이웃 법정동이 18km 안에 있고, 대마도(약 86km)와 평양(약 145km)은 걸러진다.
         */
        const val MAX_DISTANCE_KM = 50.0
    }
}
