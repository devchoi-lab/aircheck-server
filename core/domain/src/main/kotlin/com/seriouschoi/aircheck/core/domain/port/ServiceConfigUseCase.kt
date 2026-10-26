package com.seriouschoi.aircheck.core.domain.port

/**
 * 서비스 설정 관리 UseCase (인바운드 Port)
 *
 * 범용 get/set은 노출하지 않고, 의도가 담긴 메서드만 외부에 공개.
 */
interface ServiceConfigUseCase {
    fun isAppCheckEnabled(): Boolean
    fun setAppCheckEnabled(enabled: Boolean)
    fun isKmaParallelEnabled(): Boolean
    fun setKmaParallelEnabled(enabled: Boolean)
}
