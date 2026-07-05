package com.toeicquiz.backend.service;

import com.toeicquiz.backend.domain.UserWordMastery;
import com.toeicquiz.backend.domain.Word;
import com.toeicquiz.backend.dto.MasteryDto;
import com.toeicquiz.backend.repository.UserWordMasteryRepository;
import com.toeicquiz.backend.repository.WordRepository;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
public class MasteryService {

    private final WordRepository wordRepository;
    private final UserWordMasteryRepository masteryRepository;
    private final MasteryCalculator masteryCalculator;

    public MasteryService(WordRepository wordRepository,
                           UserWordMasteryRepository masteryRepository,
                           MasteryCalculator masteryCalculator) {
        this.wordRepository = wordRepository;
        this.masteryRepository = masteryRepository;
        this.masteryCalculator = masteryCalculator;
    }

    public List<MasteryDto> getMasteryList(Long userId, Integer level) {
        List<Word> words = level == null ? wordRepository.findAll() : wordRepository.findByLevel(level);

        Map<Long, UserWordMastery> masteryByWordId = masteryRepository.findByUser(userId).stream()
                .collect(java.util.stream.Collectors.toMap(UserWordMastery::getWord, Function.identity()));

        return words.stream()
                .map(word -> {
                    UserWordMastery m = masteryByWordId.get(word.getId());
                    int mastery = m == null ? 0 : m.getMastery();
                    int attempts = m == null ? 0 : m.getAttempts();
                    int correctCount = m == null ? 0 : m.getCorrectCount();
                    int hintsUsedTotal = m == null ? 0 : m.getHintsUsedTotal();
                    String lastStudied = m == null ? null : m.getLastStudied();
                    List<String> synonyms = word.getSynonyms() == null || word.getSynonyms().isBlank()
                            ? List.of()
                            : Arrays.stream(word.getSynonyms().split(",")).map(String::trim).toList();
                    return new MasteryDto(word.getId(), word.getWord(), word.getLevel(),
                            mastery, masteryCalculator.rankOf(mastery),
                            attempts, correctCount, hintsUsedTotal, lastStudied,
                            word.getDefinition(), word.getExample(), synonyms);
                })
                .toList();
    }
}
