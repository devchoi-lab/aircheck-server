package com.seriouschoi.aircheck.core.domain.port

import com.seriouschoi.aircheck.core.domain.model.BjdLocation

/**
 * 법정동(法定洞) 위치 정규화 Port (아웃바운드)
 *
 * 입력 위경도를 가장 가까운 법정동 대표 좌표로 정규화.
 * 구현체: BjdMatcherAdapter
 */
interface BjdPort {
    fun locate(lat: Double, lng: Double): BjdLocation?
}
