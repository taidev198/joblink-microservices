package com.joblink.vocabulary.service;

import com.joblink.vocabulary.dto.request.WordRequest;
import com.joblink.vocabulary.dto.response.ApiResponse;
import com.joblink.vocabulary.dto.response.WordResponse;
import com.joblink.vocabulary.mapper.WordMapper;
import com.joblink.vocabulary.model.entity.Word;
import com.joblink.vocabulary.repository.WordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.joblink.vocabulary.dto.request.OxfordWordEntry;
import com.joblink.vocabulary.service.PDFExtractionService.WordData;
import org.springframework.data.domain.PageRequest;

@Service
@RequiredArgsConstructor
@Slf4j
public class WordService {
    
    private final WordRepository wordRepository;
    private final WordMapper wordMapper;
    private final TTSService ttsService;
    private final PDFExtractionService pdfExtractionService;
    private final CambridgeDictionaryCrawler cambridgeCrawler;
    private final ObjectMapper objectMapper;
    
    @Transactional
    public ApiResponse<WordResponse> createWord(WordRequest request) {
        // Check if word already exists
        if (wordRepository.findByEnglishWordIgnoreCase(request.getEnglishWord()).isPresent()) {
            return ApiResponse.error("Word already exists");
        }
        
        Word word = Word.builder()
                .englishWord(request.getEnglishWord())
                .meaning(request.getMeaning())
                .pronunciation(request.getPronunciation())
                .exampleSentence(request.getExampleSentence())
                .translation(request.getTranslation())
                .level(request.getLevel())
                .category(request.getCategory())
                .partOfSpeech(request.getPartOfSpeech())
                .synonyms(request.getSynonyms())
                .antonyms(request.getAntonyms())
                .imageUrl(request.getImageUrl())
                .audioUrl(request.getAudioUrl())
                .isActive(true)
                .build();
        
        Word savedWord = wordRepository.save(word);
        return ApiResponse.success("Word created successfully", wordMapper.toResponse(savedWord));
    }
    
    public ApiResponse<WordResponse> getWordById(Long id) {
        Word word = wordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Word not found with id: " + id));
        return ApiResponse.success(wordMapper.toResponse(word));
    }
    
    public ApiResponse<Page<WordResponse>> getAllWords(Pageable pageable) {
        Page<Word> words = wordRepository.findByIsActiveTrue(pageable);
        Page<WordResponse> wordResponses = words.map(wordMapper::toResponse);
        return ApiResponse.success(wordResponses);
    }
    
    public ApiResponse<Page<WordResponse>> getWordsByLevel(Word.WordLevel level, Pageable pageable) {
        Page<Word> words = wordRepository.findByLevel(level, pageable);
        Page<WordResponse> wordResponses = words.map(wordMapper::toResponse);
        return ApiResponse.success(wordResponses);
    }
    
    public ApiResponse<Page<WordResponse>> getWordsByCategory(Word.WordCategory category, Pageable pageable) {
        Page<Word> words = wordRepository.findByCategory(category, pageable);
        Page<WordResponse> wordResponses = words.map(wordMapper::toResponse);
        return ApiResponse.success(wordResponses);
    }
    
    public ApiResponse<Page<WordResponse>> searchWords(String keyword, Pageable pageable) {
        Page<Word> words = wordRepository.searchWords(keyword, pageable);
        Page<WordResponse> wordResponses = words.map(wordMapper::toResponse);
        return ApiResponse.success(wordResponses);
    }
    
    @Transactional
    public ApiResponse<WordResponse> updateWord(Long id, WordRequest request) {
        Word word = wordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Word not found with id: " + id));
        
        word.setEnglishWord(request.getEnglishWord());
        word.setMeaning(request.getMeaning());
        word.setPronunciation(request.getPronunciation());
        word.setExampleSentence(request.getExampleSentence());
        word.setTranslation(request.getTranslation());
        word.setLevel(request.getLevel());
        word.setCategory(request.getCategory());
        word.setPartOfSpeech(request.getPartOfSpeech());
        word.setSynonyms(request.getSynonyms());
        word.setAntonyms(request.getAntonyms());
        word.setImageUrl(request.getImageUrl());
        word.setAudioUrl(request.getAudioUrl());
        
        Word updatedWord = wordRepository.save(word);
        return ApiResponse.success("Word updated successfully", wordMapper.toResponse(updatedWord));
    }
    
    @Transactional
    public ApiResponse<Void> deleteWord(Long id) {
        Word word = wordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Word not found with id: " + id));
        word.setIsActive(false);
        wordRepository.save(word);
        return ApiResponse.success("Word deleted successfully", null);
    }
    
    public ApiResponse<List<WordResponse>> getWordsByIds(List<Long> wordIds) {
        List<Word> words = wordRepository.findByIds(wordIds);
        return ApiResponse.success(wordMapper.toResponseList(words));
    }

    @Transactional
    public void crawlAudioForWords(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Word> words = wordRepository.findByIsActiveTrue(pageable);
        words.forEach(word -> {
           if (word.getEnglishAudioUrl() == null) {
               String url = null;
               try {
                   url = ttsService.fetchAndSaveEnglishAudio(word.getEnglishWord());
               } catch (Exception e) {
                   log.error("Failed to fetch English audio for word: {}", word.getEnglishWord(), e);
               }
               if (url != null) {
                   word.setEnglishAudioUrl(url);
               }
           }
            if (word.getVietnameseAudioUrl() == null) {
                String url = null;
                if (!StringUtils.isEmpty(word.getMeaning())) {
                    try {
                        url = ttsService.fetchAndSaveVietnameseAudio(word.getMeaning(), word.getEnglishWord());
                    } catch (Exception e) {
                        log.error("Failed to fetch Vietnamese audio for word: {}", word.getEnglishWord(), e);
                    }
                    if (url != null) {
                        word.setVietnameseAudioUrl(url);
                    }
                }
            }
            wordRepository.save(word);
        });
    }

    @Transactional
    public void extractAndSaveWordsFromPDF(String pdfPath, boolean fetchAudio) {
        try {
            log.info("Starting PDF extraction from: {}", pdfPath);
            
            // Extract words from PDF
            List<WordData> extractedWords = pdfExtractionService.extractWordsFromPDF(pdfPath);
            log.info("Extracted {} words from PDF", extractedWords.size());
            
            int savedCount = 0;
            int skippedCount = 0;
            int audioFetchedCount = 0;
            
            for (WordData wordData : extractedWords) {
                try {
                    // Check if word already exists
                    if (wordRepository.findByEnglishWordIgnoreCase(wordData.getEnglishWord()).isPresent()) {
                        log.debug("Word already exists: {}", wordData.getEnglishWord());
                        skippedCount++;
                        continue;
                    }
                    
                    // Create word entity
                    Word word = Word.builder()
                            .englishWord(wordData.getEnglishWord())
                            .meaning(wordData.getMeaning())
                            .exampleSentence(wordData.getExampleSentence())
                            .translation(wordData.getMeaning()) // Use meaning as translation
                            .level(Word.WordLevel.INTERMEDIATE) // Default level
                            .category(Word.WordCategory.DAILY_LIFE) // Default category
                            .isActive(true)
                            .build();
                    
                    // Save word first
                    Word savedWord = wordRepository.save(word);
                    savedCount++;
                    log.debug("Saved word: {}", savedWord.getEnglishWord());
                    
                    // Fetch audio if requested
                    if (fetchAudio) {
                        try {
                            // Fetch English audio
                            String englishAudioUrl = ttsService.fetchAndSaveEnglishAudio(savedWord.getEnglishWord());
                            if (englishAudioUrl != null) {
                                savedWord.setEnglishAudioUrl(englishAudioUrl);
                                log.debug("Fetched English audio for: {}", savedWord.getEnglishWord());
                            }
                            
                            // Fetch Vietnamese audio
                            if (!StringUtils.isEmpty(savedWord.getMeaning())) {
                                String vietnameseAudioUrl = ttsService.fetchAndSaveVietnameseAudio(
                                        savedWord.getMeaning(), savedWord.getEnglishWord());
                                if (vietnameseAudioUrl != null) {
                                    savedWord.setVietnameseAudioUrl(vietnameseAudioUrl);
                                    log.debug("Fetched Vietnamese audio for: {}", savedWord.getEnglishWord());
                                }
                            }
                            
                            // Save updated word with audio URLs
                            wordRepository.save(savedWord);
                            audioFetchedCount++;
                            
                            // Add small delay to avoid rate limiting
                            Thread.sleep(500);
                        } catch (Exception e) {
                            log.error("Failed to fetch audio for word: {}", savedWord.getEnglishWord(), e);
                        }
                    }
                } catch (Exception e) {
                    log.error("Failed to save word: {}", wordData.getEnglishWord(), e);
                }
            }
            
            log.info("PDF extraction completed. Saved: {}, Skipped: {}, Audio fetched: {}", 
                    savedCount, skippedCount, audioFetchedCount);
        } catch (IOException e) {
            log.error("Failed to extract words from PDF: {}", pdfPath, e);
            throw new RuntimeException("Failed to extract words from PDF", e);
        }
    }

    /**
     * Fetch word information from Cambridge Dictionary and update/create word
     * @param word The English word to fetch information for
     * @return Updated or created Word entity
     */
    @Transactional
    public Word fetchAndSaveWordFromCambridge(String word) {
        log.info("Fetching word information from Cambridge Dictionary for: {}", word);
        
        Map<String, Object> wordInfo = cambridgeCrawler.getWordInfo(word);
        
        if (wordInfo.isEmpty()) {
            log.warn("No information found for word: {}", word);
            throw new RuntimeException("No information found for word: " + word);
        }
        
        // Check if word already exists
        Word existingWord = wordRepository.findByEnglishWordIgnoreCase(word)
                .orElse(null);
        
        Word wordToSave;
        if (existingWord != null) {
            log.info("Updating existing word: {}", word);
            wordToSave = existingWord;
        } else {
            log.info("Creating new word: {}", word);
            wordToSave = Word.builder()
                    .englishWord(word)
                    .isActive(true)
                    .level(Word.WordLevel.INTERMEDIATE)
                    .category(Word.WordCategory.DAILY_LIFE)
                    .build();
        }
        
        // Update fields from Cambridge Dictionary
        if (wordInfo.containsKey("ipa")) {
            wordToSave.setPronunciation((String) wordInfo.get("ipa"));
        }
        
        if (wordInfo.containsKey("definition")) {
            // Use definition as meaning if meaning is not set
            if (existingWord == null || StringUtils.isEmpty(wordToSave.getMeaning())) {
                wordToSave.setMeaning((String) wordInfo.get("definition"));
            }
        }
        
        if (wordInfo.containsKey("vietnameseMeaning")) {
            wordToSave.setMeaning((String) wordInfo.get("vietnameseMeaning"));
        }
        
        // Always update example sentence if available from Cambridge
        if (wordInfo.containsKey("example")) {
            String example = (String) wordInfo.get("example");
            if (example != null && !example.trim().isEmpty()) {
                wordToSave.setExampleSentence(example.trim());
                log.info("Set example sentence from Cambridge: {}", example);
            }
        } else if (wordInfo.containsKey("examples")) {
            @SuppressWarnings("unchecked")
            List<String> examples = (List<String>) wordInfo.get("examples");
            if (examples != null && !examples.isEmpty()) {
                String firstExample = examples.get(0);
                if (firstExample != null && !firstExample.trim().isEmpty()) {
                    wordToSave.setExampleSentence(firstExample.trim());
                    log.info("Set example sentence from Cambridge examples list: {}", firstExample);
                }
            }
        }
        
        if (wordInfo.containsKey("vietnameseExample")) {
            String vietnameseExample = (String) wordInfo.get("vietnameseExample");
            if (vietnameseExample != null && !vietnameseExample.trim().isEmpty()) {
                wordToSave.setTranslation(vietnameseExample.trim());
            }
        } else if (wordInfo.containsKey("vietnameseExamples")) {
            @SuppressWarnings("unchecked")
            List<String> vietnameseExamples = (List<String>) wordInfo.get("vietnameseExamples");
            if (vietnameseExamples != null && !vietnameseExamples.isEmpty()) {
                String firstVietnameseExample = vietnameseExamples.get(0);
                if (firstVietnameseExample != null && !firstVietnameseExample.trim().isEmpty()) {
                    wordToSave.setTranslation(firstVietnameseExample.trim());
                }
            }
        }
        
        Word savedWord = wordRepository.save(wordToSave);
        log.info("Successfully saved word from Cambridge Dictionary: {}", savedWord.getEnglishWord());
        
        return savedWord;
    }

    /**
     * Batch fetch words from Cambridge Dictionary
     * @param words List of words to fetch
     * @param fetchAudio Whether to fetch audio after getting word info
     */
    @Transactional
    public void batchFetchWordsFromCambridge(List<String> words, boolean fetchAudio) {
        log.info("Starting batch fetch from Cambridge Dictionary for {} words", words.size());
        
        int successCount = 0;
        int errorCount = 0;
        
        for (String word : words) {
            try {
                Word savedWord = fetchAndSaveWordFromCambridge(word);
                successCount++;
                
                // Fetch audio if requested
                if (fetchAudio) {
                    try {
                        // Fetch English audio
                        if (savedWord.getEnglishAudioUrl() == null) {
                            String englishAudioUrl = ttsService.fetchAndSaveEnglishAudio(savedWord.getEnglishWord());
                            if (englishAudioUrl != null) {
                                savedWord.setEnglishAudioUrl(englishAudioUrl);
                                wordRepository.save(savedWord);
                            }
                        }
                        
                        // Fetch Vietnamese audio
                        if (savedWord.getVietnameseAudioUrl() == null && !StringUtils.isEmpty(savedWord.getMeaning())) {
                            String vietnameseAudioUrl = ttsService.fetchAndSaveVietnameseAudio(
                                    savedWord.getMeaning(), savedWord.getEnglishWord());
                            if (vietnameseAudioUrl != null) {
                                savedWord.setVietnameseAudioUrl(vietnameseAudioUrl);
                                wordRepository.save(savedWord);
                            }
                        }
                        
                        // Add delay to avoid rate limiting
                        Thread.sleep(1000);
                    } catch (Exception e) {
                        log.error("Failed to fetch audio for word: {}", word, e);
                    }
                }
                
                // Add delay between Cambridge requests to avoid rate limiting
                Thread.sleep(2000);
                
            } catch (Exception e) {
                log.error("Failed to fetch word from Cambridge Dictionary: {}", word, e);
                errorCount++;
            }
        }
        
        log.info("Batch fetch completed. Success: {}, Errors: {}", successCount, errorCount);
    }

    /**
     * Read words from Oxford JSON file and fetch information from Cambridge Dictionary
     * @param jsonFilePath Path to the JSON file
     * @param startIndex Starting index (for batch processing)
     * @param batchSize Number of words to process in this batch
     * @param fetchAudio Whether to fetch and save audio files
     */
    @Transactional
    public void processOxfordWordsFromJson(String jsonFilePath, int startIndex, int batchSize, boolean fetchAudio) {
        try {
            log.info("Reading Oxford words from JSON file: {}", jsonFilePath);
            
            // Read JSON file
            File jsonFile = new File(jsonFilePath);
            if (!jsonFile.exists()) {
                throw new RuntimeException("JSON file not found: " + jsonFilePath);
            }
            
            List<OxfordWordEntry> entries = objectMapper.readValue(
                    jsonFile, 
                    new TypeReference<List<OxfordWordEntry>>() {}
            );
            
            log.info("Found {} words in JSON file", entries.size());
            
            // Process words in batch
            int endIndex = Math.min(startIndex + batchSize, entries.size());
            int successCount = 0;
            int errorCount = 0;
            int skippedCount = 0;
            
            for (int i = startIndex; i < endIndex; i++) {
                OxfordWordEntry entry = entries.get(i);
                String word = entry.getWord() != null ? entry.getWord().replace(".", "").trim() : null;
                
                if (word == null || word.isEmpty()) {
                    log.warn("Skipping entry at index {}: word is empty", i);
                    skippedCount++;
                    continue;
                }
                
                try {
                    log.info("Processing word {}/{}: {}", (i - startIndex + 1), (endIndex - startIndex), word);
                    
                    // Check if word already exists
                    Word existingWord = wordRepository.findByEnglishWordIgnoreCase(word).orElse(null);
                    
                    // Fetch information from Cambridge Dictionary
                    Map<String, Object> cambridgeInfo = cambridgeCrawler.getWordInfo(word);
                    
                    if (cambridgeInfo.isEmpty()) {
                        log.warn("No information found from Cambridge Dictionary for word: {}", word);
                        errorCount++;
                        continue;
                    }
                    
                    Word wordToSave;
                    if (existingWord != null) {
                        log.info("Updating existing word: {}", word);
                        wordToSave = existingWord;
                    } else {
                        log.info("Creating new word: {}", word);
                        wordToSave = Word.builder()
                                .englishWord(word)
                                .isActive(true)
                                .build();
                    }
                    
                    // Map level from JSON (A1, A2, B1, B2, C1, C2) to Word.WordLevel
                    Word.WordLevel wordLevel = mapLevelToWordLevel(entry.getLevel());
                    if (wordLevel != null) {
                        wordToSave.setLevel(wordLevel);
                    } else if (existingWord == null) {
                        wordToSave.setLevel(Word.WordLevel.INTERMEDIATE); // Default
                    }
                    
                    // Set part of speech from JSON type
                    if (entry.getType() != null && !entry.getType().isEmpty()) {
                        wordToSave.setPartOfSpeech(entry.getType());
                    }
                    
                    // Update from Cambridge Dictionary
                    if (cambridgeInfo.containsKey("ipa")) {
                        wordToSave.setPronunciation((String) cambridgeInfo.get("ipa"));
                    }
                    
                    if (cambridgeInfo.containsKey("vietnameseMeaning")) {
                        wordToSave.setMeaning((String) cambridgeInfo.get("vietnameseMeaning"));
                    } else if (cambridgeInfo.containsKey("definition")) {
                        // Use English definition if Vietnamese is not available
                        if (existingWord == null || StringUtils.isEmpty(wordToSave.getMeaning())) {
                            wordToSave.setMeaning((String) cambridgeInfo.get("definition"));
                        }
                    }
                    
                    // Always update example sentence if available from Cambridge
                    if (cambridgeInfo.containsKey("example")) {
                        String example = (String) cambridgeInfo.get("example");
                        if (example != null && !example.trim().isEmpty()) {
                            wordToSave.setExampleSentence(example.trim());
                            log.info("Set example sentence from Cambridge: {}", example);
                        }
                    } else if (cambridgeInfo.containsKey("examples")) {
                        @SuppressWarnings("unchecked")
                        List<String> examples = (List<String>) cambridgeInfo.get("examples");
                        if (examples != null && !examples.isEmpty()) {
                            String firstExample = examples.get(0);
                            if (firstExample != null && !firstExample.trim().isEmpty()) {
                                wordToSave.setExampleSentence(firstExample.trim());
                                log.info("Set example sentence from Cambridge examples list: {}", firstExample);
                            }
                        }
                    }
                    
                    if (cambridgeInfo.containsKey("vietnameseExample")) {
                        wordToSave.setTranslation((String) cambridgeInfo.get("vietnameseExample"));
                    } else if (cambridgeInfo.containsKey("vietnameseExamples")) {
                        @SuppressWarnings("unchecked")
                        List<String> vietnameseExamples = (List<String>) cambridgeInfo.get("vietnameseExamples");
                        if (!vietnameseExamples.isEmpty()) {
                            wordToSave.setTranslation(vietnameseExamples.get(0));
                        }
                    }
                    
                    // Set default category if not set
                    if (existingWord == null) {
                        wordToSave.setCategory(Word.WordCategory.DAILY_LIFE);
                    }
                    
                    // Save word first
                    Word savedWord = wordRepository.save(wordToSave);
                    successCount++;
                    
                    // Fetch and save audio if requested
                    if (fetchAudio) {
                        try {
                            // Fetch English audio from Cambridge
                            if (cambridgeInfo.containsKey("audioUrl")) {
                                String audioUrl = (String) cambridgeInfo.get("audioUrl");
                                if (audioUrl != null && !audioUrl.isEmpty()) {
                                    // Download and save audio file
                                    String savedAudioUrl = downloadAndSaveAudio(audioUrl, word, true);
                                    if (savedAudioUrl != null) {
                                        savedWord.setEnglishAudioUrl(savedAudioUrl);
                                        wordRepository.save(savedWord);
                                        log.info("Saved English audio for word: {}", word);
                                    }
                                }
                            } else {
                                // Fallback to TTS service if Cambridge audio not available
                                if (savedWord.getEnglishAudioUrl() == null) {
                                    String englishAudioUrl = ttsService.fetchAndSaveEnglishAudio(savedWord.getEnglishWord());
                                    if (englishAudioUrl != null) {
                                        savedWord.setEnglishAudioUrl(englishAudioUrl);
                                        wordRepository.save(savedWord);
                                    }
                                }
                            }
                            
                            // Fetch Vietnamese audio
                            if (savedWord.getVietnameseAudioUrl() == null && !StringUtils.isEmpty(savedWord.getMeaning())) {
                                String vietnameseAudioUrl = ttsService.fetchAndSaveVietnameseAudio(
                                        savedWord.getMeaning(), savedWord.getEnglishWord());
                                if (vietnameseAudioUrl != null) {
                                    savedWord.setVietnameseAudioUrl(vietnameseAudioUrl);
                                    wordRepository.save(savedWord);
                                }
                            }
                            
                            // Add delay to avoid rate limiting
                            Thread.sleep(1000);
                        } catch (Exception e) {
                            log.error("Failed to fetch audio for word: {}", word, e);
                        }
                    }
                    
                    // Add delay between Cambridge requests to avoid rate limiting
                    Thread.sleep(2000);
                    
                } catch (Exception e) {
                    log.error("Failed to process word: {} at index {}", word, i, e);
                    errorCount++;
                }
            }
            
            log.info("Processing completed. Success: {}, Errors: {}, Skipped: {}", 
                    successCount, errorCount, skippedCount);
            
        } catch (Exception e) {
            log.error("Failed to process Oxford words from JSON: {}", jsonFilePath, e);
            throw new RuntimeException("Failed to process Oxford words from JSON", e);
        }
    }

    /**
     * Map level string (A1, A2, B1, B2, C1, C2) to Word.WordLevel
     */
    private Word.WordLevel mapLevelToWordLevel(String level) {
        if (level == null || level.isEmpty()) {
            return null;
        }
        
        String upperLevel = level.toUpperCase().trim();
        return switch (upperLevel) {
            case "A1", "A2" -> Word.WordLevel.BEGINNER;
            case "B1", "B2" -> Word.WordLevel.INTERMEDIATE;
            case "C1", "C2" -> Word.WordLevel.ADVANCED;
            default -> null;
        };
    }

    /**
     * Download audio file from URL and save it locally
     */
    private String downloadAndSaveAudio(String audioUrl, String word, boolean isEnglish) {
        try {
            // Use TTS service to download and save audio
            // For now, we'll use the audio URL directly if it's a full URL
            // Otherwise, we can download it using RestTemplate
            
            // If it's a full URL, we can save it directly
            if (audioUrl.startsWith("http://") || audioUrl.startsWith("https://")) {
                // Download the audio file
                RestTemplate restTemplate = new RestTemplate();
                byte[] audioBytes = restTemplate.getForObject(audioUrl, byte[].class);
                
                if (audioBytes != null && audioBytes.length > 0) {
                    // Save to the same location as TTS service
                    String storagePath = System.getProperty("user.home") + "/audio";
                    String subDir = isEnglish ? "english" : "vietnamese";
                    Path dir = Paths.get(storagePath, subDir);
                    Files.createDirectories(dir);
                    
                    String filename = word.replaceAll("\\W+", "_") + (isEnglish ? "_en.mp3" : "_vi.mp3");
                    Path filePath = dir.resolve(filename);
                    Files.write(filePath, audioBytes);
                    
                    log.info("Downloaded and saved audio file: {}", filePath);
                    return "/audio/" + subDir + "/" + filename;
                }
            }
            
            return null;
        } catch (Exception e) {
            log.error("Failed to download and save audio from URL: {}", audioUrl, e);
            return null;
        }
    }
}

