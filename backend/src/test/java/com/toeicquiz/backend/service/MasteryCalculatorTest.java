package com.toeicquiz.backend.service;

import com.toeicquiz.backend.domain.HintType;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MasteryCalculatorTest {

    private final MasteryCalculator calculator = new MasteryCalculator();

    // Part1 詳細設計書 §2.4 の計算例
    @Test
    void noHintCorrect() {
        assertEquals(20, calculator.calculateGain(Set.of()));
    }

    @Test
    void definitionAndShuffleCorrect() {
        assertEquals(11, calculator.calculateGain(Set.of(HintType.DEFINITION, HintType.SHUFFLE)));
    }

    @Test
    void allHintsCorrect() {
        assertEquals(4, calculator.calculateGain(
                Set.of(HintType.DEFINITION, HintType.SYNONYMS, HintType.PRONUNCIATION,
                        HintType.SHUFFLE, HintType.IMAGE)));
    }

    @Test
    void incorrectNormal() {
        assertEquals(10, calculator.calculateLoss(50));
    }

    @Test
    void incorrectCloseMiss() {
        assertEquals(5, calculator.calculateLoss(85));
    }

    @Test
    void clipUpperBound() {
        assertEquals(100, calculator.applyDelta(95, 20));
    }

    @Test
    void clipLowerBound() {
        assertEquals(0, calculator.applyDelta(3, -10));
    }

    @Test
    void rankLabels() {
        assertEquals("マスター", calculator.rankOf(80));
        assertEquals("学習中", calculator.rankOf(40));
        assertEquals("うろ覚え", calculator.rankOf(1));
        assertEquals("未学習", calculator.rankOf(0));
    }
}
