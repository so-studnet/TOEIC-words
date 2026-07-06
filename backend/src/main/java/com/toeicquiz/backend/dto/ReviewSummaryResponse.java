package com.toeicquiz.backend.dto;

import java.util.List;

public class ReviewSummaryResponse {
    private List<LevelReviewSummaryDto> levels;

    public ReviewSummaryResponse(List<LevelReviewSummaryDto> levels) {
        this.levels = levels;
    }

    public List<LevelReviewSummaryDto> getLevels() { return levels; }
}
