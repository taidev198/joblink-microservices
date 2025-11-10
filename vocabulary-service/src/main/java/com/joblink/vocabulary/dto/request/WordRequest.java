package com.joblink.vocabulary.dto.request;

import com.joblink.vocabulary.model.entity.Word;
import com.joblink.vocabulary.model.entity.WordCategory;
import com.joblink.vocabulary.model.entity.WordLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WordRequest {
    @NotBlank(message = "English word is required")
    private String englishWord;
    
    @NotBlank(message = "Meaning is required")
    private String meaning;
    
    private String pronunciation;
    private String exampleSentence;
    private String translation;
    
    @NotNull(message = "Level is required")
    private WordLevel level;
    
    @NotNull(message = "Category is required")
    private WordCategory category;
    
    private String partOfSpeech;
    private String synonyms;
    private String antonyms;
    private String imageUrl;
    private String audioUrl;
}

