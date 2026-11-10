package com.seriouschoi.aircheck.config

import com.seriouschoi.aircheck.core.domain.exception.DataNotFoundException
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.server.ResponseStatusException

@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(javaClass)

    data class ErrorResponse(
        val status: Int,
        val error: String,
        val message: String,
        val path: String
    )

    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingParam(
        e: MissingServletRequestParameterException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        log.warn("Missing parameter on ${request.method} ${request.requestURI}: ${e.message}")
        return ResponseEntity.badRequest().body(
            ErrorResponse(400, "Bad Request", e.message, request.requestURI)
        )
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(
        e: MethodArgumentTypeMismatchException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        log.warn("Type mismatch on ${request.method} ${request.requestURI}: param='${e.name}' value='${e.value}'")
        return ResponseEntity.badRequest().body(
            ErrorResponse(400, "Bad Request", "Invalid value for parameter '${e.name}'", request.requestURI)
        )
    }

    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatus(
        e: ResponseStatusException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val status = e.statusCode.value()
        val reasonPhrase = HttpStatus.resolve(status)?.reasonPhrase ?: "Error"
        // e.reason은 내부 정보가 담길 수 있으므로 로그에만 기록하고 응답에는 노출하지 않음
        log.warn("ResponseStatusException on ${request.method} ${request.requestURI}: $status ${e.reason}")
        return ResponseEntity.status(e.statusCode).body(
            ErrorResponse(status, reasonPhrase, reasonPhrase, request.requestURI)
        )
    }

    @ExceptionHandler(DataNotFoundException::class)
    fun handleDataNotFound(
        e: DataNotFoundException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        // e.message에는 좌표 등 내부 정보가 있으므로 로그에만 남기고 응답에는 고정 문구를 쓴다
        log.warn("${e.javaClass.simpleName} on ${request.method} ${request.requestURI}: ${e.message}")
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ErrorResponse(404, "Not Found", "요청한 데이터를 찾을 수 없습니다.", request.requestURI)
        )
    }

    @ExceptionHandler(Exception::class)
    fun handleException(
        e: Exception,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        log.error("Unhandled exception on ${request.method} ${request.requestURI}", e)
        return ResponseEntity.internalServerError().body(
            ErrorResponse(500, "Internal Server Error", "서버 오류가 발생했습니다.", request.requestURI)
        )
    }
}
