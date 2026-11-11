package com.seriouschoi.aircheck.core.domain.model

import com.seriouschoi.aircheck.util.logging.errorWithId
import org.slf4j.Logger

/**
 * 실패한 [Result]를 [DataError]로 바꾼다. 성공이면 null.
 *
 * 실패 원인은 [log]에 에러 로그로 남기고, 그 로그를 찾을 수 있는 errorId를 [DataError]에 담는다.
 */
internal fun Result<*>.toDataError(target: DataErrorTarget, log: Logger, message: String): DataError? =
    exceptionOrNull()?.let {
        DataError(
            target = target,
            errorCode = DataErrorCode.API_UNAVAILABLE,
            errorId = log.errorWithId(message, it)
        )
    }
