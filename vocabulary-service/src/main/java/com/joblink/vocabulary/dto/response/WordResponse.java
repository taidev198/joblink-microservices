package com.joblink.vocabulary.dto.response;

import com.joblink.vocabulary.model.entity.Word;
import com.joblink.vocabulary.model.entity.WordCategory;
import com.joblink.vocabulary.model.entity.WordLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WordResponse {
    private Long id;
    private String englishWord;
    private String meaning;
    private String pronunciation;
    private String exampleSentence;
    private String translation;
    private WordLevel level;
    private WordCategory category;
    private String partOfSpeech;
    private String synonyms;
    private String antonyms;
    private String imageUrl;
    private String audioUrl;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

