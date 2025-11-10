package com.joblink.vocabulary.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.joblink.vocabulary.dto.request.WordRequest;
import com.joblink.vocabulary.dto.response.ApiResponse;
import com.joblink.vocabulary.model.entity.Word;
import com.joblink.vocabulary.model.entity.UserVocabulary;
import com.joblink.vocabulary.model.entity.WordCategory;
import com.joblink.vocabulary.model.entity.WordLevel;
import com.joblink.vocabulary.repository.WordRepository;
import com.joblink.vocabulary.repository.UserVocabularyRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Slf4j
@Service
public class ContextVocabService {
    
    private final WordRepository wordRepository;
    private final UserVocabularyRepository userVocabularyRepository;
    private final WordService wordService;
    private final SM2SpacedRepetitionService sm2Service;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    
    // AI Models - using Anthropic Claude (optional to avoid bean creation issues)
    @Autowired(required = false)
    @Nullable
    private AnthropicChatModel anthropicChatModel;
    
    public ContextVocabService(
            WordRepository wordRepository,
            UserVocabularyRepository userVocabularyRepository,
            WordService wordService,
            SM2SpacedRepetitionService sm2Service,
            ObjectMapper objectMapper,
            RestTemplate restTemplate) {
        this.wordRepository = wordRepository;
        this.userVocabularyRepository = userVocabularyRepository;
        this.wordService = wordService;
        this.sm2Service = sm2Service;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
    }
    
    @Value("${phoneme.backend.url:http://localhost:8001}")
    private String phonemeBackendUrl;
    
    @Value("${spring.ai.provider:anthropic}")
    private String aiProvider; // "openai" or "anthropic"
    
    /**
     * Generate vocabulary from image using Vision API + GPT
     */
    public ApiResponse<Map<String, Object>> generateVocabFromImage(MultipartFile imageFile, Long userId) {
        try {
            // Step 1: Object detection using Python backend
            Map<String, Object> detectionResult = detectObjects(imageFile);
            String imageCaption = (String) detectionResult.get("caption");
            @SuppressWarnings("unchecked")
            List<String> objects = (List<String>) detectionResult.get("objects");
            log.info("Image caption: {}", imageCaption);
            log.info("Detected objects: {}", objects);
            
            // Step 2: Generate vocabulary using Claude AI
            Map<String, Object> vocabData = generateVocabWithGPT(objects, imageCaption);
            
            // Add metadata
            vocabData.put("imageCaption", imageCaption);
            vocabData.put("objects", objects);
            vocabData.put("detections", detectionResult.get("detections"));
            
            // Ensure vocabulary array exists (fallback if generation fails)
            if (!vocabData.containsKey("vocabulary")) {
                vocabData.put("vocabulary", new ArrayList<>());
            }
            if (!vocabData.containsKey("paragraph")) {
                vocabData.put("paragraph", imageCaption);
            }
            
            return ApiResponse.success(vocabData);
        } catch (Exception e) {
            log.error("Error generating vocabulary from image", e);
            return ApiResponse.error("Failed to generate vocabulary: " + e.getMessage());
        }
    }
    
    /**
     * Detect objects in image using Python backend
     */
    private Map<String, Object> detectObjects(MultipartFile imageFile) throws IOException {
        try {
            String apiUrl = phonemeBackendUrl + "/detect-objects/";
            
            // Prepare multipart form data using Spring's RestTemplate
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            
            // Create a Resource from the MultipartFile bytes
            Resource fileResource = new ByteArrayResource(imageFile.getBytes()) {
                @Override
                public String getFilename() {
                    return imageFile.getOriginalFilename();
                }
            };
            
            body.add("file", fileResource);
            
            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            
            // Make the request
            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                
                Map<String, Object> result = new HashMap<>();
                result.put("caption", jsonResponse.has("caption") ? jsonResponse.get("caption").asText() : "A scene with various objects");
                
                List<String> objects = new ArrayList<>();
                if (jsonResponse.has("objects") && jsonResponse.get("objects").isArray()) {
                    for (JsonNode obj : jsonResponse.get("objects")) {
                        objects.add(obj.asText());
                    }
                }
                result.put("objects", objects);
                
                List<Map<String, Object>> detections = new ArrayList<>();
                if (jsonResponse.has("detections") && jsonResponse.get("detections").isArray()) {
                    for (JsonNode det : jsonResponse.get("detections")) {
                        Map<String, Object> detection = new HashMap<>();
                        if (det.has("label")) {
                            detection.put("label", det.get("label").asText());
                        }
                        if (det.has("confidence")) {
                            detection.put("confidence", det.get("confidence").asDouble());
                        }
                        detections.add(detection);
                    }
                }
                result.put("detections", detections);
                
                return result;
            }
            
            // Fallback: return empty result
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("caption", "A scene with various objects");
            fallback.put("objects", new ArrayList<String>());
            fallback.put("detections", new ArrayList<Map<String, Object>>());
            return fallback;
        } catch (Exception e) {
            log.error("Error calling Python backend for object detection", e);
            // Fallback: return empty result
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("caption", "A scene with various objects");
            fallback.put("objects", new ArrayList<String>());
            fallback.put("detections", new ArrayList<Map<String, Object>>());
            return fallback;
        }
    }
    
    /**
     * Generate vocabulary using GPT API
     */
    private Map<String, Object> generateVocabWithGPT(List<String> objects, String description) {
        try {
            String prompt = buildPrompt(objects, description);
            
            // Call GPT API
            String gptResponse = callGPTAPI(prompt);
            
            // Parse GPT response
            return parseGPTResponse(gptResponse);
        } catch (RuntimeException e) {
            // Check if it's a quota error
            if (e.getMessage() != null && e.getMessage().contains("quota exceeded")) {
                log.warn("OpenAI API quota exceeded. Using fallback vocabulary generation.", e);
                return generateFallbackVocab(objects);
            }
            log.error("Error calling GPT API, using fallback", e);
            return generateFallbackVocab(objects);
        } catch (Exception e) {
            log.error("Error calling GPT API, using fallback", e);
            return generateFallbackVocab(objects);
        }
    }
    
    /**
     * Build prompt for GPT
     */
    private String buildPrompt(List<String> objects, String description) {
        return String.format(
            "You are an English teacher.\n" +
            "Given these objects: %s\n" +
            "Scene description: %s\n\n" +
            "Generate:\n" +
            "1. A vocabulary list (word + part of speech + CEFR level)\n" +
            "2. Example sentences for each word\n" +
            "3. A short paragraph describing the scene naturally\n\n" +
            "IMPORTANT: Return ONLY valid JSON, no additional text before or after. Use this exact format:\n" +
            "{\n" +
            "  \"vocabulary\": [\n" +
            "    {\"word\": \"laptop\", \"pos\": \"noun\", \"level\": \"A2\", \"example\": \"She is working on her laptop.\"},\n" +
            "    {\"word\": \"croissant\", \"pos\": \"noun\", \"level\": \"A2\", \"example\": \"He is eating a croissant with coffee.\"}\n" +
            "  ],\n" +
            "  \"paragraph\": \"A young woman is sitting in a café. She is working on her laptop and drinking coffee.\"\n" +
            "}",
            objects.toString(),
            description
        );
    }
    
    /**
     * Call AI API (GPT or Claude) using Spring AI ChatModel
     * Automatically falls back to Claude if OpenAI quota is exceeded
     */
    private String callGPTAPI(String prompt) {
        if (anthropicChatModel == null) {
            throw new RuntimeException("AnthropicChatModel bean is not available. Please check your Spring AI configuration.");
        }
        
        // Try primary provider first
        ChatModel primaryModel = anthropicChatModel;
        String primaryProvider = "Claude";
        ChatModel fallbackModel = anthropicChatModel;
        String fallbackProvider = "Claude";
        
        try {
            // Try primary AI provider
            log.info("Calling {} API with prompt", primaryProvider);
            Prompt chatPrompt = new Prompt(new UserMessage(prompt));
            ChatResponse response = primaryModel.call(chatPrompt);
            return response.getResult().getOutput().getContent();
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            if (e.getStatusCode() != null && e.getStatusCode().value() == 429) {
                log.warn("{} API quota exceeded. Falling back to {}.", primaryProvider, fallbackProvider);
                return tryFallbackAI(fallbackModel, prompt, fallbackProvider);
            }
            log.error("HTTP error calling {} API via Spring AI", primaryProvider, e);
            // Try fallback
            return tryFallbackAI(fallbackModel, prompt, fallbackProvider);
        } catch (Exception e) {
            log.error("Error calling {} API via Spring AI", primaryProvider, e);
            // Check if it's a quota-related error in the message
            if (e.getMessage() != null && (e.getMessage().contains("quota") || e.getMessage().contains("insufficient_quota"))) {
                log.warn("{} API quota exceeded. Falling back to {}.", primaryProvider, fallbackProvider);
                return tryFallbackAI(fallbackModel, prompt, fallbackProvider);
            }
            // Try fallback
            return tryFallbackAI(fallbackModel, prompt, fallbackProvider);
        }
    }
    
    /**
     * Try fallback AI provider
     */
    private String tryFallbackAI(ChatModel fallbackModel, String prompt, String providerName) {
        try {
            log.info("Calling fallback {} API with prompt", providerName);
            Prompt chatPrompt = new Prompt(new UserMessage(prompt));
            ChatResponse response = fallbackModel.call(chatPrompt);
            return response.getResult().getOutput().getContent();
        } catch (Exception e) {
            log.error("Error calling fallback {} API via Spring AI", providerName, e);
            throw new RuntimeException("Failed to get response from both AI providers: " + e.getMessage());
        }
    }
    
    /**
     * Parse GPT response
     */
    private Map<String, Object> parseGPTResponse(String gptResponse) {
        try {
            // Extract JSON from response (might have markdown code blocks or text before/after)
            String jsonStr = gptResponse.trim();
            
            // First, try to extract from markdown code blocks
            if (jsonStr.contains("```json")) {
                jsonStr = jsonStr.substring(jsonStr.indexOf("```json") + 7);
                jsonStr = jsonStr.substring(0, jsonStr.indexOf("```"));
            } else if (jsonStr.contains("```")) {
                jsonStr = jsonStr.substring(jsonStr.indexOf("```") + 3);
                jsonStr = jsonStr.substring(0, jsonStr.indexOf("```"));
            } else {
                // Extract JSON object from text (find first { and last })
                int firstBrace = jsonStr.indexOf('{');
                int lastBrace = jsonStr.lastIndexOf('}');
                if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
                    jsonStr = jsonStr.substring(firstBrace, lastBrace + 1);
                }
            }
            jsonStr = jsonStr.trim();
            
            log.debug("Extracted JSON string: {}", jsonStr);
            JsonNode jsonNode = objectMapper.readTree(jsonStr);
            
            Map<String, Object> result = new HashMap<>();
            
            // Parse vocabulary
            List<Map<String, String>> vocabulary = new ArrayList<>();
            if (jsonNode.has("vocabulary") && jsonNode.get("vocabulary").isArray()) {
                for (JsonNode vocab : jsonNode.get("vocabulary")) {
                    Map<String, String> vocabItem = new HashMap<>();
                    vocabItem.put("word", vocab.has("word") ? vocab.get("word").asText() : "");
                    vocabItem.put("pos", vocab.has("pos") ? vocab.get("pos").asText() : "noun");
                    vocabItem.put("level", vocab.has("level") ? vocab.get("level").asText() : "A2");
                    vocabItem.put("example", vocab.has("example") ? vocab.get("example").asText() : "");
                    if (!vocabItem.get("word").isEmpty()) {
                        vocabulary.add(vocabItem);
                    }
                }
            }
            result.put("vocabulary", vocabulary);
            
            // Parse paragraph
            String paragraph = jsonNode.has("paragraph") 
                    ? jsonNode.get("paragraph").asText() 
                    : "A scene with various objects.";
            result.put("paragraph", paragraph);
            
            return result;
        } catch (Exception e) {
            log.error("Error parsing GPT response, using fallback", e);
            return generateFallbackVocab(List.of("objects", "scene"));
        }
    }
    
    /**
     * Generate fallback vocabulary when API calls fail
     */
    private Map<String, Object> generateFallbackVocab(List<String> objects) {
        List<Map<String, String>> vocabulary = new ArrayList<>();
        
        for (String obj : objects.subList(0, Math.min(5, objects.size()))) {
            Map<String, String> vocabItem = new HashMap<>();
            vocabItem.put("word", obj);
            vocabItem.put("pos", "noun");
            vocabItem.put("level", "A2");
            vocabItem.put("example", String.format("I can see a %s in the scene.", obj));
            vocabulary.add(vocabItem);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("vocabulary", vocabulary);
        result.put("paragraph", "This scene contains various objects that can help you learn new vocabulary.");
        
        return result;
    }
    
    /**
     * Save context-generated word to vocabulary
     */
    @Transactional
    public ApiResponse<Map<String, Object>> saveContextWord(
            Long userId,
            String word,
            String partOfSpeech,
            String level,
            String example,
            String context) {
        try {
            // Check if word exists
            Optional<Word> existingWord = wordRepository.findByEnglishWordIgnoreCase(word);
            Word wordEntity;
            
            if (existingWord.isPresent()) {
                wordEntity = existingWord.get();
            } else {
                // Create new word
                WordRequest wordRequest = new WordRequest();
                wordRequest.setEnglishWord(word);
                wordRequest.setMeaning(example);
                wordRequest.setExampleSentence(example);
                wordRequest.setPartOfSpeech(partOfSpeech);
                wordRequest.setLevel(mapLevel(level));
                wordRequest.setCategory(WordCategory.OTHER);
                
                ApiResponse<com.joblink.vocabulary.dto.response.WordResponse> createResponse = 
                        wordService.createWord(wordRequest);
                
                if (createResponse.getData() == null) {
                    return ApiResponse.error("Failed to create word: " + createResponse.getMessage());
                }
                
                wordEntity = wordRepository.findByEnglishWordIgnoreCase(word)
                        .orElseThrow(() -> new RuntimeException("Word not found after creation"));
            }
            
            // Add to user vocabulary if not already added
            Optional<UserVocabulary> existingUserVocab = 
                    userVocabularyRepository.findByUserIdAndWordId(userId, wordEntity.getId());
            
            UserVocabulary userVocabulary;
            if (existingUserVocab.isPresent()) {
                userVocabulary = existingUserVocab.get();
            } else {
                userVocabulary = UserVocabulary.builder()
                        .userId(userId)
                        .word(wordEntity)
                        .status(UserVocabulary.LearningStatus.LEARNING)
                        .reviewCount(0)
                        .correctCount(0)
                        .incorrectCount(0)
                        .masteryScore(0.0)
                        .build();
                
                sm2Service.initializeSM2Fields(userVocabulary);
                userVocabulary = userVocabularyRepository.save(userVocabulary);
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("word", wordEntity);
            result.put("userVocabulary", userVocabulary);
            
            return ApiResponse.success("Word saved successfully", result);
        } catch (Exception e) {
            log.error("Error saving context word", e);
            return ApiResponse.error("Failed to save word: " + e.getMessage());
        }
    }
    
    /**
     * Map CEFR level to WordLevel enum
     */
    private WordLevel mapLevel(String cefrLevel) {
        if (cefrLevel == null || cefrLevel.trim().isEmpty()) {
            return WordLevel.BEGINNER;
        }
        
        String level = cefrLevel.toUpperCase().trim();
        if (level.contains("A1") || level.contains("A2")) {
            return WordLevel.BEGINNER;
        } else if (level.contains("B1") || level.contains("B2")) {
            return WordLevel.INTERMEDIATE;
        } else if (level.contains("C1") || level.contains("C2")) {
            return WordLevel.ADVANCED;
        } else {
            // Default to BEGINNER for unknown levels
            return WordLevel.BEGINNER;
        }
    }
}

