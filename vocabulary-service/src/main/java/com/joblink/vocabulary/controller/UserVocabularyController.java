package com.joblink.vocabulary.controller;

import com.joblink.vocabulary.dto.response.ApiResponse;
import com.joblink.vocabulary.dto.response.UserVocabularyResponse;
import com.joblink.vocabulary.model.entity.UserVocabulary;
import com.joblink.vocabulary.service.UserVocabularyService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vocabulary/users/{userId}/vocabularies")
@RequiredArgsConstructor
public class UserVocabularyController {
    
    private final UserVocabularyService userVocabularyService;
    
    @PostMapping("/words/{wordId}")
    public ApiResponse<UserVocabularyResponse> addWordToVocabulary(
            @PathVariable Long userId,
            @PathVariable Long wordId) {
        return userVocabularyService.addWordToUserVocabulary(userId, wordId);
    }
    
    @GetMapping
    public ApiResponse<Page<UserVocabularyResponse>> getUserVocabulary(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return userVocabularyService.getUserVocabulary(userId, pageable);
    }
    
    @GetMapping("/status/{status}")
    public ApiResponse<Page<UserVocabularyResponse>> getUserVocabularyByStatus(
            @PathVariable Long userId,
            @PathVariable UserVocabulary.LearningStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return userVocabularyService.getUserVocabularyByStatus(userId, status, pageable);
    }
    
    @GetMapping("/due-for-review")
    public ApiResponse<List<UserVocabularyResponse>> getWordsDueForReview(@PathVariable Long userId) {
        return userVocabularyService.getWordsDueForReview(userId);
    }
    
    @PutMapping("/words/{wordId}/status")
    public ApiResponse<UserVocabularyResponse> updateLearningStatus(
            @PathVariable Long userId,
            @PathVariable Long wordId,
            @RequestParam UserVocabulary.LearningStatus status,
            @RequestParam(required = false) Boolean isCorrect) {
        return userVocabularyService.updateLearningStatus(userId, wordId, status, isCorrect);
    }
    
    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> getUserVocabularyStats(@PathVariable Long userId) {
        return userVocabularyService.getUserVocabularyStats(userId);
    }
}

