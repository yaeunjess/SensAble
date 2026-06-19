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
     * [참고] ㄴ/ㄷ/ㅁ/ㅂ/ㅈ/ㅋ/ㅌ/ㅍ/ㅎ — 약자 테이블과 점형 공유
     *   decodeKoreanCell()은 약자를 먼저 조회하지만, State Machine은 initialConsonantTable을
     *   직접 조회하므로 초성으로 올바르게 처리됨. 약자 지원이 필요하면 State Machine에
     *   별도 약자 감지 상태 추가 필요.
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
     * [미해결] ㅒ, ㅙ, ㅞ 누락
     *   표준 한국 점자에서 이 세 모음은 2셀 복합으로 표현됨 (ㅒ = ㅑ 셀 + 별도 셀 등).
     *   단순 Map 조회로는 처리 불가. State Machine에 복합 모음 sub-state 추가 필요.
     *
     * [확인 필요] ㅘ, ㅝ, ㅟ, ㅢ
     *   현재 단일 셀로 매핑되어 있으나, 표준 규정에 따라 일부가 2셀 복합일 수 있음.
     *   국립국어원 한국 점자 규정 원문으로 재확인 권장.
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
        setOf(2, 4) to "ㅡ",        // [해결됨] 초성 ㄷ {2,4}와 점형 충돌 → State Machine으로 해결
        setOf(1, 3, 5) to "ㅣ",
        setOf(1, 2, 3, 5) to "ㅐ",
        setOf(1, 3, 4, 5) to "ㅔ",
        setOf(2, 4, 5) to "ㅚ",     // [해결됨] 초성 ㅎ {2,4,5}와 점형 충돌 → State Machine으로 해결
        setOf(1, 2, 3, 6) to "ㅘ",  // [확인 필요] 2셀 복합일 수 있음
        setOf(1, 2, 3, 4) to "ㅝ",  // [확인 필요] 2셀 복합일 수 있음
        setOf(1, 3, 4, 6) to "ㅟ",  // [확인 필요] 2셀 복합일 수 있음
        setOf(2, 4, 5, 6) to "ㅢ"   // [확인 필요] 2셀 복합일 수 있음
        // [미해결] ㅒ, ㅙ, ㅞ — 2셀 복합 모음, State Machine에 sub-state 추가 전까지 입력 불가
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
        setOf(1, 2, 3) to "ㅋ",
        setOf(1, 3, 6) to "ㅌ",
        setOf(1, 2, 3, 6) to "ㅍ",
        setOf(3, 5, 6) to "ㅎ"
        // [미해결] 겹받침 미포함 — 점형 확인 후 이 테이블에 추가하면 State Machine 수정 없이 동작
    )

    /**
     * 한글 약자 디코딩 테이블 (표준 점자 기준).
     * 수취인 이름 입력 등 명사에 사용될 수 있는 약자(가~하, 억~을)만 포함.
     *
     * [미해결] 현재 KoreanBrailleStateMachine에서 이 테이블을 사용하지 않음
     *   State Machine은 initialConsonantTable / vowelTable / finalConsonantTable만 직접 조회함.
     *   약자 지원을 추가하려면 State Machine에 "앞뒤 셀이 자음/모음이 아닌 독립 셀"을 감지하는
     *   별도 상태(EXPECT_ABBREVIATION_OR_INITIAL 등)와 lookahead 로직이 필요.
     *
     * [미해결] 나/다/마/바/자/카/타/파/하 — 초성 테이블과 점형 동일
     *   문맥(독립 셀인지 음절의 초성인지)으로만 구분 가능. 현재 State Machine 미지원.
     *
     * [미해결] 약자 간 점형 충돌
     *   '운' / '영' 둘 다 {1,2,4,5,6}으로 겹침, '옹' / '인' 둘 다 {1,2,3,4,5,6}으로 겹침.
     *   Map key 중복이라 하나만 적용됨. 완전한 지원을 위해서는 규칙 기반 분기 필요.
     */
    val abbreviationTable: Map<Set<Int>, String> = mapOf(
        setOf(1, 2, 4, 6) to "가",   // 유일하게 초성 테이블과 다른 독립 점형
        // 아래는 초성 점형과 동일 → State Machine에서 초성으로 해석되므로 약자로 동작 안 함
        setOf(1, 4) to "나",
        setOf(2, 4) to "다",
        setOf(1, 5) to "마",
        setOf(4, 5) to "바",
        setOf(1, 2, 3) to "사",
        setOf(4, 6) to "자",
        setOf(1, 2, 4) to "카",
        setOf(1, 2, 5) to "타",
        setOf(1, 4, 5) to "파",
        setOf(2, 4, 5) to "하",

        setOf(1, 4, 5, 6) to "억",
        setOf(2, 3, 4, 5, 6) to "언",
        setOf(2, 3, 4, 5) to "얼",
        setOf(1, 6) to "연",
        setOf(1, 2, 5, 6) to "열",
        setOf(1, 2, 4, 5, 6) to "영", // [미해결] '운'과 점형 충돌 {1,2,4,5,6}
        setOf(1, 3, 4, 5, 6) to "옥",
        setOf(1, 2, 3, 5, 6) to "온",
        setOf(1, 2, 3, 4, 5, 6) to "옹", // [미해결] '인'과 점형 충돌 {1,2,3,4,5,6}
        setOf(1, 2, 3, 4, 6) to "울",
        setOf(1, 3, 5, 6) to "은",
        setOf(2, 3, 4, 6) to "을"
    )

    fun isNumberPrefix(dots: Set<Int>): Boolean = dots == NUMBER_PREFIX

    fun decodeNumber(dots: Set<Int>): Char? = numberTable[dots]

    /**
     * 단일 셀을 한글 자모 또는 약자로 디코딩. 우선순위: 약자 → 초성 → 중성.
     *
     * [참고] KoreanBrailleStateMachine 도입 후 BrailleViewModel에서 직접 호출되지 않음.
     *   State Machine이 각 테이블을 상태에 맞게 직접 조회하므로 이 함수는 사실상 미사용.
     *   약자를 포함한 단순 1-셀 조회가 필요한 경우(디버깅, 테스트 등)를 위해 유지.
     */
    fun decodeKoreanCell(dots: Set<Int>): String? {
        return abbreviationTable[dots]
            ?: initialConsonantTable[dots]
            ?: vowelTable[dots]
    }
}