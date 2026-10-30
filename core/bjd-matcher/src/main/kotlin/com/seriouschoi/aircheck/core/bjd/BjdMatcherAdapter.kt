package com.seriouschoi.aircheck.core.bjd

import com.seriouschoi.aircheck.core.domain.model.BjdLocation
import com.seriouschoi.aircheck.core.domain.port.BjdPort
import org.springframework.stereotype.Component

/**
 * BjdPort 구현체 — BjdMatcher를 사용해 위경도를 법정동 대표 좌표로 정규화
 */
@Component
class BjdMatcherAdapter : BjdPort {

    override fun locate(lat: Double, lng: Double): BjdLocation? {
        val bjd = BjdMatcher.match(lat, lng) ?: return null
        val latLng = bjd.latLng ?: return null
        return BjdLocation(
            doCode = bjd.doGroup.code,
            lat = latLng.first,
            lng = latLng.second
        )
    }
}
