package com.toeicquiz.backend.dto;

import java.util.List;

public class WordsResponse {
    private int level;
    private List<WordDto> words;

    public WordsResponse(int level, List<WordDto> words) {
        this.level = level;
        this.words = words;
    }

    public int getLevel() { return level; }
    public List<WordDto> getWords() { return words; }
}
