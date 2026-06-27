package com.sensable.app.core.braille

/**
 * 점 번호는 표준 점자 기준 (읽기 방향):
 *
 * [1] [4]
 * [2] [5]
 * [3] [6]
 *
 * UI 버튼은 쓰기 방향([4][1]/[5][2]/[6][3])으로 배치되어 있으나,
 * 버튼을 누르면 표준 점 번호(1~6)가 그대로 전달되므로 디코더는 표준 번호로 처리.
 */

object BrailleDecoder {

    // 수표 (숫자 모드 prefix): 표준 점자 기준 점 {3,4,5,6}
    val NUMBER_PREFIX: Set<Int> = setOf(3, 4, 5, 6)

    /**
     * 숫자 디코딩 테이블 (표준 점자 기준).
     */
    private val numberTable: Map<Set<Int>, Char> = mapOf(
        setOf(1) to '1',
        setOf(1, 2) to '2',
        setOf(1, 4) to '3',
        setOf(1, 4, 5) to '4',
        setOf(1, 5) to '5',
        setOf(1, 2, 4) to '6',
        setOf(1, 2, 4, 5) to '7',
        setOf(1, 2, 5) to '8',
        setOf(2, 4) to '9',
        setOf(2, 4, 5) to '0',
    )

    /**
     * 한글 초성 디코딩 테이블 (표준 점자 기준).
     *
     * [해결됨] ㄷ {2,4} — 중성 ㅡ {2,4}와 점형 충돌
     *   → KoreanBrailleStateMachine이 EXPECT_INITIAL 상태일 때만 이 테이블을 조회하므로 해결.
     *
     * [해결됨] ㅎ {2,4,5} — 중성 ㅚ {2,4,5}와 점형 충돌
     *   → 마찬가지로 State Machine이 상태(초성/중성)에 따라 테이블을 분리 조회하므로 해결.
     *
     * [미해결] ㅅ {6} — 된소리표와 점형 동일
     *   현재는 기본 매핑을 ㅅ으로 두지만, 된소리(ㄲ, ㄸ, ㅃ, ㅆ, ㅉ)는 된소리표 셀 + 기본 자음 셀의
     *   2-셀 조합이라 State Machine에 된소리 prefix 상태를 별도로 추가해야 처리 가능.
     *
     * [참고] ㅇ — 초성 점자 없음 (의도적 생략)
     *   모음 셀이 초성 없이 오면 State Machine이 ㅇ 초성으로 자동 처리함.
     */
    val initialConsonantTable: Map<Set<Int>, String> = mapOf(
        setOf(4) to "ㄱ",
        setOf(1, 4) to "ㄴ",
        setOf(2, 4) to "ㄷ",       // [해결됨] 중성 ㅡ {2,4}와 점형 충돌 → State Machine으로 해결
        setOf(5) to "ㄹ",
        setOf(1, 5) to "ㅁ",
        setOf(4, 5) to "ㅂ",
        setOf(6) to "ㅅ",           // [미해결] 된소리표와 점형 동일 → 된소리 prefix 로직 미구현
        setOf(4, 6) to "ㅈ",
        setOf(5, 6) to "ㅊ",
        setOf(1, 2, 4) to "ㅋ",
        setOf(1, 2, 5) to "ㅌ",
        setOf(1, 4, 5) to "ㅍ",
        setOf(2, 4, 5) to "ㅎ"     // [해결됨] 중성 ㅚ {2,4,5}와 점형 충돌 → State Machine으로 해결
    )

    /**
     * 한글 중성(모음) 디코딩 테이블 (표준 점자 기준).
     *
     * [해결됨] ㅡ {2,4} — 초성 ㄷ {2,4}와 점형 충돌
     *   → KoreanBrailleStateMachine이 EXPECT_VOWEL 상태일 때만 이 테이블을 조회하므로 해결.
     *
     * [해결됨] ㅚ {2,4,5} — 초성 ㅎ {2,4,5}와 점형 충돌
     *   → 마찬가지로 State Machine이 상태에 따라 테이블을 분리 조회하므로 해결.
     *
     * [해결됨] ㅟ/ㅒ/ㅙ/ㅞ — 2셀 복합 모음
     *   이 테이블에는 없고 KoreanBrailleStateMachine의 COMPOUND_VOWEL_RESULT로 처리됨.
     *   모두 (첫 번째 셀) + ㅐ{1,2,3,5} 조합: ㅜ→ㅟ, ㅑ→ㅒ, ㅘ→ㅙ, ㅝ→ㅞ.
     */
    val vowelTable: Map<Set<Int>, String> = mapOf(
        setOf(1, 2, 6) to "ㅏ",
        setOf(3, 4, 5) to "ㅑ",
        setOf(2, 3, 4) to "ㅓ",
        setOf(1, 5, 6) to "ㅕ",
        setOf(1, 3, 6) to "ㅗ",
        setOf(3, 4, 6) to "ㅛ",
        setOf(1, 3, 4) to "ㅜ",
        setOf(1, 4, 6) to "ㅠ",
        setOf(2, 4, 6) to "ㅡ",
        setOf(1, 3, 5) to "ㅣ",
        setOf(1, 2, 3, 5) to "ㅐ",
        setOf(1, 3, 4, 5) to "ㅔ",
        setOf(1, 3, 4, 5, 6) to "ㅚ",
        setOf(1, 2, 3, 6) to "ㅘ",
        setOf(1, 2, 3, 4) to "ㅝ",
        setOf(2, 4, 5, 6) to "ㅢ",
        setOf(3, 4) to "ㅖ",
        // 2셀 복합 모음(ㅟ/ㅒ/ㅙ/ㅞ)은 KoreanBrailleStateMachine.COMPOUND_VOWEL_RESULT로 처리
    )

    /**
     * 한글 종성(받침) 디코딩 테이블 (표준 점자 기준).
     *
     * [해결됨] 초성과 종성은 표준 점자에서 의도적으로 다른 점형을 사용함
     *   → KoreanBrailleStateMachine이 EXPECT_FINAL_OR_NEXT_INITIAL 상태에서 finalConsonantTable을
     *     먼저 조회하고, 매칭 실패 시 initialConsonantTable을 조회하는 순서로 초성/종성을 올바르게 구분.
     *
     * [미해결] 겹받침 (ㄳ, ㄵ, ㄺ, ㄻ, ㄼ, ㄽ, ㄾ, ㄿ, ㅀ, ㅄ) 미포함
     *   겹받침은 추가 점형 정의 후 이 테이블에 항목 추가만 하면 State Machine 수정 없이 동작 가능.
     */
    val finalConsonantTable: Map<Set<Int>, String> = mapOf(
        setOf(1) to "ㄱ",
        setOf(2, 5) to "ㄴ",
        setOf(3, 5) to "ㄷ",
        setOf(2) to "ㄹ",
        setOf(2, 6) to "ㅁ",
        setOf(1, 2) to "ㅂ",
        setOf(3) to "ㅅ",
        setOf(2, 3, 5, 6) to "ㅇ",
        setOf(1, 3) to "ㅈ",
        setOf(2, 3) to "ㅊ",
        setOf(2, 3, 5) to "ㅋ",
        setOf(2, 3, 6) to "ㅌ",
        setOf(2, 4, 6) to "ㅍ",
        setOf(3, 5, 6) to "ㅎ"
        // [미해결] 겹받침 미포함 — 점형 확인 후 이 테이블에 추가하면 State Machine 수정 없이 동작
    )

    fun isNumberPrefix(dots: Set<Int>): Boolean = dots == NUMBER_PREFIX

    fun decodeNumber(dots: Set<Int>): Char? = numberTable[dots]
}