package com.toeicquiz.backend.service;

import com.toeicquiz.backend.domain.UserWordMastery;
import io.github.openspacedrepetition.Rating;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class FsrsSchedulerTest {

    private final FsrsScheduler scheduler = new FsrsScheduler();

    // 確定した評価マッピング:今回の解答で使ったヒント量が基準(累積の習熟度スコアではない)
    @Test
    void incorrectIsAlwaysAgain() {
        assertEquals(Rating.AGAIN, scheduler.ratingFor(false, 0));
        assertEquals(Rating.AGAIN, scheduler.ratingFor(false, 0.8));
    }

    @Test
    void correctWithNoHintsIsEasy() {
        assertEquals(Rating.EASY, scheduler.ratingFor(true, 0));
    }

    @Test
    void correctWithLightHintsIsGood() {
        assertEquals(Rating.GOOD, scheduler.ratingFor(true, 0.15));
        assertEquals(Rating.GOOD, scheduler.ratingFor(true, 0.30));
    }

    @Test
    void correctWithHeavyHintsIsHard() {
        assertEquals(Rating.HARD, scheduler.ratingFor(true, 0.31));
        assertEquals(Rating.HARD, scheduler.ratingFor(true, 0.8));
    }

    @Test
    void neverStudiedWordCategorizesAsNew() {
        assertEquals(FsrsScheduler.DueCategory.NEW, scheduler.categorize(null, Instant.now()));
    }

    @Test
    void categorizeReflectsLearningAndReviewStates() {
        UserWordMastery learningRow = new UserWordMastery();
        learningRow.setUser(1L);
        learningRow.setWord(3L);
        scheduler.review(learningRow, 3L, false, 0); // AGAIN -> stays in LEARNING, due ~1 minute later
        assertEquals(FsrsScheduler.DueCategory.LEARNING,
                scheduler.categorize(learningRow, Instant.now().plus(java.time.Duration.ofMinutes(2))));

        UserWordMastery reviewRow = new UserWordMastery();
        reviewRow.setUser(1L);
        reviewRow.setWord(4L);
        scheduler.review(reviewRow, 4L, true, 0); // EASY -> graduates straight to REVIEW, due weeks out
        assertEquals(FsrsScheduler.DueCategory.NOT_DUE, scheduler.categorize(reviewRow, Instant.now()));
        assertEquals(FsrsScheduler.DueCategory.REVIEW,
                scheduler.categorize(reviewRow, Instant.now().plus(java.time.Duration.ofDays(60))));
    }

    @Test
    void reviewingNewWordFillsFsrsFieldsAndSchedulesDueDate() {
        UserWordMastery row = new UserWordMastery();
        row.setUser(1L);
        row.setWord(1L);

        Rating rating = scheduler.review(row, 1L, true, 0.45);

        assertEquals(Rating.HARD, rating);
        assertNotNull(row.getFsrsState());
        assertNotNull(row.getFsrsStability());
        assertNotNull(row.getFsrsDifficulty());
        assertNotNull(row.getDueAt());
        assertEquals(FsrsScheduler.DueCategory.NOT_DUE, scheduler.categorize(row, Instant.now()));
    }

    @Test
    void lapseAfterGraduatingReschedulesSoon() {
        UserWordMastery row = new UserWordMastery();
        row.setUser(1L);
        row.setWord(2L);

        // Two hint-free correct answers (Easy) graduate the card out of learning.
        scheduler.review(row, 2L, true, 0);
        row.setLastStudied(Instant.now().toString());
        scheduler.review(row, 2L, true, 0);
        row.setLastStudied(Instant.now().toString());

        // Forgetting it afterwards must push the due date back to relearning-step range
        // (minutes), not the long review interval (days) the card had before the lapse.
        scheduler.review(row, 2L, false, 0);
        assertEquals(FsrsScheduler.DueCategory.LEARNING,
                scheduler.categorize(row, Instant.now().plus(java.time.Duration.ofMinutes(30))));
    }
}
