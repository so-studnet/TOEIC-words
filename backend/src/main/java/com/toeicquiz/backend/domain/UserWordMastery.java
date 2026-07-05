package com.toeicquiz.backend.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "user_word_mastery")
@IdClass(UserWordMasteryId.class)
public class UserWordMastery {

    @Id
    @Column(name = "user_id")
    private Long user;

    @Id
    @Column(name = "word_id")
    private Long word;

    @Column(nullable = false)
    private int mastery = 0;

    @Column(nullable = false)
    private int attempts = 0;

    @Column(name = "correct_count", nullable = false)
    private int correctCount = 0;

    @Column(name = "hints_used_total", nullable = false)
    private int hintsUsedTotal = 0;

    @Column(name = "last_studied")
    private String lastStudied;

    public Long getUser() { return user; }
    public void setUser(Long user) { this.user = user; }

    public Long getWord() { return word; }
    public void setWord(Long word) { this.word = word; }

    public int getMastery() { return mastery; }
    public void setMastery(int mastery) { this.mastery = mastery; }

    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }

    public int getCorrectCount() { return correctCount; }
    public void setCorrectCount(int correctCount) { this.correctCount = correctCount; }

    public int getHintsUsedTotal() { return hintsUsedTotal; }
    public void setHintsUsedTotal(int hintsUsedTotal) { this.hintsUsedTotal = hintsUsedTotal; }

    public String getLastStudied() { return lastStudied; }
    public void setLastStudied(String lastStudied) { this.lastStudied = lastStudied; }
}
