package com.joblink.vocabulary.controller;

import com.joblink.vocabulary.dto.request.WordRequest;
import com.joblink.vocabulary.dto.response.ApiResponse;
import com.joblink.vocabulary.dto.response.WordResponse;
import com.joblink.vocabulary.model.entity.Word;
import com.joblink.vocabulary.service.WordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/vocabulary/words")
@RequiredArgsConstructor
public class WordController {
    
    private final WordService wordService;
    
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
            @PathVariable Word.WordLevel level,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return wordService.getWordsByLevel(level, pageable);
    }
    
    @GetMapping("/category/{category}")
    public ApiResponse<Page<WordResponse>> getWordsByCategory(
            @PathVariable Word.WordCategory category,
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
}

