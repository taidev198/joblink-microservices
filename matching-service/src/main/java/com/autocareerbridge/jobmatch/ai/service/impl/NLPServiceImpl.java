package com.autocareerbridge.jobmatch.ai.service.impl;

import com.autocareerbridge.jobmatch.ai.service.NLPService;
import lombok.extern.slf4j.Slf4j;
import opennlp.tools.stemmer.PorterStemmer;
import opennlp.tools.tokenize.SimpleTokenizer;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class NLPServiceImpl implements NLPService {

    private final SimpleTokenizer tokenizer = SimpleTokenizer.INSTANCE;
    private final PorterStemmer stemmer = new PorterStemmer();
    private final Set<String> stopWords = new HashSet<>(Arrays.asList(
            "a", "an", "the", "and", "or", "but", "is", "are", "was", "were", 
            "be", "been", "being", "have", "has", "had", "do", "does", "did",
            "to", "at", "by", "for", "with", "about", "against", "between", 
            "into", "through", "during", "before", "after", "above", "below",
            "from", "up", "down", "in", "out", "on", "off", "over", "under",
            "again", "further", "then", "once", "here", "there", "when", "where",
            "why", "how", "all", "any", "both", "each", "few", "more", "most",
            "other", "some", "such", "no", "nor", "not", "only", "own", "same",
            "so", "than", "too", "very", "s", "t", "can", "will", "just", "don",
            "should", "now", "d", "ll", "m", "o", "re", "ve", "y", "ain", "aren",
            "couldn", "didn", "doesn", "hadn", "hasn", "haven", "isn", "ma",
            "mightn", "mustn", "needn", "shan", "shouldn", "wasn", "weren", "won",
            "wouldn", "of", "this", "that", "these", "those", "as", "if", "while",
            "because", "until", "unless", "since", "although"
    ));

    @Override
    public List<String> extractKeywords(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        try {
            // Tokenize text
            String[] tokens = tokenizer.tokenize(text.toLowerCase());
            
            // Remove stop words and apply stemming
            return Arrays.stream(tokens)
                    .filter(token -> token.length() > 2) // Filter out very short words
                    .filter(token -> !stopWords.contains(token)) // Filter out stop words
                    .filter(token -> token.matches("[a-zA-Z]+")) // Keep only alphabetic words
                    .map(stemmer::stem) // Apply stemming
                    .distinct() // Remove duplicates
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error extracting keywords: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    public double calculateSimilarity(List<String> keywords1, List<String> keywords2) {
        if (keywords1 == null || keywords2 == null || keywords1.isEmpty() || keywords2.isEmpty()) {
            return 0.0;
        }
        
        try {
            // Convert to lowercase and stem
            List<String> processedKeywords1 = keywords1.stream()
                    .map(String::toLowerCase)
                    .map(stemmer::stem)
                    .collect(Collectors.toList());
            
            List<String> processedKeywords2 = keywords2.stream()
                    .map(String::toLowerCase)
                    .map(stemmer::stem)
                    .collect(Collectors.toList());
            
            // Calculate Jaccard similarity
            Set<String> union = new HashSet<>(processedKeywords1);
            union.addAll(processedKeywords2);
            
            Set<String> intersection = new HashSet<>(processedKeywords1);
            intersection.retainAll(processedKeywords2);
            
            // Basic Jaccard similarity
            double jaccardSimilarity = (double) intersection.size() / union.size();
            
            // Calculate cosine similarity for more nuanced matching
            Map<String, Integer> vector1 = createTermFrequencyVector(processedKeywords1);
            Map<String, Integer> vector2 = createTermFrequencyVector(processedKeywords2);
            
            double cosineSimilarity = calculateCosineSimilarity(vector1, vector2);
            
            // Combine both similarity measures (weighted average)
            return 0.4 * jaccardSimilarity + 0.6 * cosineSimilarity;
        } catch (Exception e) {
            log.error("Error calculating similarity: {}", e.getMessage(), e);
            return 0.0;
        }
    }

    @Override
    public double calculateWordSimilarity(String word1, String word2) {
        if (word1 == null || word2 == null || word1.trim().isEmpty() || word2.trim().isEmpty()) {
            return 0.0;
        }
        
        try {
            // Process words
            String processedWord1 = stemmer.stem(word1.toLowerCase());
            String processedWord2 = stemmer.stem(word2.toLowerCase());
            
            // Exact match after stemming
            if (processedWord1.equals(processedWord2)) {
                return 1.0;
            }
            
            // Calculate Levenshtein distance
            int distance = levenshteinDistance(processedWord1, processedWord2);
            int maxLength = Math.max(processedWord1.length(), processedWord2.length());
            
            // Convert distance to similarity score (1 - normalized distance)
            return 1.0 - (double) distance / maxLength;
        } catch (Exception e) {
            log.error("Error calculating word similarity: {}", e.getMessage(), e);
            return 0.0;
        }
    }
    
    private Map<String, Integer> createTermFrequencyVector(List<String> terms) {
        Map<String, Integer> vector = new HashMap<>();
        for (String term : terms) {
            vector.put(term, vector.getOrDefault(term, 0) + 1);
        }
        return vector;
    }
    
    private double calculateCosineSimilarity(Map<String, Integer> vector1, Map<String, Integer> vector2) {
        // Get all unique terms
        Set<String> allTerms = new HashSet<>(vector1.keySet());
        allTerms.addAll(vector2.keySet());
        
        // Calculate dot product
        double dotProduct = 0.0;
        for (String term : allTerms) {
            dotProduct += vector1.getOrDefault(term, 0) * vector2.getOrDefault(term, 0);
        }
        
        // Calculate magnitudes
        double magnitude1 = calculateMagnitude(vector1);
        double magnitude2 = calculateMagnitude(vector2);
        
        // Avoid division by zero
        if (magnitude1 == 0.0 || magnitude2 == 0.0) {
            return 0.0;
        }
        
        // Return cosine similarity
        return dotProduct / (magnitude1 * magnitude2);
    }
    
    private double calculateMagnitude(Map<String, Integer> vector) {
        double sumOfSquares = 0.0;
        for (int value : vector.values()) {
            sumOfSquares += value * value;
        }
        return Math.sqrt(sumOfSquares);
    }
    
    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        
        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }
        
        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost
                );
            }
        }
        
        return dp[s1.length()][s2.length()];
    }
}