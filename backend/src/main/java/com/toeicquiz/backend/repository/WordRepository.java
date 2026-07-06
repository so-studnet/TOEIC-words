package com.toeicquiz.backend.repository;

import com.toeicquiz.backend.domain.Word;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WordRepository extends JpaRepository<Word, Long> {

    List<Word> findByLevel(int level);
}
