package com.toeicquiz.backend.dto;

public class AnswerResponse {
    private boolean correct;
    private int similarityPercent;
    private String correctWord;
    private int masteryBefore;
    private int masteryAfter;
    private int masteryDelta;

    public AnswerResponse(boolean correct, int similarityPercent, String correctWord,
                           int masteryBefore, int masteryAfter) {
        this.correct = correct;
        this.similarityPercent = similarityPercent;
        this.correctWord = correctWord;
        this.masteryBefore = masteryBefore;
        this.masteryAfter = masteryAfter;
        this.masteryDelta = masteryAfter - masteryBefore;
    }

    public boolean isCorrect() { return correct; }
    public int getSimilarityPercent() { return similarityPercent; }
    public String getCorrectWord() { return correctWord; }
    public int getMasteryBefore() { return masteryBefore; }
    public int getMasteryAfter() { return masteryAfter; }
    public int getMasteryDelta() { return masteryDelta; }
}
