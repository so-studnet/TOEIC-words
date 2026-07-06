package com.toeicquiz.backend.service;

import com.toeicquiz.backend.domain.UserWordMastery;
import io.github.openspacedrepetition.Rating;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FsrsSchedulerTest {

    private final FsrsScheduler scheduler = new FsrsScheduler();

    // 確定した習熟度→Anki評価のマッピング
    @Test
    void incorrectIsAlwaysAgain() {
        assertEquals(Rating.AGAIN, scheduler.ratingFor(false, 90));
    }

    @Test
    void masteryOneHundredIsEasy() {
        assertEquals(Rating.EASY, scheduler.ratingFor(true, 100));
    }

    @Test
    void masteryEightyToNinetyNineIsGood() {
        assertEquals(Rating.GOOD, scheduler.ratingFor(true, 80));
        assertEquals(Rating.GOOD, scheduler.ratingFor(true, 99));
    }

    @Test
    void masteryFiftyToSeventyNineIsHard() {
        assertEquals(Rating.HARD, scheduler.ratingFor(true, 50));
        assertEquals(Rating.HARD, scheduler.ratingFor(true, 79));
    }

    @Test
    void masteryBelowFiftyIsAgainEvenWhenCorrect() {
        assertEquals(Rating.AGAIN, scheduler.ratingFor(true, 49));
        assertEquals(Rating.AGAIN, scheduler.ratingFor(true, 0));
    }

    @Test
    void neverStudiedWordIsDue() {
        assertTrue(scheduler.isDue(null, Instant.now()));
    }

    @Test
    void reviewingNewWordFillsFsrsFieldsAndSchedulesDueDate() {
        UserWordMastery row = new UserWordMastery();
        row.setUser(1L);
        row.setWord(1L);

        scheduler.review(row, 1L, true, 20);

        assertNotNull(row.getFsrsState());
        assertNotNull(row.getFsrsStability());
        assertNotNull(row.getFsrsDifficulty());
        assertNotNull(row.getDueAt());
        assertFalse(scheduler.isDue(row, Instant.now()));
    }

    @Test
    void lapseAfterGraduatingReschedulesSoon() {
        UserWordMastery row = new UserWordMastery();
        row.setUser(1L);
        row.setWord(2L);

        // Two correct(Easy) reviews graduate the card out of learning.
        scheduler.review(row, 2L, true, 100);
        row.setLastStudied(Instant.now().toString());
        scheduler.review(row, 2L, true, 100);
        row.setLastStudied(Instant.now().toString());

        // Forgetting it afterwards must push the due date back to relearning-step range
        // (minutes), not the long review interval (days) the card had before the lapse.
        scheduler.review(row, 2L, false, 0);
        assertTrue(scheduler.isDue(row, Instant.now().plus(java.time.Duration.ofMinutes(30))));
    }
}
