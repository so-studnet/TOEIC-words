package com.toeicquiz.backend.config;

import com.fasterxml.jackson.annotation.JsonProperty;

public class WordSeedEntry {
    public String word;
    public int level;
    public String definition;
    public String example;
    public String synonyms;

    @JsonProperty("image_url")
    public String imageUrl;
}
