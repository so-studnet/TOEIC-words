package com.toeicquiz.backend.controller;

import com.toeicquiz.backend.dto.AnswerRequest;
import com.toeicquiz.backend.dto.AnswerResponse;
import com.toeicquiz.backend.security.AppUserPrincipal;
import com.toeicquiz.backend.service.QuizService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/quiz")
public class QuizController {

    private final QuizService quizService;

    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    @PostMapping("/answer")
    public AnswerResponse answer(@Valid @RequestBody AnswerRequest req,
                                  @AuthenticationPrincipal AppUserPrincipal principal) {
        return quizService.submitAnswer(principal.getId(), req.getWordId(), req.getAnswer(), req.getHintsUsed());
    }
}
