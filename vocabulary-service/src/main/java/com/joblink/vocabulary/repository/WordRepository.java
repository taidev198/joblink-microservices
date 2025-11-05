package com.joblink.vocabulary.repository;

import com.joblink.vocabulary.model.entity.Word;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WordRepository extends JpaRepository<Word, Long> {
    Optional<Word> findByEnglishWordIgnoreCase(String englishWord);
    
    Page<Word> findByLevel(Word.WordLevel level, Pageable pageable);
    
    Page<Word> findByCategory(Word.WordCategory category, Pageable pageable);
    
    Page<Word> findByLevelAndCategory(Word.WordLevel level, Word.WordCategory category, Pageable pageable);
    
    Page<Word> findByIsActiveTrue(Pageable pageable);
    
    @Query("SELECT w FROM Word w WHERE w.isActive = true AND " +
           "(LOWER(w.englishWord) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(w.meaning) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Word> searchWords(@Param("keyword") String keyword, Pageable pageable);
    
    @Query("SELECT w FROM Word w WHERE w.isActive = true AND w.id IN :wordIds")
    List<Word> findByIds(@Param("wordIds") List<Long> wordIds);
}

