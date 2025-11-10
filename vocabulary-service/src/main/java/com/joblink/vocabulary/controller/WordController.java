package com.joblink.vocabulary.controller;

import com.joblink.vocabulary.dto.request.WordRequest;
import com.joblink.vocabulary.dto.response.ApiResponse;
import com.joblink.vocabulary.dto.response.WordResponse;
import com.joblink.vocabulary.model.entity.Word;
import com.joblink.vocabulary.model.entity.WordCategory;
import com.joblink.vocabulary.model.entity.WordLevel;
import com.joblink.vocabulary.service.WordService;
import com.joblink.vocabulary.service.ContextVocabService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/vocabulary/words")
@RequiredArgsConstructor
public class WordController {
    
    private final WordService wordService;
    private final ContextVocabService contextVocabService;
    
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<WordResponse> createWord(@Valid @RequestBody WordRequest request) {
        return wordService.createWord(request);
    }
    
    @GetMapping("/{id}")
    public ApiResponse<WordResponse> getWordById(@PathVariable Long id) {
        return wordService.getWordById(id);
    }
    
    @GetMapping
    public ApiResponse<Page<WordResponse>> getAllWords(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "ASC") Sort.Direction sortDir) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDir, sortBy));
        return wordService.getAllWords(pageable);
    }
    
    @GetMapping("/level/{level}")
    public ApiResponse<Page<WordResponse>> getWordsByLevel(
            @PathVariable WordLevel level,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return wordService.getWordsByWordLevel(level, pageable);
    }
    
    @GetMapping("/category/{category}")
    public ApiResponse<Page<WordResponse>> getWordsByCategory(
            @PathVariable WordCategory category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return wordService.getWordsByCategory(category, pageable);
    }
    
    @GetMapping("/search")
    public ApiResponse<Page<WordResponse>> searchWords(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return wordService.searchWords(keyword, pageable);
    }
    
    // Allow GET for quick testing in a browser or Postman
    @GetMapping("/crawl-audio")
    public ApiResponse<Void> crawlAudioGet(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "200") int size) {
        return crawlAudio(page, size);
    }
    
    @PostMapping("/crawl-audio")
    public ApiResponse<Void> crawlAudio(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "200") int size) {
        wordService.crawlAudioForWords(page, size);

        return ApiResponse.success("Audio crawling completed", null);
    }

    @GetMapping("/extract-pdf")
    public ApiResponse<Void> extractPDFGet(
            @RequestParam String pdfPath,
            @RequestParam(defaultValue = "true") boolean fetchAudio) {
        return extractPDF(pdfPath, fetchAudio);
    }

    @PostMapping("/extract-pdf")
    public ApiResponse<Void> extractPDF(
            @RequestParam String pdfPath,
            @RequestParam(defaultValue = "true") boolean fetchAudio) {
        wordService.extractAndSaveWordsFromPDF(pdfPath, fetchAudio);
        return ApiResponse.success("PDF extraction and word saving completed", null);
    }

    @GetMapping("/crawl-cambridge")
    public ApiResponse<WordResponse> crawlCambridgeGet(@RequestParam String word) {
        return crawlCambridge(word);
    }

    @PostMapping("/crawl-cambridge")
    public ApiResponse<WordResponse> crawlCambridge(@RequestParam String word) {
        Word savedWord = wordService.fetchAndSaveWordFromCambridge(word);
        return ApiResponse.success("Word fetched from Cambridge Dictionary successfully", 
                wordService.getWordById(savedWord.getId()).getData());
    }

    @PostMapping("/crawl-cambridge/batch")
    public ApiResponse<Void> batchCrawlCambridge(
            @RequestBody java.util.List<String> words,
            @RequestParam(defaultValue = "false") boolean fetchAudio) {
        wordService.batchFetchWordsFromCambridge(words, fetchAudio);
        return ApiResponse.success("Batch fetch from Cambridge Dictionary completed", null);
    }

    @GetMapping("/process-oxford-json")
    public ApiResponse<Void> processOxfordJsonGet(
            @RequestParam String jsonFilePath,
            @RequestParam(defaultValue = "0") int startIndex,
            @RequestParam(defaultValue = "50") int batchSize,
            @RequestParam(defaultValue = "true") boolean fetchAudio) {
        return processOxfordJson(jsonFilePath, startIndex, batchSize, fetchAudio);
    }

    @PostMapping("/process-oxford-json")
    public ApiResponse<Void> processOxfordJson(
            @RequestParam String jsonFilePath,
            @RequestParam(defaultValue = "0") int startIndex,
            @RequestParam(defaultValue = "50") int batchSize,
            @RequestParam(defaultValue = "true") boolean fetchAudio) {
        wordService.processOxfordWordsFromJson(jsonFilePath, startIndex, batchSize, fetchAudio);
        return ApiResponse.success("Oxford words processing completed", null);
    }
    
    @PutMapping("/{id}")
    public ApiResponse<WordResponse> updateWord(
            @PathVariable Long id,
            @Valid @RequestBody WordRequest request) {
        return wordService.updateWord(id, request);
    }
    
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteWord(@PathVariable Long id) {
        return wordService.deleteWord(id);
    }
    
    /**
     * Generate vocabulary from image using Vision API + GPT
     */
    @PostMapping("/context/generate")
    public ApiResponse<Map<String, Object>> generateVocabFromImage(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile imageFile,
            @RequestParam("userId") Long userId) {
        return contextVocabService.generateVocabFromImage(imageFile, userId);
    }
    
    /**
     * Save context-generated word to vocabulary
     */
    @PostMapping("/context/save")
    public ApiResponse<Map<String, Object>> saveContextWord(
            @RequestBody Map<String, String> request) {
        Long userId = Long.parseLong(request.get("userId"));
        String word = request.get("word");
        String partOfSpeech = request.get("partOfSpeech");
        String level = request.get("level");
        String example = request.get("example");
        String context = request.get("context");
        
        return contextVocabService.saveContextWord(userId, word, partOfSpeech, level, example, context);
    }
}

