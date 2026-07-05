package com.toeicquiz.backend.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SimilarityCalculatorTest {

    private final SimilarityCalculator calculator = new SimilarityCalculator();

    // Part1 詳細設計書 §1.4 の計算例(正解:comfortable、11文字)
    @Test
    void exactMatch() {
        assertEquals(100, calculator.calculate("comfortable", "comfortable"));
    }

    @Test
    void rlConfusion() {
        assertEquals(97, calculator.calculate("comfortabre", "comfortable"));
    }

    @Test
    void adjacentTransposition() {
        assertEquals(95, calculator.calculate("comfortalbe", "comfortable"));
    }

    @Test
    void normalSubstitution() {
        assertEquals(91, calculator.calculate("confortable", "comfortable"));
    }

    @Test
    void missingFourChars() {
        assertEquals(64, calculator.calculate("comfort", "comfortable"));
    }

    @Test
    void almostDifferentWord() {
        assertEquals(9, calculator.calculate("happy", "comfortable"));
    }

    @Test
    void caseInsensitive() {
        assertEquals(100, calculator.calculate("COMFORTABLE", "comfortable"));
    }
}
