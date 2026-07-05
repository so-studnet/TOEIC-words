package com.toeicquiz.backend.service;

import com.toeicquiz.backend.domain.HintType;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Part1 詳細設計書 §2: 習熟度計算。
 */
@Component
public class MasteryCalculator {

    private static final int BASE_GAIN = 20;
    private static final int BASE_LOSS = 10;
    private static final int CLOSE_MISS_THRESHOLD = 80;
    private static final double MAX_HINT_PENALTY = 0.8;

    public int calculateGain(Set<HintType> hintsUsed) {
        double totalPenalty = hintsUsed.stream().mapToDouble(HintType::getPenalty).sum();
        totalPenalty = Math.min(totalPenalty, MAX_HINT_PENALTY);
        // add a small epsilon to guard against floating-point rounding before flooring
        return (int) Math.floor(BASE_GAIN * (1 - totalPenalty) + 1e-9);
    }

    public int calculateLoss(int similarityPercent) {
        return similarityPercent >= CLOSE_MISS_THRESHOLD ? BASE_LOSS / 2 : BASE_LOSS;
    }

    public int applyDelta(int currentMastery, int delta) {
        return Math.max(0, Math.min(100, currentMastery + delta));
    }

    public String rankOf(int mastery) {
        if (mastery >= 80) return "マスター";
        if (mastery >= 40) return "学習中";
        if (mastery >= 1) return "うろ覚え";
        return "未学習";
    }
}
