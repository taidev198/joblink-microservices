package com.joblink.vocabulary.service;

import com.joblink.vocabulary.dto.request.WordRequest;
import com.joblink.vocabulary.dto.response.ApiResponse;
import com.joblink.vocabulary.dto.response.WordResponse;
import com.joblink.vocabulary.mapper.WordMapper;
import com.joblink.vocabulary.model.entity.Word;
import com.joblink.vocabulary.repository.WordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WordService {
    
    private final WordRepository wordRepository;
    private final WordMapper wordMapper;
    
    @Transactional
    public ApiResponse<WordResponse> createWord(WordRequest request) {
        // Check if word already exists
        if (wordRepository.findByEnglishWordIgnoreCase(request.getEnglishWord()).isPresent()) {
            return ApiResponse.error("Word already exists");
        }
        
        Word word = Word.builder()
                .englishWord(request.getEnglishWord())
                .meaning(request.getMeaning())
                .pronunciation(request.getPronunciation())
                .exampleSentence(request.getExampleSentence())
                .translation(request.getTranslation())
                .level(request.getLevel())
                .category(request.getCategory())
                .partOfSpeech(request.getPartOfSpeech())
                .synonyms(request.getSynonyms())
                .antonyms(request.getAntonyms())
                .imageUrl(request.getImageUrl())
                .audioUrl(request.getAudioUrl())
                .isActive(true)
                .build();
        
        Word savedWord = wordRepository.save(word);
        return ApiResponse.success("Word created successfully", wordMapper.toResponse(savedWord));
    }
    
    public ApiResponse<WordResponse> getWordById(Long id) {
        Word word = wordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Word not found with id: " + id));
        return ApiResponse.success(wordMapper.toResponse(word));
    }
    
    public ApiResponse<Page<WordResponse>> getAllWords(Pageable pageable) {
        Page<Word> words = wordRepository.findByIsActiveTrue(pageable);
        Page<WordResponse> wordResponses = words.map(wordMapper::toResponse);
        return ApiResponse.success(wordResponses);
    }
    
    public ApiResponse<Page<WordResponse>> getWordsByLevel(Word.WordLevel level, Pageable pageable) {
        Page<Word> words = wordRepository.findByLevel(level, pageable);
        Page<WordResponse> wordResponses = words.map(wordMapper::toResponse);
        return ApiResponse.success(wordResponses);
    }
    
    public ApiResponse<Page<WordResponse>> getWordsByCategory(Word.WordCategory category, Pageable pageable) {
        Page<Word> words = wordRepository.findByCategory(category, pageable);
        Page<WordResponse> wordResponses = words.map(wordMapper::toResponse);
        return ApiResponse.success(wordResponses);
    }
    
    public ApiResponse<Page<WordResponse>> searchWords(String keyword, Pageable pageable) {
        Page<Word> words = wordRepository.searchWords(keyword, pageable);
        Page<WordResponse> wordResponses = words.map(wordMapper::toResponse);
        return ApiResponse.success(wordResponses);
    }
    
    @Transactional
    public ApiResponse<WordResponse> updateWord(Long id, WordRequest request) {
        Word word = wordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Word not found with id: " + id));
        
        word.setEnglishWord(request.getEnglishWord());
        word.setMeaning(request.getMeaning());
        word.setPronunciation(request.getPronunciation());
        word.setExampleSentence(request.getExampleSentence());
        word.setTranslation(request.getTranslation());
        word.setLevel(request.getLevel());
        word.setCategory(request.getCategory());
        word.setPartOfSpeech(request.getPartOfSpeech());
        word.setSynonyms(request.getSynonyms());
        word.setAntonyms(request.getAntonyms());
        word.setImageUrl(request.getImageUrl());
        word.setAudioUrl(request.getAudioUrl());
        
        Word updatedWord = wordRepository.save(word);
        return ApiResponse.success("Word updated successfully", wordMapper.toResponse(updatedWord));
    }
    
    @Transactional
    public ApiResponse<Void> deleteWord(Long id) {
        Word word = wordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Word not found with id: " + id));
        word.setIsActive(false);
        wordRepository.save(word);
        return ApiResponse.success("Word deleted successfully", null);
    }
    
    public ApiResponse<List<WordResponse>> getWordsByIds(List<Long> wordIds) {
        List<Word> words = wordRepository.findByIds(wordIds);
        return ApiResponse.success(wordMapper.toResponseList(words));
    }
}

