package com.seriouschoi.aircheck.core.domain.exception

/**
 * 요청한 데이터를 찾지 못했을 때의 공통 상위 예외.
 *
 * 서버 오류가 아니라 정상적으로 처리한 결과이다. 이 타입을 상속하면 HTTP 404로 응답된다
 * (app 계층의 GlobalExceptionHandler). 도메인은 HTTP를 모르므로 상태 코드는 여기서 정하지 않는다.
 */
abstract class DataNotFoundException(message: String) : RuntimeException(message)
