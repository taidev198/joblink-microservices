package com.joblink.vocabulary.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShadowingResponse {
    /**
     * Transcribed text from user's audio
     */
    private String transcribedText;
    
    /**
     * Expected text
     */
    private String expectedText;
    
    /**
     * Accuracy percentage (0-100)
     */
    private Double accuracy;
    
    /**
     * Total number of expected words
     */
    private Integer totalExpected;
    
    /**
     * Total number of correct words
     */
    private Integer totalCorrect;
    
    /**
     * List of correct words with their positions
     */
    private List<WordMatch> correctWords;
    
    /**
     * List of wrong/mispronounced words
     */
    private List<WordMismatch> wrongWords;
    
    /**
     * List of missing words
     */
    private List<WordPosition> missingWords;
    
    /**
     * List of extra words
     */
    private List<WordPosition> extraWords;
    
    /**
     * Word-by-word comparison results
     */
    private List<WordComparison> wordComparison;
    
    /**
     * Feedback message
     */
    private String feedback;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WordMatch {
        private String word;
        private Integer position;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WordMismatch {
        private String expected;
        private String actual;
        private Integer position;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WordPosition {
        private String word;
        private Integer position;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WordComparison {
        private String status; // "✓", "✗", "−", "+"
        private String expectedWord;
        private String transcribedWord;
    }
}

