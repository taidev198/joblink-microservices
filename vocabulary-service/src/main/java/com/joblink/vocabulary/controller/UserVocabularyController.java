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
@RequestMapping("/vocabulary/users/{userId}/vocabularies")
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
    
    /**
     * Update learning status using SM-2 algorithm with quality parameter
     * 
     * Quality scale (0-5):
     * - 5: Perfect response
     * - 4: Correct response after hesitation
     * - 3: Correct response with serious difficulty
     * - 2: Incorrect response; correct one remembered
     * - 1: Incorrect response; correct one seemed familiar
     * - 0: Complete blackout
     */
    @PutMapping("/words/{wordId}/review")
    public ApiResponse<UserVocabularyResponse> reviewWordWithSM2(
            @PathVariable Long userId,
            @PathVariable Long wordId,
            @RequestParam Integer quality) {
        if (quality < 0 || quality > 5) {
            return ApiResponse.error("Quality must be between 0 and 5");
        }
        return userVocabularyService.updateLearningStatusWithSM2(userId, wordId, quality);
    }
    
    /**
     * Legacy endpoint for backward compatibility
     * Maps isCorrect boolean to quality (5 if correct, 2 if incorrect)
     */
    @PutMapping("/words/{wordId}/status")
    public ApiResponse<UserVocabularyResponse> updateLearningStatus(
            @PathVariable Long userId,
            @PathVariable Long wordId,
            @RequestParam(required = false) UserVocabulary.LearningStatus status,
            @RequestParam(required = false) Boolean isCorrect) {
        return userVocabularyService.updateLearningStatus(userId, wordId, status, isCorrect);
    }
    
    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> getUserVocabularyStats(@PathVariable Long userId) {
        return userVocabularyService.getUserVocabularyStats(userId);
    }
    
    /**
     * Get words reviewed today
     */
    @GetMapping("/reviewed-today")
    public ApiResponse<List<UserVocabularyResponse>> getWordsReviewedToday(@PathVariable Long userId) {
        return userVocabularyService.getWordsReviewedToday(userId);
    }
    
    /**
     * Get new words learned today
     */
    @GetMapping("/learned-today")
    public ApiResponse<List<UserVocabularyResponse>> getNewWordsLearnedToday(@PathVariable Long userId) {
        return userVocabularyService.getNewWordsLearnedToday(userId);
    }
    
    /**
     * Get daily progress from database
     */
    @GetMapping("/daily-progress")
    public ApiResponse<Map<String, Object>> getDailyProgress(@PathVariable Long userId) {
        return userVocabularyService.getDailyProgress(userId);
    }
}

