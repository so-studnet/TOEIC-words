package com.toeicquiz.backend.dto;

public class AnswerResponse {
    private boolean correct;
    private int similarityPercent;
    private String correctWord;
    private String ratingLabel;
    private String dueAt;

    public AnswerResponse(boolean correct, int similarityPercent, String correctWord,
                           String ratingLabel, String dueAt) {
        this.correct = correct;
        this.similarityPercent = similarityPercent;
        this.correctWord = correctWord;
        this.ratingLabel = ratingLabel;
        this.dueAt = dueAt;
    }

    public boolean isCorrect() { return correct; }
    public int getSimilarityPercent() { return similarityPercent; }
    public String getCorrectWord() { return correctWord; }
    public String getRatingLabel() { return ratingLabel; }
    public String getDueAt() { return dueAt; }
}
