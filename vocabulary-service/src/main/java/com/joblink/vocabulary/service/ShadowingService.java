package com.joblink.vocabulary.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.joblink.vocabulary.dto.request.ShadowingRequest;
import com.joblink.vocabulary.dto.response.ShadowingResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class ShadowingService {
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${whisper.api.url:http://localhost:8000/api/transcribe}")
    private String whisperApiUrl;
    
    @Value("${whisper.temp.dir:${java.io.tmpdir}/whisper}")
    private String tempDir;
    
    /**
     * Map of contractions to their expanded forms
     * Based on whisper project's english_practice.py
     */
    private static final Map<String, List<String>> CONTRACTIONS = new HashMap<>();
    
    static {
        CONTRACTIONS.put("there's", Arrays.asList("there", "is"));
        CONTRACTIONS.put("it's", Arrays.asList("it", "is"));
        CONTRACTIONS.put("that's", Arrays.asList("that", "is"));
        CONTRACTIONS.put("what's", Arrays.asList("what", "is"));
        CONTRACTIONS.put("who's", Arrays.asList("who", "is"));
        CONTRACTIONS.put("where's", Arrays.asList("where", "is"));
        CONTRACTIONS.put("here's", Arrays.asList("here", "is"));
        CONTRACTIONS.put("he's", Arrays.asList("he", "is"));
        CONTRACTIONS.put("she's", Arrays.asList("she", "is"));
        CONTRACTIONS.put("we're", Arrays.asList("we", "are"));
        CONTRACTIONS.put("they're", Arrays.asList("they", "are"));
        CONTRACTIONS.put("you're", Arrays.asList("you", "are"));
        CONTRACTIONS.put("i'm", Arrays.asList("i", "am"));
        CONTRACTIONS.put("i've", Arrays.asList("i", "have"));
        CONTRACTIONS.put("i'll", Arrays.asList("i", "will"));
        CONTRACTIONS.put("can't", Arrays.asList("can", "not"));
        CONTRACTIONS.put("won't", Arrays.asList("will", "not"));
        CONTRACTIONS.put("don't", Arrays.asList("do", "not"));
        CONTRACTIONS.put("doesn't", Arrays.asList("does", "not"));
        CONTRACTIONS.put("didn't", Arrays.asList("did", "not"));
        CONTRACTIONS.put("isn't", Arrays.asList("is", "not"));
        CONTRACTIONS.put("aren't", Arrays.asList("are", "not"));
        CONTRACTIONS.put("wasn't", Arrays.asList("was", "not"));
        CONTRACTIONS.put("weren't", Arrays.asList("were", "not"));
        CONTRACTIONS.put("hasn't", Arrays.asList("has", "not"));
        CONTRACTIONS.put("haven't", Arrays.asList("have", "not"));
        CONTRACTIONS.put("hadn't", Arrays.asList("had", "not"));
    }
    
    /**
     * Normalize text for comparison - handle contractions and punctuation
     * Based on whisper project's normalize_text function
     */
    public List<String> normalizeText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        // Convert to lowercase
        String normalized = text.toLowerCase().trim();
        
        // Handle contractions by expanding them
        for (Map.Entry<String, List<String>> entry : CONTRACTIONS.entrySet()) {
            String contraction = entry.getKey();
            List<String> expansion = entry.getValue();
            
            // Use word boundaries to match whole words only
            String pattern = "\\b" + Pattern.quote(contraction) + "\\b";
            String replacement = String.join(" ", expansion);
            normalized = normalized.replaceAll(pattern, replacement);
        }
        
        // Remove punctuation and split into words
        normalized = normalized.replaceAll("[^\\w\\s]", "");
        String[] words = normalized.split("\\s+");
        
        // Remove empty strings
        List<String> result = new ArrayList<>();
        for (String word : words) {
            if (!word.isEmpty()) {
                result.add(word);
            }
        }
        
        return result;
    }
    
    /**
     * Compare transcribed and expected texts with detailed word-by-word analysis
     * Based on whisper project's compare_texts function using sequence alignment
     */
    public ShadowingResponse compareTexts(String transcribed, String expected) {
        List<String> transcribedWords = normalizeText(transcribed);
        List<String> expectedWords = normalizeText(expected);
        
        if (expectedWords.isEmpty()) {
            return ShadowingResponse.builder()
                    .transcribedText(transcribed)
                    .expectedText(expected)
                    .accuracy(0.0)
                    .totalExpected(0)
                    .totalCorrect(0)
                    .correctWords(new ArrayList<>())
                    .wrongWords(new ArrayList<>())
                    .missingWords(new ArrayList<>())
                    .extraWords(new ArrayList<>())
                    .wordComparison(new ArrayList<>())
                    .feedback("No expected text provided")
                    .build();
        }
        
        int n = expectedWords.size();
        int m = transcribedWords.size();
        
        // Dynamic programming for sequence alignment
        // dp[i][j] = best match score for expected[0:i] and transcribed[0:j]
        int[][] dp = new int[n + 1][m + 1];
        String[][] path = new String[n + 1][m + 1];
        
        // Initialize
        for (int i = 1; i <= n; i++) {
            dp[i][0] = -i;
            path[i][0] = "del";
        }
        for (int j = 1; j <= m; j++) {
            dp[0][j] = -j;
            path[0][j] = "ins";
        }
        
        // Fill DP table
        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {
                int matchScore = expectedWords.get(i - 1).equals(transcribedWords.get(j - 1)) ? 2 : -1;
                int match = dp[i - 1][j - 1] + matchScore;
                int delete = dp[i - 1][j] - 1;
                int insert = dp[i][j - 1] - 1;
                
                if (match >= delete && match >= insert) {
                    dp[i][j] = match;
                    path[i][j] = "match";
                } else if (delete >= insert) {
                    dp[i][j] = delete;
                    path[i][j] = "del";
                } else {
                    dp[i][j] = insert;
                    path[i][j] = "ins";
                }
            }
        }
        
        // Backtrack to find alignment
        List<Alignment> alignment = new ArrayList<>();
        int i = n, j = m;
        while (i > 0 || j > 0) {
            if (i > 0 && j > 0 && "match".equals(path[i][j])) {
                alignment.add(new Alignment("match", expectedWords.get(i - 1), transcribedWords.get(j - 1), i - 1, j - 1));
                i--;
                j--;
            } else if (i > 0 && "del".equals(path[i][j])) {
                alignment.add(new Alignment("del", expectedWords.get(i - 1), null, i - 1, null));
                i--;
            } else if (j > 0 && "ins".equals(path[i][j])) {
                alignment.add(new Alignment("ins", null, transcribedWords.get(j - 1), null, j - 1));
                j--;
            } else {
                break;
            }
        }
        
        Collections.reverse(alignment);
        
        // Analyze alignment
        List<ShadowingResponse.WordMatch> correctWords = new ArrayList<>();
        List<ShadowingResponse.WordMismatch> wrongWords = new ArrayList<>();
        List<ShadowingResponse.WordPosition> missingWords = new ArrayList<>();
        List<ShadowingResponse.WordPosition> extraWords = new ArrayList<>();
        List<ShadowingResponse.WordComparison> wordComparison = new ArrayList<>();
        
        for (Alignment align : alignment) {
            if ("match".equals(align.operation)) {
                if (align.expectedWord.equals(align.transcribedWord)) {
                    correctWords.add(ShadowingResponse.WordMatch.builder()
                            .word(align.expectedWord)
                            .position(align.expectedIndex)
                            .build());
                    wordComparison.add(ShadowingResponse.WordComparison.builder()
                            .status("✓")
                            .expectedWord(align.expectedWord)
                            .transcribedWord(align.transcribedWord)
                            .build());
                } else {
                    wrongWords.add(ShadowingResponse.WordMismatch.builder()
                            .expected(align.expectedWord)
                            .actual(align.transcribedWord)
                            .position(align.expectedIndex)
                            .build());
                    wordComparison.add(ShadowingResponse.WordComparison.builder()
                            .status("✗")
                            .expectedWord(align.expectedWord)
                            .transcribedWord(align.transcribedWord)
                            .build());
                }
            } else if ("del".equals(align.operation)) {
                missingWords.add(ShadowingResponse.WordPosition.builder()
                        .word(align.expectedWord)
                        .position(align.expectedIndex)
                        .build());
                wordComparison.add(ShadowingResponse.WordComparison.builder()
                        .status("−")
                        .expectedWord(align.expectedWord)
                        .transcribedWord(null)
                        .build());
            } else if ("ins".equals(align.operation)) {
                extraWords.add(ShadowingResponse.WordPosition.builder()
                        .word(align.transcribedWord)
                        .position(align.transcribedIndex)
                        .build());
                wordComparison.add(ShadowingResponse.WordComparison.builder()
                        .status("+")
                        .expectedWord(null)
                        .transcribedWord(align.transcribedWord)
                        .build());
            }
        }
        
        // Calculate accuracy
        int totalWords = expectedWords.size();
        int correctCount = correctWords.size();
        double accuracy = totalWords > 0 ? (correctCount * 100.0 / totalWords) : 0.0;
        
        // Generate feedback
        String feedback = generateFeedback(accuracy, correctCount, totalWords, wrongWords.size(), missingWords.size(), extraWords.size());
        
        return ShadowingResponse.builder()
                .transcribedText(transcribed)
                .expectedText(expected)
                .accuracy(accuracy)
                .totalExpected(totalWords)
                .totalCorrect(correctCount)
                .correctWords(correctWords)
                .wrongWords(wrongWords)
                .missingWords(missingWords)
                .extraWords(extraWords)
                .wordComparison(wordComparison)
                .feedback(feedback)
                .build();
    }
    
    /**
     * Transcribe audio using Whisper API
     */
    public String transcribeAudio(MultipartFile audioFile, String language, Boolean wordTimestamps) {
        Path tempFile = null;
        try {
            log.info("Transcribing audio file: {}, language: {}", audioFile.getOriginalFilename(), language);
            
            // Save audio file temporarily
            Path tempDirPath = Paths.get(tempDir);
            Files.createDirectories(tempDirPath);
            String filename = UUID.randomUUID().toString() + "_" + audioFile.getOriginalFilename();
            tempFile = tempDirPath.resolve(filename);
            audioFile.transferTo(tempFile.toFile());
            
            // Prepare multipart request
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new org.springframework.core.io.FileSystemResource(tempFile.toFile()));
            body.add("language", language);
            body.add("word_timestamps", wordTimestamps != null ? wordTimestamps.toString() : "true");
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            
            // Call Whisper API
            ResponseEntity<String> response = restTemplate.exchange(
                    whisperApiUrl,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                // Parse response
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                String transcribedText = jsonResponse.has("text") ? jsonResponse.get("text").asText() : "";
                log.info("Transcription successful: {}", transcribedText);
                return transcribedText;
            } else {
                log.error("Whisper API returned error: {}", response.getStatusCode());
                throw new RuntimeException("Failed to transcribe audio: " + response.getStatusCode());
            }
        } catch (org.springframework.web.client.ResourceAccessException e) {
            log.error("Cannot connect to Whisper API at {}: {}", whisperApiUrl, e.getMessage());
            throw new RuntimeException(
                "Cannot connect to Whisper API. Please ensure the Whisper API server is running at " + whisperApiUrl + 
                ". See whisper/README_API.md for instructions on starting the server.", e
            );
        } catch (org.springframework.web.client.HttpClientErrorException | org.springframework.web.client.HttpServerErrorException e) {
            log.error("Whisper API HTTP error: {}", e.getMessage());
            throw new RuntimeException("Whisper API error: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error transcribing audio", e);
            throw new RuntimeException("Failed to transcribe audio: " + e.getMessage(), e);
        } finally {
            // Clean up temp file
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException e) {
                    log.warn("Failed to delete temp file: {}", tempFile, e);
                }
            }
        }
    }
    
    /**
     * Perform shadowing practice: transcribe audio and compare with expected text
     */
    public ShadowingResponse performShadowing(ShadowingRequest request) {
        try {
            log.info("Performing shadowing practice for expected text: {}", request.getExpectedText());
            
            // Step 1: Transcribe audio using Whisper
            String transcribedText = transcribeAudio(
                    request.getAudioFile(),
                    request.getLanguage() != null ? request.getLanguage() : "en",
                    request.getWordTimestamps() != null ? request.getWordTimestamps() : true
            );
            
            // Step 2: Compare transcribed text with expected text
            ShadowingResponse response = compareTexts(transcribedText, request.getExpectedText());
            
            log.info("Shadowing practice completed. Accuracy: {}%", response.getAccuracy());
            return response;
        } catch (Exception e) {
            log.error("Error performing shadowing practice", e);
            throw new RuntimeException("Failed to perform shadowing practice: " + e.getMessage(), e);
        }
    }
    
    /**
     * Generate feedback message based on accuracy and errors
     */
    private String generateFeedback(double accuracy, int correctCount, int totalWords, 
                                    int wrongCount, int missingCount, int extraCount) {
        StringBuilder feedback = new StringBuilder();
        
        if (accuracy >= 90) {
            feedback.append("🌟 Excellent! Your pronunciation is very clear!");
        } else if (accuracy >= 70) {
            feedback.append("👍 Good! You're doing well. Keep practicing to improve.");
        } else if (accuracy >= 50) {
            feedback.append("💪 Not bad! Focus on the words marked above to improve.");
        } else {
            feedback.append("📚 Keep practicing! Focus on clear pronunciation of the words marked above.");
        }
        
        feedback.append("\n\n");
        feedback.append(String.format("Correct words: %d/%d (%.1f%%)", correctCount, totalWords, accuracy));
        
        if (wrongCount > 0) {
            feedback.append(String.format("\nWrong/Mispronounced words: %d", wrongCount));
        }
        if (missingCount > 0) {
            feedback.append(String.format("\nMissing words: %d", missingCount));
        }
        if (extraCount > 0) {
            feedback.append(String.format("\nExtra words: %d", extraCount));
        }
        
        return feedback.toString();
    }
    
    /**
     * Internal class for alignment tracking
     */
    private static class Alignment {
        String operation; // "match", "del", "ins"
        String expectedWord;
        String transcribedWord;
        Integer expectedIndex;
        Integer transcribedIndex;
        
        Alignment(String operation, String expectedWord, String transcribedWord, 
                 Integer expectedIndex, Integer transcribedIndex) {
            this.operation = operation;
            this.expectedWord = expectedWord;
            this.transcribedWord = transcribedWord;
            this.expectedIndex = expectedIndex;
            this.transcribedIndex = transcribedIndex;
        }
    }
}

