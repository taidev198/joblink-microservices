package com.joblink.vocabulary.dto.response;

import com.joblink.vocabulary.model.entity.Word;
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
    private Word.WordLevel level;
    private Word.WordCategory category;
    private String partOfSpeech;
    private String synonyms;
    private String antonyms;
    private String imageUrl;
    private String audioUrl;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

