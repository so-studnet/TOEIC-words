package com.toeicquiz.backend.dto;

import java.util.List;

public class MasteryDto {
    private Long wordId;
    private String word;
    private int level;
    private int mastery;
    private String rank;
    private int attempts;
    private int correctCount;
    private int hintsUsedTotal;
    private String lastStudied;
    private String dueAt;
    private String definition;
    private String example;
    private List<String> synonyms;

    public MasteryDto(Long wordId, String word, int level, int mastery, String rank,
                       int attempts, int correctCount, int hintsUsedTotal, String lastStudied, String dueAt,
                       String definition, String example, List<String> synonyms) {
        this.wordId = wordId;
        this.word = word;
        this.level = level;
        this.mastery = mastery;
        this.rank = rank;
        this.attempts = attempts;
        this.correctCount = correctCount;
        this.hintsUsedTotal = hintsUsedTotal;
        this.lastStudied = lastStudied;
        this.dueAt = dueAt;
        this.definition = definition;
        this.example = example;
        this.synonyms = synonyms;
    }

    public Long getWordId() { return wordId; }
    public String getWord() { return word; }
    public int getLevel() { return level; }
    public int getMastery() { return mastery; }
    public String getRank() { return rank; }
    public int getAttempts() { return attempts; }
    public int getCorrectCount() { return correctCount; }
    public int getHintsUsedTotal() { return hintsUsedTotal; }
    public String getLastStudied() { return lastStudied; }
    public String getDueAt() { return dueAt; }
    public String getDefinition() { return definition; }
    public String getExample() { return example; }
    public List<String> getSynonyms() { return synonyms; }
}
