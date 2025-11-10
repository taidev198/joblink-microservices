package com.joblink.vocabulary.service;

import com.joblink.vocabulary.dto.response.ApiResponse;
import com.joblink.vocabulary.dto.response.UserVocabularyResponse;
import com.joblink.vocabulary.mapper.WordMapper;
import com.joblink.vocabulary.model.entity.UserVocabulary;
import com.joblink.vocabulary.model.entity.Word;
import com.joblink.vocabulary.repository.UserVocabularyRepository;
import com.joblink.vocabulary.repository.WordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
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
        // Ensure all words are scheduled before getting due words
        scheduleUnscheduledWords(userId);
        
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
     * Schedule all words that don't have a nextReviewAt set
     * This ensures all learned words are properly scheduled for spaced repetition
     */
    @Transactional
    public void scheduleUnscheduledWords(Long userId) {
        List<UserVocabulary> allWords = userVocabularyRepository.findByUserId(userId, 
                org.springframework.data.domain.PageRequest.of(0, 10000)).getContent();
        
        LocalDateTime now = LocalDateTime.now();
        int scheduledCount = 0;
        
        for (UserVocabulary uv : allWords) {
            if (uv.getNextReviewAt() == null) {
                // If word has never been reviewed, set it to be due now
                if (uv.getRepetitions() == 0 && uv.getReviewCount() == 0) {
                    uv.setNextReviewAt(now);
                } else if (uv.getLastReviewedAt() != null && uv.getIntervalDays() != null) {
                    // If word was reviewed before but doesn't have nextReviewAt, calculate it
                    uv.setNextReviewAt(uv.getLastReviewedAt().plusDays(uv.getIntervalDays()));
                } else {
                    // Default: make it due now
                    uv.setNextReviewAt(now);
                }
                userVocabularyRepository.save(uv);
                scheduledCount++;
            }
        }
        
        if (scheduledCount > 0) {
            log.info("Scheduled {} words for user {}", scheduledCount, userId);
        }
    }
    
    /**
     * Get daily progress from database
     */
    public ApiResponse<Map<String, Object>> getDailyProgress(Long userId) {
        // Ensure all words are scheduled before getting progress
        scheduleUnscheduledWords(userId);
        
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
    
    /**
     * Get historical progress data for line graph
     * Returns daily statistics for the last N days
     */
    public ApiResponse<Map<String, Object>> getHistoricalProgress(Long userId, int days) {
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusDays(days);
        
        // Get all words reviewed in the date range
        List<UserVocabulary> wordsReviewed = userVocabularyRepository.findWordsReviewedInRange(userId, startDate, endDate);
        
        // Group by date
        Map<String, Map<String, Integer>> dailyStats = new HashMap<>();
        
        for (UserVocabulary uv : wordsReviewed) {
            if (uv.getLastReviewedAt() != null) {
                String dateKey = uv.getLastReviewedAt().toLocalDate().toString();
                dailyStats.putIfAbsent(dateKey, new HashMap<>());
                Map<String, Integer> dayStats = dailyStats.get(dateKey);
                
                // Count reviews
                dayStats.put("reviews", dayStats.getOrDefault("reviews", 0) + 1);
                
                // Count new words learned (first review with quality >= 3)
                if (uv.getReviewCount() == 1 && uv.getRepetitions() > 0) {
                    dayStats.put("newWords", dayStats.getOrDefault("newWords", 0) + 1);
                }
                
                // Count mastered words (status = MASTERED)
                if (uv.getStatus() == UserVocabulary.LearningStatus.MASTERED) {
                    dayStats.put("mastered", dayStats.getOrDefault("mastered", 0) + 1);
                }
            }
        }
        
        // Create data points for graph (fill in missing days with 0)
        List<Map<String, Object>> dataPoints = new java.util.ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            LocalDateTime date = endDate.minusDays(i);
            String dateKey = date.toLocalDate().toString();
            Map<String, Integer> dayStats = dailyStats.getOrDefault(dateKey, new HashMap<>());
            
            Map<String, Object> point = new HashMap<>();
            point.put("date", dateKey);
            point.put("reviews", dayStats.getOrDefault("reviews", 0));
            point.put("newWords", dayStats.getOrDefault("newWords", 0));
            point.put("mastered", dayStats.getOrDefault("mastered", 0));
            dataPoints.add(point);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("dataPoints", dataPoints);
        result.put("totalDays", days);
        result.put("startDate", startDate.toLocalDate().toString());
        result.put("endDate", endDate.toLocalDate().toString());
        
        return ApiResponse.success(result);
    }
    
    /**
     * Get scheduler data - words scheduled for review in the next N days
     */
    public ApiResponse<Map<String, Object>> getSchedulerData(Long userId, int days) {
        LocalDateTime startDate = LocalDateTime.now();
        LocalDateTime endDate = startDate.plusDays(days);
        
        // Get words scheduled for review in the date range
        List<UserVocabulary> scheduledWords = userVocabularyRepository.findWordsScheduledInRange(userId, startDate, endDate);
        
        // Group by date
        Map<String, List<UserVocabularyResponse>> scheduleByDate = new HashMap<>();
        
        for (UserVocabulary uv : scheduledWords) {
            if (uv.getNextReviewAt() != null) {
                String dateKey = uv.getNextReviewAt().toLocalDate().toString();
                scheduleByDate.putIfAbsent(dateKey, new java.util.ArrayList<>());
                scheduleByDate.get(dateKey).add(toResponse(uv));
            }
        }
        
        // Create schedule data points
        List<Map<String, Object>> schedulePoints = new java.util.ArrayList<>();
        for (int i = 0; i < days; i++) {
            LocalDateTime date = startDate.plusDays(i);
            String dateKey = date.toLocalDate().toString();
            List<UserVocabularyResponse> words = scheduleByDate.getOrDefault(dateKey, new java.util.ArrayList<>());
            
            Map<String, Object> point = new HashMap<>();
            point.put("date", dateKey);
            point.put("wordCount", words.size());
            point.put("words", words);
            schedulePoints.add(point);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("schedule", schedulePoints);
        result.put("totalDays", days);
        result.put("startDate", startDate.toLocalDate().toString());
        result.put("endDate", endDate.toLocalDate().toString());
        
        return ApiResponse.success(result);
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

