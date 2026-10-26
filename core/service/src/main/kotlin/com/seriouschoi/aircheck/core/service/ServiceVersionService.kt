package com.seriouschoi.aircheck.core.service

import com.seriouschoi.aircheck.core.domain.model.ServiceVersion
import com.seriouschoi.aircheck.core.domain.port.VersionCheckResult
import com.seriouschoi.aircheck.core.domain.port.ServiceVersionPort
import com.seriouschoi.aircheck.core.domain.port.ServiceVersionUseCase
import org.springframework.stereotype.Service

@Service
class ServiceVersionService(
    private val serviceVersionPort: ServiceVersionPort
) : ServiceVersionUseCase {

    override fun checkVersion(osType: String, currentVersionCode: Int): VersionCheckResult {
        val serviceVersion = serviceVersionPort.getServiceVersion(osType)
            ?: return VersionCheckResult(needUpdate = false, updateUrl = null)

        return VersionCheckResult(
            needUpdate = serviceVersion.needsUpdate(currentVersionCode),
            updateUrl = serviceVersion.updateUrl
        )
    }

    override fun getServiceVersion(osType: String): ServiceVersion? {
        return serviceVersionPort.getServiceVersion(osType)
    }

    override fun updateServiceVersion(osType: String, minVersionCode: Int, updateUrl: String?): ServiceVersion {
        val serviceVersion = ServiceVersion(
            osType = osType.uppercase(),
            minVersionCode = minVersionCode,
            updateUrl = updateUrl
        )
        return serviceVersionPort.saveServiceVersion(serviceVersion)
    }
}
