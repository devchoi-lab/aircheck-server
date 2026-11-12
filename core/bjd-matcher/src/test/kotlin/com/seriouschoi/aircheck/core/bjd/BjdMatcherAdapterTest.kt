package com.seriouschoi.aircheck.core.bjd

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * 실제 법정동 CSV로 확인한다. 법정동 매칭은 외부 시스템이 아닌 순수 계산이라 mock을 쓰지 않는다.
 * 대마도, 평양처럼 경계에 있는 좌표는 계약에 넣지 않는다 (임계값 구현에 묶이기 때문).
 */
class BjdMatcherAdapterTest {

    private val adapter = BjdMatcherAdapter()

    @Test
    fun `한국 안 좌표는, 해당 지역의 법정동을 리턴한다`() {
        // 서울시청
        val location = adapter.locate(37.5665, 126.9780)

        assertNotNull(location, "서울시청 좌표에서 법정동을 찾지 못했습니다.")
        assertEquals(SEOUL_DO_CODE, location!!.doCode, "서울시청의 시도 코드가 서울이어야 합니다.")
    }

    @Test
    fun `한국에서 멀리 떨어진 좌표는, 법정동을 찾지 못하고 null을 리턴한다`() {
        val foreign = mapOf(
            "도쿄" to (35.68 to 139.69),
            "뉴욕" to (40.71 to -74.00),
            "(0,0)" to (0.0 to 0.0)
        )

        val found = foreign.filter { (_, p) -> adapter.locate(p.first, p.second) != null }.keys

        assertTrue(
            found.isEmpty(),
            "한국 밖 좌표인데 법정동을 찾았습니다: $found"
        )
    }

    private companion object {
        const val SEOUL_DO_CODE = 1100000000L
    }
}
