package com.seriouschoi.aircheck.core.domain.port

import com.seriouschoi.aircheck.core.domain.model.ServiceVersion
import com.seriouschoi.aircheck.core.domain.model.VersionCheckResult

/**
 * 앱 버전 관리 UseCase (인바운드 Port)
 */
interface ServiceVersionUseCase {
    fun checkVersion(osType: String, currentVersionCode: Int): VersionCheckResult
    fun getServiceVersion(osType: String): ServiceVersion?
    fun updateServiceVersion(osType: String, minVersionCode: Int, updateUrl: String?): ServiceVersion
}
