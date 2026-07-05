package com.toeicquiz.backend.dto;

import com.toeicquiz.backend.domain.Word;

import java.util.Arrays;
import java.util.List;

public class WordDto {
    private Long id;
    private String word;
    private String definition;
    private String example;
    private List<String> synonyms;
    private String imageUrl;

    public static WordDto from(Word w) {
        WordDto dto = new WordDto();
        dto.id = w.getId();
        dto.word = w.getWord();
        dto.definition = w.getDefinition();
        dto.example = w.getExample();
        dto.synonyms = w.getSynonyms() == null || w.getSynonyms().isBlank()
                ? List.of()
                : Arrays.stream(w.getSynonyms().split(",")).map(String::trim).toList();
        dto.imageUrl = w.getImageUrl();
        return dto;
    }

    public Long getId() { return id; }
    public String getWord() { return word; }
    public String getDefinition() { return definition; }
    public String getExample() { return example; }
    public List<String> getSynonyms() { return synonyms; }
    public String getImageUrl() { return imageUrl; }
}
