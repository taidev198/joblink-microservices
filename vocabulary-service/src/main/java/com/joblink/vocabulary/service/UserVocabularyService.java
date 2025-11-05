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
    
    @Transactional
    public ApiResponse<UserVocabularyResponse> updateLearningStatus(
            Long userId, Long wordId, UserVocabulary.LearningStatus status, Boolean isCorrect) {
        UserVocabulary userVocabulary = userVocabularyRepository.findByUserIdAndWordId(userId, wordId)
                .orElseThrow(() -> new RuntimeException("Word not found in user vocabulary"));
        
        userVocabulary.setStatus(status);
        userVocabulary.setReviewCount(userVocabulary.getReviewCount() + 1);
        userVocabulary.setLastReviewedAt(LocalDateTime.now());
        
        if (isCorrect != null) {
            if (isCorrect) {
                userVocabulary.setCorrectCount(userVocabulary.getCorrectCount() + 1);
            } else {
                userVocabulary.setIncorrectCount(userVocabulary.getIncorrectCount() + 1);
            }
        }
        
        // Calculate mastery score (0.0 to 1.0)
        int totalAttempts = userVocabulary.getCorrectCount() + userVocabulary.getIncorrectCount();
        if (totalAttempts > 0) {
            double masteryScore = (double) userVocabulary.getCorrectCount() / totalAttempts;
            userVocabulary.setMasteryScore(masteryScore);
            
            if (masteryScore >= 0.8 && userVocabulary.getReviewCount() >= 5) {
                userVocabulary.setStatus(UserVocabulary.LearningStatus.MASTERED);
            }
        }
        
        // Set next review date (spaced repetition: 1 day, 3 days, 7 days, etc.)
        LocalDateTime nextReview = calculateNextReviewDate(userVocabulary.getReviewCount());
        userVocabulary.setNextReviewAt(nextReview);
        
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
    
    private LocalDateTime calculateNextReviewDate(int reviewCount) {
        // Spaced repetition algorithm
        int daysToAdd = switch (reviewCount) {
            case 1 -> 1;
            case 2 -> 3;
            case 3 -> 7;
            case 4 -> 14;
            default -> 30;
        };
        return LocalDateTime.now().plusDays(daysToAdd);
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
                .createdAt(userVocabulary.getCreatedAt())
                .updatedAt(userVocabulary.getUpdatedAt())
                .build();
    }
}

