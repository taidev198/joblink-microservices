package com.joblink.vocabulary.dto.response;

import com.joblink.vocabulary.model.entity.UserVocabulary;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserVocabularyResponse {
    private Long id;
    private Long userId;
    private WordResponse word;
    private UserVocabulary.LearningStatus status;
    private Integer reviewCount;
    private Integer correctCount;
    private Integer incorrectCount;
    private LocalDateTime lastReviewedAt;
    private LocalDateTime nextReviewAt;
    private Double masteryScore;
    
    // SM-2 Algorithm fields
    private Double easinessFactor;
    private Integer intervalDays;
    private Integer repetitions;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

