package com.toeicquiz.backend.service;

import com.toeicquiz.backend.domain.UserWordMastery;
import io.github.openspacedrepetition.Card;
import io.github.openspacedrepetition.Rating;
import io.github.openspacedrepetition.Scheduler;
import io.github.openspacedrepetition.State;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Part1 詳細設計書 §2.6: 習熟度スコアをAnki(FSRS)評価に変換し、
 * 公式FSRSライブラリ(io.github.open-spaced-repetition:fsrs)で次回復習日時を算出する。
 */
@Component
public class FsrsScheduler {

    private final Scheduler scheduler = Scheduler.builder().build();

    public Rating ratingFor(boolean correct, int masteryAfter) {
        if (!correct) return Rating.AGAIN;
        if (masteryAfter == 100) return Rating.EASY;
        if (masteryAfter >= 80) return Rating.GOOD;
        if (masteryAfter >= 50) return Rating.HARD;
        return Rating.AGAIN;
    }

    public void review(UserWordMastery masteryRow, Long wordId, boolean correct, int masteryAfter) {
        Rating rating = ratingFor(correct, masteryAfter);
        Card card = toCard(masteryRow, wordId);
        Card updated = scheduler.reviewCard(card, rating).card();
        applyCard(masteryRow, updated);
    }

    public boolean isDue(UserWordMastery masteryRow, Instant now) {
        if (masteryRow == null || masteryRow.getDueAt() == null) {
            return true;
        }
        return !Instant.parse(masteryRow.getDueAt()).isAfter(now);
    }

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
