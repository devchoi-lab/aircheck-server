package com.seriouschoi.aircheck.feature.weather

import com.seriouschoi.aircheck.core.domain.port.ServiceVersionUseCase
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/app")
class AppVersionController(
    private val serviceVersionUseCase: ServiceVersionUseCase
) {

    /**
     * 앱 버전 체크 API
     *
     * 강제 업데이트 여부를 확인합니다.
     * currentVersion < minVersion 이면 needUpdate = true
     *
     * @param osType OS 타입 (android, ios)
     * @param versionCode 현재 앱 버전 코드
     * @return 업데이트 필요 여부 및 스토어 URL
     */
    @GetMapping("/version")
    fun checkVersion(
        @RequestParam osType: String,
        @RequestParam versionCode: Int
    ): ResponseEntity<VersionCheckResponse> {
        val result = serviceVersionUseCase.checkVersion(osType, versionCode)
        return ResponseEntity.ok(
            VersionCheckResponse(
                needUpdate = result.needUpdate,
                updateUrl = result.updateUrl
            )
        )
    }
}

data class VersionCheckResponse(
    val needUpdate: Boolean,
    val updateUrl: String?
)
