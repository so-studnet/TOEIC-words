package com.toeicquiz.backend.domain;

public enum HintType {
    DEFINITION(0.15),
    SYNONYMS(0.15),
    PRONUNCIATION(0.20),
    SHUFFLE(0.30),
    IMAGE(0.10);

    private final double penalty;

    HintType(double penalty) {
        this.penalty = penalty;
    }

    public double getPenalty() {
        return penalty;
    }

    public static HintType fromKey(String key) {
        return switch (key) {
            case "definition" -> DEFINITION;
            case "synonyms" -> SYNONYMS;
            case "pronunciation" -> PRONUNCIATION;
            case "shuffle" -> SHUFFLE;
            case "image" -> IMAGE;
            default -> throw new IllegalArgumentException("Unknown hint type: " + key);
        };
    }
}
