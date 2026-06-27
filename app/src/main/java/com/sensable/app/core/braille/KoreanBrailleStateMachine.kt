package com.sensable.app.core.braille

/**
 * 한글 점자 셀 시퀀스를 완성된 한글 음절로 조합하는 상태 머신.
 *
 * 상태 전이:
 *   EXPECT_INITIAL → (초성 셀) → EXPECT_VOWEL
 *   EXPECT_INITIAL → (단순 모음 셀) → ㅇ 초성으로 처리 → EXPECT_FINAL_OR_NEXT_INITIAL
 *   EXPECT_INITIAL → (복합 모음 첫 셀) → ㅇ 초성으로 처리 → EXPECT_COMPOUND_VOWEL_CONTINUATION
 *   EXPECT_VOWEL   → (단순 모음 셀) → EXPECT_FINAL_OR_NEXT_INITIAL
 *   EXPECT_VOWEL   → (복합 모음 첫 셀) → EXPECT_COMPOUND_VOWEL_CONTINUATION
 *   EXPECT_COMPOUND_VOWEL_CONTINUATION → (ㅐ 셀) → 복합 모음 확정 → EXPECT_FINAL_OR_NEXT_INITIAL
 *   EXPECT_COMPOUND_VOWEL_CONTINUATION → (그 외) → 첫 셀을 단독 모음으로 확정 후 현재 셀 재처리
 *   EXPECT_FINAL_OR_NEXT_INITIAL → (종성 셀) → 음절 완성 → EXPECT_INITIAL
 *   EXPECT_FINAL_OR_NEXT_INITIAL → (초성 셀) → 이전 음절 완성 후 새 초성 → EXPECT_VOWEL
 *   EXPECT_FINAL_OR_NEXT_INITIAL → (단순 모음 셀) → 이전 음절 완성 후 ㅇ 초성 → EXPECT_FINAL_OR_NEXT_INITIAL
 *   EXPECT_FINAL_OR_NEXT_INITIAL → (복합 모음 첫 셀) → 이전 음절 완성 후 ㅇ 초성 → EXPECT_COMPOUND_VOWEL_CONTINUATION
 *
 * 복합 모음(ㅟ/ㅒ/ㅙ/ㅞ)은 모두 첫 번째 셀 + ㅐ{1,2,3,5} 두 번째 셀의 조합으로 입력.
 * process()는 음절이 완성될 때만 문자를 반환하며, 미완성이면 빈 문자열 반환.
 * 입력 필드 제출 시 flush()를 호출해 마지막 미완성 음절을 강제 완성해야 함.
 */
class KoreanBrailleStateMachine {

    private enum class State {
        EXPECT_INITIAL,
        EXPECT_VOWEL,
        EXPECT_COMPOUND_VOWEL_CONTINUATION,
        EXPECT_FINAL_OR_NEXT_INITIAL,
    }

    private var state = State.EXPECT_INITIAL
    private var pendingInitial: Int? = null
    private var pendingVowel: Int? = null
    private var pendingFirstVowelDots: Set<Int>? = null
    private val output = StringBuilder()

    fun process(dots: Set<Int>): String {
        output.clear()
        when (state) {
            State.EXPECT_INITIAL -> handleExpectInitial(dots)
            State.EXPECT_VOWEL -> handleExpectVowel(dots)
            State.EXPECT_COMPOUND_VOWEL_CONTINUATION -> handleExpectCompoundVowelContinuation(dots)
            State.EXPECT_FINAL_OR_NEXT_INITIAL -> handleExpectFinalOrNext(dots)
        }
        return output.toString()
    }

    fun flush(): String {
        output.clear()
        if (state == State.EXPECT_COMPOUND_VOWEL_CONTINUATION) {
            commitPendingFirstVowelAsStandalone()
        }
        assemblePendingSyllable(finalIndex = 0)
        val result = output.toString()
        reset()
        return result
    }

    fun reset() {
        state = State.EXPECT_INITIAL
        pendingInitial = null
        pendingVowel = null
        pendingFirstVowelDots = null
        output.clear()
    }

    /** 현재 조합 중인 음절 미리보기. 초성만 있으면 자모, 초성+중성이면 결합 음절. */
    fun getPendingDisplay(): String {
        val initial = pendingInitial ?: return ""
        val vowel = pendingVowel
        return if (vowel == null) {
            INITIAL_CONSONANTS.getOrElse(initial) { "" }
        } else {
            (0xAC00 + initial * 21 * 28 + vowel * 28).toChar().toString()
        }
    }

    private fun handleExpectInitial(dots: Set<Int>) {
        val consonant = BrailleDecoder.initialConsonantTable[dots]
        if (consonant != null) {
            pendingInitial = INITIAL_INDEX[consonant]
            state = State.EXPECT_VOWEL
            return
        }
        if (dots in COMPOUND_FIRST_CELLS) {
            pendingInitial = INITIAL_INDEX["ㅇ"]
            pendingFirstVowelDots = dots
            state = State.EXPECT_COMPOUND_VOWEL_CONTINUATION
            return
        }
        val vowel = BrailleDecoder.vowelTable[dots]
        if (vowel != null) {
            pendingInitial = INITIAL_INDEX["ㅇ"]
            pendingVowel = VOWEL_INDEX[vowel]
            state = State.EXPECT_FINAL_OR_NEXT_INITIAL
        }
    }

    private fun handleExpectVowel(dots: Set<Int>) {
        if (dots in COMPOUND_FIRST_CELLS) {
            pendingFirstVowelDots = dots
            state = State.EXPECT_COMPOUND_VOWEL_CONTINUATION
            return
        }
        val vowel = BrailleDecoder.vowelTable[dots]
        if (vowel != null) {
            pendingVowel = VOWEL_INDEX[vowel]
            state = State.EXPECT_FINAL_OR_NEXT_INITIAL
        }
    }

    private fun handleExpectCompoundVowelContinuation(dots: Set<Int>) {
        if (dots == COMPOUND_SECOND_CELL) {
            val compound = COMPOUND_VOWEL_RESULT[pendingFirstVowelDots]
            if (compound != null) {
                pendingVowel = VOWEL_INDEX[compound]
                pendingFirstVowelDots = null
                state = State.EXPECT_FINAL_OR_NEXT_INITIAL
            }
            return
        }
        // 두 번째 셀이 ㅐ가 아니면 첫 셀을 단독 모음으로 확정하고 현재 셀 재처리
        commitPendingFirstVowelAsStandalone()
        handleExpectFinalOrNext(dots)
    }

    private fun commitPendingFirstVowelAsStandalone() {
        val firstDots = pendingFirstVowelDots ?: return
        val vowel = BrailleDecoder.vowelTable[firstDots]
        if (vowel != null) {
            pendingVowel = VOWEL_INDEX[vowel]
        }
        pendingFirstVowelDots = null
        state = State.EXPECT_FINAL_OR_NEXT_INITIAL
    }

    private fun handleExpectFinalOrNext(dots: Set<Int>) {
        // 종성 테이블 먼저 — 초성/종성 점형이 다르므로 겹치지 않음
        val finalConsonant = BrailleDecoder.finalConsonantTable[dots]
        if (finalConsonant != null) {
            assemblePendingSyllable(FINAL_INDEX[finalConsonant] ?: 0)
            state = State.EXPECT_INITIAL
            return
        }
        // 다음 음절의 초성
        val nextInitial = BrailleDecoder.initialConsonantTable[dots]
        if (nextInitial != null) {
            assemblePendingSyllable(finalIndex = 0)
            pendingInitial = INITIAL_INDEX[nextInitial]
            state = State.EXPECT_VOWEL
            return
        }
        // 다음 음절이 ㅇ 초성 + 복합 모음
        if (dots in COMPOUND_FIRST_CELLS) {
            assemblePendingSyllable(finalIndex = 0)
            pendingInitial = INITIAL_INDEX["ㅇ"]
            pendingFirstVowelDots = dots
            state = State.EXPECT_COMPOUND_VOWEL_CONTINUATION
            return
        }
        // 다음 음절이 ㅇ 초성 + 단순 모음
        val vowel = BrailleDecoder.vowelTable[dots]
        if (vowel != null) {
            assemblePendingSyllable(finalIndex = 0)
            pendingInitial = INITIAL_INDEX["ㅇ"]
            pendingVowel = VOWEL_INDEX[vowel]
            // 새 음절도 종성을 받을 수 있으므로 상태 유지
        }
    }

    private fun assemblePendingSyllable(finalIndex: Int) {
        val initial = pendingInitial ?: return
        val vowel = pendingVowel ?: return
        output.append((0xAC00 + initial * 21 * 28 + vowel * 28 + finalIndex).toChar())
        pendingInitial = null
        pendingVowel = null
    }

    companion object {
        // 유니코드 한글 음절 = 0xAC00 + 초성 * 21 * 28 + 중성 * 28 + 종성

        // 복합 모음 첫 번째 셀 집합 (ㅜ/ㅑ/ㅘ/ㅝ) — 다음 셀이 ㅐ이면 복합 모음으로 확정
        private val COMPOUND_FIRST_CELLS: Set<Set<Int>> = setOf(
            setOf(1, 3, 4),    // ㅜ → ㅟ
            setOf(3, 4, 5),    // ㅑ → ㅒ
            setOf(1, 2, 3, 6), // ㅘ → ㅙ
            setOf(1, 2, 3, 4), // ㅝ → ㅞ
        )

        // 복합 모음 두 번째 셀: ㅐ {1,2,3,5}
        private val COMPOUND_SECOND_CELL: Set<Int> = setOf(1, 2, 3, 5)

        // 첫 번째 셀 → 완성된 복합 모음 문자
        private val COMPOUND_VOWEL_RESULT: Map<Set<Int>, String> = mapOf(
            setOf(1, 3, 4) to "ㅟ",
            setOf(3, 4, 5) to "ㅒ",
            setOf(1, 2, 3, 6) to "ㅙ",
            setOf(1, 2, 3, 4) to "ㅞ",
        )

        // 초성 인덱스 → 자모 문자 (getPendingDisplay용)
        private val INITIAL_CONSONANTS = arrayOf(
            "ㄱ", "ㄲ", "ㄴ", "ㄷ", "ㄸ", "ㄹ", "ㅁ", "ㅂ", "ㅃ",
            "ㅅ", "ㅆ", "ㅇ", "ㅈ", "ㅉ", "ㅊ", "ㅋ", "ㅌ", "ㅍ", "ㅎ"
        )

        private val INITIAL_INDEX = mapOf(
            "ㄱ" to 0,  "ㄴ" to 2,  "ㄷ" to 3,  "ㄹ" to 5,
            "ㅁ" to 6,  "ㅂ" to 7,  "ㅅ" to 9,  "ㅇ" to 11,
            "ㅈ" to 12, "ㅊ" to 14, "ㅋ" to 15, "ㅌ" to 16,
            "ㅍ" to 17, "ㅎ" to 18,
        )

        private val VOWEL_INDEX = mapOf(
            "ㅏ" to 0,  "ㅐ" to 1,  "ㅑ" to 2,  "ㅒ" to 3,
            "ㅓ" to 4,  "ㅔ" to 5,  "ㅕ" to 6,  "ㅖ" to 7,
            "ㅗ" to 8,  "ㅘ" to 9,  "ㅙ" to 10, "ㅚ" to 11,
            "ㅛ" to 12, "ㅜ" to 13, "ㅝ" to 14, "ㅞ" to 15,
            "ㅟ" to 16, "ㅠ" to 17, "ㅡ" to 18, "ㅢ" to 19,
            "ㅣ" to 20,
        )

        private val FINAL_INDEX = mapOf(
            "ㄱ" to 1,  "ㄴ" to 4,  "ㄷ" to 7,  "ㄹ" to 8,
            "ㅁ" to 16, "ㅂ" to 17, "ㅅ" to 19, "ㅇ" to 21,
            "ㅈ" to 22, "ㅊ" to 23, "ㅋ" to 24, "ㅌ" to 25,
            "ㅍ" to 26, "ㅎ" to 27,
        )
    }
}
