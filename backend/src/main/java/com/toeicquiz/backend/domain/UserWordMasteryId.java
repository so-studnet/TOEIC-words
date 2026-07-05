package com.toeicquiz.backend.domain;

import java.io.Serializable;
import java.util.Objects;

public class UserWordMasteryId implements Serializable {

    private Long user;
    private Long word;

    public UserWordMasteryId() {}

    public UserWordMasteryId(Long user, Long word) {
        this.user = user;
        this.word = word;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserWordMasteryId that)) return false;
        return Objects.equals(user, that.user) && Objects.equals(word, that.word);
    }

    @Override
    public int hashCode() {
        return Objects.hash(user, word);
    }
}
