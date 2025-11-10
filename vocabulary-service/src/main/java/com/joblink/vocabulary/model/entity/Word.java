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
@Table(name = "words")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Word {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String englishWord;
    
    @Column(nullable = false)
    private String meaning;
    
    @Column(length = 500)
    private String pronunciation;
    
    @Column(length = 1000)
    private String exampleSentence;
    
    @Column(length = 1000)
    private String translation;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WordLevel level;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WordCategory category;
    
    @Column(length = 50)
    private String partOfSpeech; // noun, verb, adjective, etc.
    
    @Column(length = 500)
    private String synonyms;
    
    @Column(length = 500)
    private String antonyms;
    
    @Column(name = "image_url")
    private String imageUrl;
    
    @Column(name = "audio_url")
    private String audioUrl;

    // Adding separate fields for English and Vietnamese audio URLs
    @Column(name = "english_audio_url")
    private String englishAudioUrl;

    @Column(name = "vietnamese_audio_url")
    private String vietnameseAudioUrl;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum WordLevel {
        BEGINNER, INTERMEDIATE, ADVANCED
    }
    
    public enum WordCategory {
        DAILY_LIFE, BUSINESS, ACADEMIC, TECHNOLOGY, TRAVEL, FOOD, SPORTS, ENTERTAINMENT, OTHER
    }
}

