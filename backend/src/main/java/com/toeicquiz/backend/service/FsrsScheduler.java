package com.toeicquiz.backend.service;

import com.toeicquiz.backend.domain.UserWordMastery;
import io.github.openspacedrepetition.Card;
import io.github.openspacedrepetition.Rating;
import io.github.openspacedrepetition.Scheduler;
import io.github.openspacedrepetition.State;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Part1 詳細設計書 §2.6: 今回の解答で使ったヒント量をAnki(FSRS)評価に変換し、
 * 公式FSRSライブラリ(io.github.open-spaced-repetition:fsrs)で次回復習日時を算出する。
 */
@Component
public class FsrsScheduler {

    private final Scheduler scheduler = Scheduler.builder().build();

    private static final double GOOD_MAX_PENALTY = 0.30;

    /** 今回の解答で使ったヒントの量(累積の習熟度スコアではない)を基準に評価する。 */
    public Rating ratingFor(boolean correct, double hintPenalty) {
        if (!correct) return Rating.AGAIN;
        if (hintPenalty <= 0) return Rating.EASY;
        if (hintPenalty <= GOOD_MAX_PENALTY) return Rating.GOOD;
        return Rating.HARD;
    }

    public Rating review(UserWordMastery masteryRow, Long wordId, boolean correct, double hintPenalty) {
        Rating rating = ratingFor(correct, hintPenalty);
        Card card = toCard(masteryRow, wordId);
        Card updated = scheduler.reviewCard(card, rating).card();
        applyCard(masteryRow, updated);
        return rating;
    }

    /** Anki風の3区分(New/Learning/Review)+未期限(NOT_DUE)への分類。 */
    public DueCategory categorize(UserWordMastery masteryRow, Instant now) {
        if (masteryRow == null || masteryRow.getFsrsState() == null) {
            return DueCategory.NEW;
        }
        if (Instant.parse(masteryRow.getDueAt()).isAfter(now)) {
            return DueCategory.NOT_DUE;
        }
        return switch (State.valueOf(masteryRow.getFsrsState())) {
            case REVIEW -> DueCategory.REVIEW;
            case LEARNING, RELEARNING -> DueCategory.LEARNING;
        };
    }

    public enum DueCategory { NEW, LEARNING, REVIEW, NOT_DUE }

    private Card toCard(UserWordMastery row, Long wordId) {
        Card.Builder builder = Card.builder().cardId(wordId.intValue());
        if (row.getFsrsState() != null) {
            builder.state(State.valueOf(row.getFsrsState()))
                    .step(row.getFsrsStep())
                    .stability(row.getFsrsStability())
                    .difficulty(row.getFsrsDifficulty())
                    .due(Instant.parse(row.getDueAt()))
                    .lastReview(row.getLastStudied() == null ? null : Instant.parse(row.getLastStudied()));
        }
        return builder.build();
    }

    private void applyCard(UserWordMastery row, Card card) {
        row.setFsrsState(card.getState().name());
        row.setFsrsStep(card.getStep());
        row.setFsrsStability(card.getStability());
        row.setFsrsDifficulty(card.getDifficulty());
        row.setDueAt(card.getDue().toString());
    }
}
