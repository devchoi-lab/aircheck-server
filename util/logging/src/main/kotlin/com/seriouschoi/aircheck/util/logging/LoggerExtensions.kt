package com.seriouschoi.aircheck.util.logging

import org.slf4j.Logger
import java.util.UUID

/**
 * 에러 로그를 남기고, 로그 추적용 errorId(UUID)를 반환한다.
 * 반환된 errorId를 API 응답에 포함하면 클라이언트가 서버 로그를 grep으로 추적할 수 있다.
 */
fun Logger.errorWithId(message: String, cause: Throwable? = null): String {
    val errorId = UUID.randomUUID().toString()
    if (cause != null) {
        error("$message [errorId=$errorId]", cause)
    } else {
        error("$message [errorId=$errorId]")
    }
    return errorId
}
