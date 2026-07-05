package com.toeicquiz.backend.repository;

import com.toeicquiz.backend.domain.Word;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WordRepository extends JpaRepository<Word, Long> {

    List<Word> findByLevel(int level);

    @Query(value = "SELECT * FROM words WHERE level = :level ORDER BY RANDOM() LIMIT :count", nativeQuery = true)
    List<Word> findRandomByLevel(@Param("level") int level, @Param("count") int count);
}
