package com.seriouschoi.aircheck.core.kma

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 기상청 격자 변환 테스트.
 *
 * 기대값은 기상청 단기예보 조회서비스 오픈API 활용가이드의 "격자_위경도(2610)" 문서에 적힌 값이다.
 * 문서의 위경도는 행정구역의 대표점이라 격자 경계에 걸리면 문서의 격자와 한 칸 어긋나는 행이 있다. (전체 3,839행 중 45행)
 * 그래서 경계에서 멀어 일치하는 행만 골라 쓴다. 이어도처럼 좌표가 (0, 0)인 행은 쓰지 않는다.
 */
class KmaGridConverterTest {

    @Test
    fun `기상청 문서의 행정구역 위경도는, 문서에 적힌 격자 좌표로 변환된다`() {
        // 이름 → (위도, 경도, 문서의 nx, ny)
        val documented = mapOf(
            "서울특별시" to Documented(37.5635694, 126.9800083, 60, 127),
            "서울 종로구" to Documented(37.5703778, 126.9816417, 60, 127),
            "서울 강남구" to Documented(37.514575, 127.0495556, 61, 126),
            "부산광역시" to Documented(35.1770194, 129.0769528, 98, 76),
            "부산 해운대구" to Documented(35.1600194, 129.1658083, 99, 75),
            "제주특별자치도" to Documented(33.4856944, 126.5003333, 52, 38),
            "제주시" to Documented(33.4963111, 126.5332083, 53, 38)
        )

        val mismatches = documented.mapNotNull { (name, d) ->
            val grid = KmaGridConverter.toGrid(d.lat, d.lng)
            if (grid == KmaGridConverter.Grid(d.nx, d.ny)) null
            else "$name: 문서 (${d.nx}, ${d.ny}), 변환 (${grid.nx}, ${grid.ny})"
        }

        assertTrue(mismatches.isEmpty(), "문서와 다른 격자가 나왔습니다: $mismatches")
    }

    @Test
    fun `격자 좌표를 위경도로 바꿨다가 다시 격자로 바꾸면, 원래 격자 좌표가 나온다`() {
        val grids = listOf(
            60 to 127, // 서울
            98 to 76,  // 부산
            52 to 38,  // 제주
            55 to 124, // 인천
            89 to 90,  // 대구
            73 to 134  // 강원
        )

        val mismatches = grids.mapNotNull { (nx, ny) ->
            val (lat, lng) = KmaGridConverter.toLatLng(nx, ny)
            val restored = KmaGridConverter.toGrid(lat, lng)
            if (restored == KmaGridConverter.Grid(nx, ny)) null
            else "($nx, $ny) → ($lat, $lng) → (${restored.nx}, ${restored.ny})"
        }

        assertTrue(mismatches.isEmpty(), "원래 격자로 돌아오지 않았습니다: $mismatches")
    }

    private data class Documented(val lat: Double, val lng: Double, val nx: Int, val ny: Int)
}
