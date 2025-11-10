package com.joblink.vocabulary.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_vocabularies", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "word_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class UserVocabulary {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "word_id", nullable = false)
    private Word word;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private LearningStatus status = LearningStatus.NOT_STARTED;
    
    @Column(name = "review_count")
    @Builder.Default
    private Integer reviewCount = 0;
    
    @Column(name = "correct_count")
    @Builder.Default
    private Integer correctCount = 0;
    
    @Column(name = "incorrect_count")
    @Builder.Default
    private Integer incorrectCount = 0;
    
    @Column(name = "last_reviewed_at")
    private LocalDateTime lastReviewedAt;
    
    @Column(name = "next_review_at")
    private LocalDateTime nextReviewAt;
    
    @Column(name = "mastery_score")
    @Builder.Default
    private Double masteryScore = 0.0;
    
    // SM-2 Algorithm fields
    @Column(name = "easiness_factor")
    @Builder.Default
    private Double easinessFactor = 2.5; // Default EF value for SM-2
    
    @Column(name = "interval_days")
    @Builder.Default
    private Integer intervalDays = 0; // Days until next review
    
    @Column(name = "repetitions")
    @Builder.Default
    private Integer repetitions = 0; // Number of successful consecutive reviews
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum LearningStatus {
        NOT_STARTED, LEARNING, REVIEWING, MASTERED
    }
}

