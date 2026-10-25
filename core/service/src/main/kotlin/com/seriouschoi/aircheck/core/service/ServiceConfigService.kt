package com.seriouschoi.aircheck.core.service

import com.seriouschoi.aircheck.core.domain.port.ServiceConfigPort
import com.seriouschoi.aircheck.core.domain.port.ServiceConfigUseCase
import org.springframework.stereotype.Service

@Service
class ServiceConfigService(
    private val configPort: ServiceConfigPort
) : ServiceConfigUseCase {
    companion object {
        const val KEY_APPCHECK_ENABLED = "appcheck_enabled"
        const val KEY_KMA_PARALLEL_ENABLED = "kma_parallel_enabled"
    }

    private fun get(key: String): String? {
        return configPort.get(key)
    }

    private fun getBoolean(key: String, default: Boolean = false): Boolean {
        return get(key)?.toBooleanStrictOrNull() ?: default
    }

    private fun set(key: String, value: String) {
        configPort.set(key, value)
    }

    override fun isAppCheckEnabled(): Boolean {
        return getBoolean(KEY_APPCHECK_ENABLED, default = true)
    }

    override fun setAppCheckEnabled(enabled: Boolean) {
        set(KEY_APPCHECK_ENABLED, enabled.toString())
    }

    override fun isKmaParallelEnabled(): Boolean {
        return getBoolean(KEY_KMA_PARALLEL_ENABLED, default = true)
    }

    override fun setKmaParallelEnabled(enabled: Boolean) {
        set(KEY_KMA_PARALLEL_ENABLED, enabled.toString())
    }
}
