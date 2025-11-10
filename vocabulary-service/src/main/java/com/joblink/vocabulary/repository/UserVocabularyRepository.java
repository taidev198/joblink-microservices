package com.joblink.vocabulary.repository;

import com.joblink.vocabulary.model.entity.UserVocabulary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserVocabularyRepository extends JpaRepository<UserVocabulary, Long> {
    Optional<UserVocabulary> findByUserIdAndWordId(Long userId, Long wordId);
    
    Page<UserVocabulary> findByUserId(Long userId, Pageable pageable);
    
    Page<UserVocabulary> findByUserIdAndStatus(Long userId, UserVocabulary.LearningStatus status, Pageable pageable);
    
    @Query("SELECT uv FROM UserVocabulary uv WHERE uv.userId = :userId AND uv.nextReviewAt <= :now")
    List<UserVocabulary> findWordsDueForReview(@Param("userId") Long userId, @Param("now") LocalDateTime now);
    
    @Query("SELECT COUNT(uv) FROM UserVocabulary uv WHERE uv.userId = :userId")
    Long countByUserId(@Param("userId") Long userId);
    
    @Query("SELECT COUNT(uv) FROM UserVocabulary uv WHERE uv.userId = :userId AND uv.status = :status")
    Long countByUserIdAndStatus(@Param("userId") Long userId, @Param("status") UserVocabulary.LearningStatus status);
    
    // Get words reviewed today (using date range)
    @Query("SELECT uv FROM UserVocabulary uv WHERE uv.userId = :userId AND uv.lastReviewedAt IS NOT NULL AND uv.lastReviewedAt >= :startOfDay AND uv.lastReviewedAt <= :endOfDay")
    List<UserVocabulary> findWordsReviewedToday(@Param("userId") Long userId, @Param("startOfDay") LocalDateTime startOfDay, @Param("endOfDay") LocalDateTime endOfDay);
    
    // Get new words learned today (words with reviewCount = 1 and lastReviewedAt is today and quality >= 3)
    @Query("SELECT uv FROM UserVocabulary uv WHERE uv.userId = :userId AND uv.reviewCount = 1 AND uv.lastReviewedAt IS NOT NULL AND uv.lastReviewedAt >= :startOfDay AND uv.lastReviewedAt <= :endOfDay AND uv.repetitions > 0")
    List<UserVocabulary> findNewWordsLearnedToday(@Param("userId") Long userId, @Param("startOfDay") LocalDateTime startOfDay, @Param("endOfDay") LocalDateTime endOfDay);
    
    // Count new words learned today
    @Query("SELECT COUNT(uv) FROM UserVocabulary uv WHERE uv.userId = :userId AND uv.reviewCount = 1 AND uv.lastReviewedAt IS NOT NULL AND uv.lastReviewedAt >= :startOfDay AND uv.lastReviewedAt <= :endOfDay AND uv.repetitions > 0")
    Long countNewWordsLearnedToday(@Param("userId") Long userId, @Param("startOfDay") LocalDateTime startOfDay, @Param("endOfDay") LocalDateTime endOfDay);
}

