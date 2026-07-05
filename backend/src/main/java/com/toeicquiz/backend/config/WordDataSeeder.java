package com.toeicquiz.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.toeicquiz.backend.domain.Word;
import com.toeicquiz.backend.repository.WordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;

@Component
public class WordDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(WordDataSeeder.class);

    private final WordRepository wordRepository;
    private final String seedPath;

    public WordDataSeeder(WordRepository wordRepository,
                           @Value("${app.words-seed-path}") String seedPath) {
        this.wordRepository = wordRepository;
        this.seedPath = seedPath;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (wordRepository.count() > 0) {
            log.info("Words table already has {} rows, skipping seed.", wordRepository.count());
            return;
        }

        File file = new File(seedPath);
        if (!file.exists()) {
            log.warn("Word seed file not found at {}, skipping seed.", file.getAbsolutePath());
            return;
        }

        ObjectMapper mapper = new ObjectMapper();
        List<WordSeedEntry> entries = mapper.readValue(file, mapper.getTypeFactory()
                .constructCollectionType(List.class, WordSeedEntry.class));

        for (WordSeedEntry entry : entries) {
            Word word = new Word();
            word.setWord(entry.word);
            word.setLevel(entry.level);
            word.setDefinition(entry.definition);
            word.setExample(entry.example);
            word.setSynonyms(entry.synonyms);
            word.setImageUrl(entry.imageUrl);
            wordRepository.save(word);
        }
        log.info("Seeded {} words from {}", entries.size(), file.getAbsolutePath());
    }
}
