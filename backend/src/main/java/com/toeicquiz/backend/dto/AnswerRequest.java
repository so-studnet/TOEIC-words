package com.toeicquiz.backend.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Set;

public class AnswerRequest {

    @NotNull
    private Long wordId;

    private String answer = "";

    private Set<String> hintsUsed = Set.of();

    public Long getWordId() { return wordId; }
    public void setWordId(Long wordId) { this.wordId = wordId; }

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }

    public Set<String> getHintsUsed() { return hintsUsed; }
    public void setHintsUsed(Set<String> hintsUsed) { this.hintsUsed = hintsUsed; }
}
