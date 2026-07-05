package com.toeicquiz.backend.service;

import com.toeicquiz.backend.domain.HintType;
import com.toeicquiz.backend.domain.UserWordMastery;
import com.toeicquiz.backend.domain.Word;
import com.toeicquiz.backend.dto.AnswerResponse;
import com.toeicquiz.backend.repository.UserWordMasteryRepository;
import com.toeicquiz.backend.repository.WordRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class QuizService {

    private static final int WORDS_PER_SESSION = 10;

    private final WordRepository wordRepository;
    private final UserWordMasteryRepository masteryRepository;
    private final SimilarityCalculator similarityCalculator;
    private final MasteryCalculator masteryCalculator;

    public QuizService(WordRepository wordRepository,
                        UserWordMasteryRepository masteryRepository,
                        SimilarityCalculator similarityCalculator,
                        MasteryCalculator masteryCalculator) {
        this.wordRepository = wordRepository;
        this.masteryRepository = masteryRepository;
        this.similarityCalculator = similarityCalculator;
        this.masteryCalculator = masteryCalculator;
    }

    public List<Word> getWordsForLevel(int level) {
        return wordRepository.findRandomByLevel(level, WORDS_PER_SESSION);
    }

    public AnswerResponse submitAnswer(Long userId, Long wordId, String answer, Set<String> hintKeys) {
        Word word = wordRepository.findById(wordId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "WORD_NOT_FOUND", "The requested word does not exist."));

        Set<HintType> hintsUsed = hintKeys.stream()
                .map(this::parseHintKey)
                .collect(Collectors.toSet());

        int similarity = similarityCalculator.calculate(answer, word.getWord());
        boolean correct = similarity == 100;

        UserWordMastery masteryRow = masteryRepository.findByUserAndWord(userId, wordId)
                .orElseGet(() -> {
                    UserWordMastery m = new UserWordMastery();
                    m.setUser(userId);
                    m.setWord(wordId);
                    return m;
                });

        int before = masteryRow.getMastery();
        int delta = correct
                ? masteryCalculator.calculateGain(hintsUsed)
                : -masteryCalculator.calculateLoss(similarity);
        int after = masteryCalculator.applyDelta(before, delta);

        masteryRow.setMastery(after);
        masteryRow.setAttempts(masteryRow.getAttempts() + 1);
        if (correct) {
            masteryRow.setCorrectCount(masteryRow.getCorrectCount() + 1);
        }
        masteryRow.setHintsUsedTotal(masteryRow.getHintsUsedTotal() + hintsUsed.size());
        masteryRow.setLastStudied(Instant.now().toString());
        masteryRepository.save(masteryRow);

        return new AnswerResponse(correct, similarity, word.getWord(), before, after);
    }

    private HintType parseHintKey(String key) {
        try {
            return HintType.fromKey(key);
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_HINT_KEY", "Unknown hint key: " + key);
        }
    }
}
