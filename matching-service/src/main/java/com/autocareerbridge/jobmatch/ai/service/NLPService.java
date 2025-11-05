package com.autocareerbridge.jobmatch.ai.service;

import java.util.List;

public interface NLPService {
    /**
     * Extract keywords from text
     * 
     * @param text The text to extract keywords from
     * @return List of extracted keywords
     */
    List<String> extractKeywords(String text);
    
    /**
     * Calculate similarity between two lists of keywords
     * 
     * @param keywords1 First list of keywords
     * @param keywords2 Second list of keywords
     * @return Similarity score between 0 and 1
     */
    double calculateSimilarity(List<String> keywords1, List<String> keywords2);
    
    /**
     * Calculate similarity between two words
     * 
     * @param word1 First word
     * @param word2 Second word
     * @return Similarity score between 0 and 1
     */
    double calculateWordSimilarity(String word1, String word2);
}