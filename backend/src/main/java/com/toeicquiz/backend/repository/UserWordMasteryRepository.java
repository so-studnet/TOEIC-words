package com.toeicquiz.backend.repository;

import com.toeicquiz.backend.domain.UserWordMastery;
import com.toeicquiz.backend.domain.UserWordMasteryId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserWordMasteryRepository extends JpaRepository<UserWordMastery, UserWordMasteryId> {
    Optional<UserWordMastery> findByUserAndWord(Long user, Long word);
    List<UserWordMastery> findByUser(Long user);
}
