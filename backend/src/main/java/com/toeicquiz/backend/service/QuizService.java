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
        List<Word> overdue = new ArrayList<>();
        List<Word> newWords = new ArrayList<>();
        List<Word> notDue = new ArrayList<>();
        for (Word word : words) {
            switch (fsrsScheduler.categorize(masteryByWordId.get(word.getId()), now)) {
                case LEARNING, REVIEW -> overdue.add(word);
                case NEW -> newWords.add(word);
                case NOT_DUE -> notDue.add(word);
            }
        }
        Collections.shuffle(overdue);
        Collections.shuffle(newWords);
        Collections.shuffle(notDue);

        // 復習期限切れの単語を最優先し、次に新規、最後に期限内の単語で埋める。
        List<Word> selected = new ArrayList<>();
        fillUpTo(selected, overdue, WORDS_PER_SESSION);
        fillUpTo(selected, newWords, WORDS_PER_SESSION);
        fillUpTo(selected, notDue, WORDS_PER_SESSION);
        Collections.shuffle(selected);
        return selected;
    }

    private void fillUpTo(List<Word> target, List<Word> source, int limit) {
        int remaining = limit - target.size();
        if (remaining > 0) {
            target.addAll(source.subList(0, Math.min(remaining, source.size())));
        }
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

        double hintPenalty = masteryCalculator.totalPenalty(hintsUsed);
        Rating rating = fsrsScheduler.review(masteryRow, wordId, correct, hintPenalty);

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
