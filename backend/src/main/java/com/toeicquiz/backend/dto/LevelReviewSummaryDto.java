package com.toeicquiz.backend.dto;

public class LevelReviewSummaryDto {
    private int level;
    private int newCount;
    private int learningCount;
    private int reviewCount;

    public LevelReviewSummaryDto(int level, int newCount, int learningCount, int reviewCount) {
        this.level = level;
        this.newCount = newCount;
        this.learningCount = learningCount;
        this.reviewCount = reviewCount;
    }

    public int getLevel() { return level; }
    public int getNewCount() { return newCount; }
    public int getLearningCount() { return learningCount; }
    public int getReviewCount() { return reviewCount; }
    public int getDueTotal() { return newCount + learningCount + reviewCount; }
}
