package com.joblink.vocabulary.service;

import com.joblink.vocabulary.dto.request.LearningProgressRequest;
import com.joblink.vocabulary.dto.response.ApiResponse;
import com.joblink.vocabulary.model.entity.LearningProgress;
import com.joblink.vocabulary.model.entity.UserVocabulary;
import com.joblink.vocabulary.model.entity.Word;
import com.joblink.vocabulary.repository.LearningProgressRepository;
import com.joblink.vocabulary.repository.UserVocabularyRepository;
import com.joblink.vocabulary.repository.WordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LearningProgressService {
    
    private final LearningProgressRepository learningProgressRepository;
    private final UserVocabularyRepository userVocabularyRepository;
    private final WordRepository wordRepository;
    private final UserVocabularyService userVocabularyService;
    
    @Transactional
    public ApiResponse<LearningProgress> recordProgress(Long userId, LearningProgressRequest request) {
        Word word = wordRepository.findById(request.getWordId())
                .orElseThrow(() -> new RuntimeException("Word not found with id: " + request.getWordId()));
        
        LearningProgress progress = LearningProgress.builder()
                .userId(userId)
                .word(word)
                .progressType(request.getProgressType())
                .isCorrect(request.getIsCorrect())
                .timeSpentSeconds(request.getTimeSpentSeconds())
                .notes(request.getNotes())
                .build();
        
        LearningProgress savedProgress = learningProgressRepository.save(progress);
        
        // Ensure word is in user vocabulary (add if doesn't exist)
        if (userVocabularyRepository.findByUserIdAndWordId(userId, request.getWordId()).isEmpty()) {
            userVocabularyService.addWordToUserVocabulary(userId, request.getWordId());
        }
        
        // Update user vocabulary status
        UserVocabulary.LearningStatus newStatus = request.getIsCorrect() 
                ? UserVocabulary.LearningStatus.LEARNING 
                : UserVocabulary.LearningStatus.REVIEWING;
        
        userVocabularyService.updateLearningStatus(userId, request.getWordId(), newStatus, request.getIsCorrect());
        
        return ApiResponse.success("Progress recorded", savedProgress);
    }
    
    public ApiResponse<Page<LearningProgress>> getUserProgress(Long userId, Pageable pageable) {
        Page<LearningProgress> progress = learningProgressRepository.findByUserId(userId, pageable);
        return ApiResponse.success(progress);
    }
    
    public ApiResponse<Map<String, Object>> getUserProgressStats(Long userId) {
        Long totalProgress = learningProgressRepository.countByUserIdAndDateRange(
                userId, LocalDateTime.now().minusDays(30), LocalDateTime.now());
        Long correctAnswers = learningProgressRepository.countCorrectAnswers(userId);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalProgress", totalProgress);
        stats.put("correctAnswers", correctAnswers);
        stats.put("accuracyRate", totalProgress > 0 ? (double) correctAnswers / totalProgress : 0.0);
        
        return ApiResponse.success(stats);
    }
}

