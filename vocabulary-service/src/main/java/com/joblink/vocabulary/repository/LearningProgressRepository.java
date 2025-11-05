package com.joblink.vocabulary.repository;

import com.joblink.vocabulary.model.entity.LearningProgress;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LearningProgressRepository extends JpaRepository<LearningProgress, Long> {
    Page<LearningProgress> findByUserId(Long userId, Pageable pageable);
    
    Page<LearningProgress> findByUserIdAndWordId(Long userId, Long wordId, Pageable pageable);
    
    @Query("SELECT COUNT(lp) FROM LearningProgress lp WHERE lp.userId = :userId AND lp.isCorrect = true")
    Long countCorrectAnswers(@Param("userId") Long userId);
    
    @Query("SELECT COUNT(lp) FROM LearningProgress lp WHERE lp.userId = :userId AND " +
           "lp.createdAt >= :startDate AND lp.createdAt <= :endDate")
    Long countByUserIdAndDateRange(@Param("userId") Long userId, 
                                   @Param("startDate") LocalDateTime startDate,
                                   @Param("endDate") LocalDateTime endDate);
}

