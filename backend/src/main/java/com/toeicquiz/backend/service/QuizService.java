package com.toeicquiz.backend.service;

import com.toeicquiz.backend.domain.HintType;
import com.toeicquiz.backend.domain.UserWordMastery;
import com.toeicquiz.backend.domain.Word;
import com.toeicquiz.backend.dto.AnswerResponse;
import com.toeicquiz.backend.dto.LevelReviewSummaryDto;
import com.toeicquiz.backend.repository.UserWordMasteryRepository;
import com.toeicquiz.backend.repository.WordRepository;
import io.github.openspacedrepetition.Rating;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class QuizService {

    private static final int WORDS_PER_SESSION = 10;

    private final WordRepository wordRepository;
    private final UserWordMasteryRepository masteryRepository;
    private final SimilarityCalculator similarityCalculator;
    private final MasteryCalculator masteryCalculator;
    private final FsrsScheduler fsrsScheduler;

    public QuizService(WordRepository wordRepository,
                        UserWordMasteryRepository masteryRepository,
                        SimilarityCalculator similarityCalculator,
                        MasteryCalculator masteryCalculator,
                        FsrsScheduler fsrsScheduler) {
        this.wordRepository = wordRepository;
        this.masteryRepository = masteryRepository;
        this.similarityCalculator = similarityCalculator;
        this.masteryCalculator = masteryCalculator;
        this.fsrsScheduler = fsrsScheduler;
    }

    public List<Word> getWordsForLevel(Long userId, int level) {
        List<Word> words = wordRepository.findByLevel(level);
        Map<Long, UserWordMastery> masteryByWordId = masteryRepository.findByUser(userId).stream()
                .collect(Collectors.toMap(UserWordMastery::getWord, Function.identity()));

        Instant now = Instant.now();
        List<Word> due = new ArrayList<>();
        List<Word> notDue = new ArrayList<>();
        for (Word word : words) {
            if (fsrsScheduler.isDue(masteryByWordId.get(word.getId()), now)) {
                due.add(word);
            } else {
                notDue.add(word);
            }
        }
        Collections.shuffle(due);
        Collections.shuffle(notDue);

        List<Word> selected = new ArrayList<>(due.subList(0, Math.min(WORDS_PER_SESSION, due.size())));
        if (selected.size() < WORDS_PER_SESSION) {
            int remaining = WORDS_PER_SESSION - selected.size();
            selected.addAll(notDue.subList(0, Math.min(remaining, notDue.size())));
        }
        Collections.shuffle(selected);
        return selected;
    }

    public List<LevelReviewSummaryDto> getReviewSummary(Long userId) {
        Map<Long, UserWordMastery> masteryByWordId = masteryRepository.findByUser(userId).stream()
                .collect(Collectors.toMap(UserWordMastery::getWord, Function.identity()));
        Instant now = Instant.now();

        return wordRepository.findAll().stream()
                .collect(Collectors.groupingBy(Word::getLevel))
                .entrySet().stream()
                .map(entry -> {
                    int newCount = 0;
                    int learningCount = 0;
                    int reviewCount = 0;
                    for (Word word : entry.getValue()) {
                        switch (fsrsScheduler.categorize(masteryByWordId.get(word.getId()), now)) {
                            case NEW -> newCount++;
                            case LEARNING -> learningCount++;
                            case REVIEW -> reviewCount++;
                            case NOT_DUE -> { }
                        }
                    }
                    return new LevelReviewSummaryDto(entry.getKey(), newCount, learningCount, reviewCount);
                })
                .sorted(Comparator.comparingInt(LevelReviewSummaryDto::getLevel))
                .toList();
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

        Rating rating = fsrsScheduler.review(masteryRow, wordId, correct, after);

        masteryRow.setMastery(after);
        masteryRow.setAttempts(masteryRow.getAttempts() + 1);
        if (correct) {
            masteryRow.setCorrectCount(masteryRow.getCorrectCount() + 1);
        }
        masteryRow.setHintsUsedTotal(masteryRow.getHintsUsedTotal() + hintsUsed.size());
        masteryRow.setLastStudied(Instant.now().toString());
        masteryRepository.save(masteryRow);

        return new AnswerResponse(correct, similarity, word.getWord(), rating.name(), masteryRow.getDueAt());
    }

    private HintType parseHintKey(String key) {
        try {
            return HintType.fromKey(key);
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_HINT_KEY", "Unknown hint key: " + key);
        }
    }
}
