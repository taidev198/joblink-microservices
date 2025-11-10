package com.joblink.vocabulary.service;

import com.joblink.vocabulary.dto.response.ApiResponse;
import com.joblink.vocabulary.dto.response.UserVocabularyResponse;
import com.joblink.vocabulary.mapper.WordMapper;
import com.joblink.vocabulary.model.entity.UserVocabulary;
import com.joblink.vocabulary.model.entity.Word;
import com.joblink.vocabulary.repository.UserVocabularyRepository;
import com.joblink.vocabulary.repository.WordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserVocabularyService {
    
    private final UserVocabularyRepository userVocabularyRepository;
    private final WordRepository wordRepository;
    private final WordMapper wordMapper;
    private final SM2SpacedRepetitionService sm2Service;
    
    @Transactional
    public ApiResponse<UserVocabularyResponse> addWordToUserVocabulary(Long userId, Long wordId) {
        Word word = wordRepository.findById(wordId)
                .orElseThrow(() -> new RuntimeException("Word not found with id: " + wordId));
        
        if (userVocabularyRepository.findByUserIdAndWordId(userId, wordId).isPresent()) {
            return ApiResponse.error("Word already in user vocabulary");
        }
        
        UserVocabulary userVocabulary = UserVocabulary.builder()
                .userId(userId)
                .word(word)
                .status(UserVocabulary.LearningStatus.NOT_STARTED)
                .reviewCount(0)
                .correctCount(0)
                .incorrectCount(0)
                .masteryScore(0.0)
                .build();
        
        // Initialize SM-2 fields
        sm2Service.initializeSM2Fields(userVocabulary);
        
        UserVocabulary saved = userVocabularyRepository.save(userVocabulary);
        return ApiResponse.success("Word added to vocabulary", toResponse(saved));
    }
    
    public ApiResponse<Page<UserVocabularyResponse>> getUserVocabulary(Long userId, Pageable pageable) {
        Page<UserVocabulary> userVocabularies = userVocabularyRepository.findByUserId(userId, pageable);
        Page<UserVocabularyResponse> responses = userVocabularies.map(this::toResponse);
        return ApiResponse.success(responses);
    }
    
    public ApiResponse<Page<UserVocabularyResponse>> getUserVocabularyByStatus(
            Long userId, UserVocabulary.LearningStatus status, Pageable pageable) {
        Page<UserVocabulary> userVocabularies = userVocabularyRepository.findByUserIdAndStatus(userId, status, pageable);
        Page<UserVocabularyResponse> responses = userVocabularies.map(this::toResponse);
        return ApiResponse.success(responses);
    }
    
    public ApiResponse<List<UserVocabularyResponse>> getWordsDueForReview(Long userId) {
        List<UserVocabulary> wordsDue = userVocabularyRepository.findWordsDueForReview(userId, LocalDateTime.now());
        List<UserVocabularyResponse> responses = wordsDue.stream()
                .map(this::toResponse)
                .toList();
        return ApiResponse.success(responses);
    }
    
    /**
     * Update learning status using SM-2 algorithm
     * 
     * @param userId User ID
     * @param wordId Word ID
     * @param quality Quality of response (0-5) for SM-2 algorithm
     *                5: Perfect response
     *                4: Correct response after hesitation
     *                3: Correct response with serious difficulty
     *                2: Incorrect response; correct one remembered
     *                1: Incorrect response; correct one seemed familiar
     *                0: Complete blackout
     * @return Updated user vocabulary response
     */
    @Transactional
    public ApiResponse<UserVocabularyResponse> updateLearningStatusWithSM2(
            Long userId, Long wordId, Integer quality) {
        UserVocabulary userVocabulary = userVocabularyRepository.findByUserIdAndWordId(userId, wordId)
                .orElseThrow(() -> new RuntimeException("Word not found in user vocabulary"));
        
        // Use SM-2 algorithm to calculate next review
        sm2Service.calculateNextReview(userVocabulary, quality);
        
        UserVocabulary updated = userVocabularyRepository.save(userVocabulary);
        return ApiResponse.success("Learning status updated with SM-2 algorithm", toResponse(updated));
    }
    
    /**
     * Legacy method for backward compatibility
     * Maps isCorrect boolean to quality (5 if correct, 2 if incorrect)
     */
    @Transactional
    public ApiResponse<UserVocabularyResponse> updateLearningStatus(
            Long userId, Long wordId, UserVocabulary.LearningStatus status, Boolean isCorrect) {
        UserVocabulary userVocabulary = userVocabularyRepository.findByUserIdAndWordId(userId, wordId)
                .orElseThrow(() -> new RuntimeException("Word not found in user vocabulary"));
        
        // Map boolean to quality for SM-2
        int quality = (isCorrect != null && isCorrect) ? 5 : 2;
        
        // Use SM-2 algorithm
        sm2Service.calculateNextReview(userVocabulary, quality);
        
        // Override status if explicitly provided
        if (status != null) {
            userVocabulary.setStatus(status);
        }
        
        UserVocabulary updated = userVocabularyRepository.save(userVocabulary);
        return ApiResponse.success("Learning status updated", toResponse(updated));
    }
    
    public ApiResponse<Map<String, Object>> getUserVocabularyStats(Long userId) {
        Long totalWords = userVocabularyRepository.countByUserId(userId);
        Long notStarted = userVocabularyRepository.countByUserIdAndStatus(userId, UserVocabulary.LearningStatus.NOT_STARTED);
        Long learning = userVocabularyRepository.countByUserIdAndStatus(userId, UserVocabulary.LearningStatus.LEARNING);
        Long reviewing = userVocabularyRepository.countByUserIdAndStatus(userId, UserVocabulary.LearningStatus.REVIEWING);
        Long mastered = userVocabularyRepository.countByUserIdAndStatus(userId, UserVocabulary.LearningStatus.MASTERED);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalWords", totalWords);
        stats.put("notStarted", notStarted);
        stats.put("learning", learning);
        stats.put("reviewing", reviewing);
        stats.put("mastered", mastered);
        
        return ApiResponse.success(stats);
    }
    
    /**
     * Get words reviewed today
     */
    public ApiResponse<List<UserVocabularyResponse>> getWordsReviewedToday(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = now.toLocalDate().atTime(23, 59, 59);
        List<UserVocabulary> wordsReviewed = userVocabularyRepository.findWordsReviewedToday(userId, startOfDay, endOfDay);
        List<UserVocabularyResponse> responses = wordsReviewed.stream()
                .map(this::toResponse)
                .toList();
        return ApiResponse.success(responses);
    }
    
    /**
     * Get new words learned today (first review with quality >= 3)
     */
    public ApiResponse<List<UserVocabularyResponse>> getNewWordsLearnedToday(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = now.toLocalDate().atTime(23, 59, 59);
        List<UserVocabulary> newWords = userVocabularyRepository.findNewWordsLearnedToday(userId, startOfDay, endOfDay);
        List<UserVocabularyResponse> responses = newWords.stream()
                .map(this::toResponse)
                .toList();
        return ApiResponse.success(responses);
    }
    
    /**
     * Get daily progress from database
     */
    public ApiResponse<Map<String, Object>> getDailyProgress(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = now.toLocalDate().atTime(23, 59, 59);
        
        // Get words reviewed today (using date range)
        List<UserVocabulary> wordsReviewedToday = userVocabularyRepository.findWordsReviewedToday(userId, startOfDay, endOfDay);
        int wordsReviewed = wordsReviewedToday.size();
        
        // Count words learned today (quality >= 3)
        int wordsLearned = (int) wordsReviewedToday.stream()
                .filter(uv -> uv.getCorrectCount() > 0 || uv.getRepetitions() > 0)
                .count();
        
        // Count new words learned today
        Long newWordsLearnedToday = userVocabularyRepository.countNewWordsLearnedToday(userId, startOfDay, endOfDay);
        
        // Get words due for review
        List<UserVocabulary> wordsDue = userVocabularyRepository.findWordsDueForReview(userId, now);
        
        Map<String, Object> progress = new HashMap<>();
        progress.put("wordsReviewed", wordsReviewed);
        progress.put("wordsLearned", wordsLearned);
        progress.put("newWordsLearnedToday", newWordsLearnedToday.intValue());
        progress.put("wordsDueForReview", wordsDue.size());
        progress.put("hasActiveSession", wordsReviewed > 0 && wordsDue.size() > 0);
        
        return ApiResponse.success(progress);
    }
    
    
    private UserVocabularyResponse toResponse(UserVocabulary userVocabulary) {
        return UserVocabularyResponse.builder()
                .id(userVocabulary.getId())
                .userId(userVocabulary.getUserId())
                .word(wordMapper.toResponse(userVocabulary.getWord()))
                .status(userVocabulary.getStatus())
                .reviewCount(userVocabulary.getReviewCount())
                .correctCount(userVocabulary.getCorrectCount())
                .incorrectCount(userVocabulary.getIncorrectCount())
                .lastReviewedAt(userVocabulary.getLastReviewedAt())
                .nextReviewAt(userVocabulary.getNextReviewAt())
                .masteryScore(userVocabulary.getMasteryScore())
                .easinessFactor(userVocabulary.getEasinessFactor())
                .intervalDays(userVocabulary.getIntervalDays())
                .repetitions(userVocabulary.getRepetitions())
                .createdAt(userVocabulary.getCreatedAt())
                .updatedAt(userVocabulary.getUpdatedAt())
                .build();
    }
}

