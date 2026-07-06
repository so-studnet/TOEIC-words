package com.toeicquiz.backend.controller;

import com.toeicquiz.backend.dto.ReviewSummaryResponse;
import com.toeicquiz.backend.dto.WordDto;
import com.toeicquiz.backend.dto.WordsResponse;
import com.toeicquiz.backend.security.AppUserPrincipal;
import com.toeicquiz.backend.service.QuizService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WordController {

    private final QuizService quizService;

    public WordController(QuizService quizService) {
        this.quizService = quizService;
    }

    @GetMapping("/api/words")
    public WordsResponse getWords(@RequestParam int level, @AuthenticationPrincipal AppUserPrincipal principal) {
        var words = quizService.getWordsForLevel(principal.getId(), level).stream().map(WordDto::from).toList();
        return new WordsResponse(level, words);
    }

    @GetMapping("/api/review-summary")
    public ReviewSummaryResponse getReviewSummary(@AuthenticationPrincipal AppUserPrincipal principal) {
        return new ReviewSummaryResponse(quizService.getReviewSummary(principal.getId()));
    }
}
