package com.joblink.vocabulary.service;

import com.joblink.vocabulary.dto.request.QuizRequest;
import com.joblink.vocabulary.dto.response.ApiResponse;
import com.joblink.vocabulary.dto.response.QuizResponse;
import com.joblink.vocabulary.dto.response.WordResponse;
import com.joblink.vocabulary.mapper.WordMapper;
import com.joblink.vocabulary.model.entity.Word;
import com.joblink.vocabulary.model.entity.WordLevel;
import com.joblink.vocabulary.repository.WordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuizService {
    
    private final WordRepository wordRepository;
    private final WordMapper wordMapper;
    
    public ApiResponse<QuizResponse> generateQuiz(Long userId, QuizRequest request) {
        List<Word> words;
        
        if (request.getWordIds() != null && !request.getWordIds().isEmpty()) {
            words = wordRepository.findByIds(request.getWordIds());
        } else {
            // Get random words based on difficulty
            words = getRandomWords(request.getDifficulty(), 
                    request.getNumberOfQuestions() != null ? request.getNumberOfQuestions() : 10);
        }
        
        if (words.isEmpty()) {
            return ApiResponse.error("No words available for quiz");
        }
        
        List<QuizResponse.QuizQuestion> questions = words.stream()
                .map(this::createQuestion)
                .collect(Collectors.toList());
        
        QuizResponse quiz = QuizResponse.builder()
                .quizId(System.currentTimeMillis()) // Simple ID generation
                .questions(questions)
                .totalQuestions(questions.size())
                .build();
        
        return ApiResponse.success(quiz);
    }
    
    private QuizResponse.QuizQuestion createQuestion(Word word) {
        // Get random words for options (wrong answers)
        List<Word> allWords = wordRepository.findAll();
        List<Word> wrongAnswers = allWords.stream()
                .filter(w -> !w.getId().equals(word.getId()) && w.getIsActive())
                .collect(Collectors.toList());
        
        Collections.shuffle(wrongAnswers);
        int wrongAnswerCount = Math.min(3, wrongAnswers.size());
        List<Word> optionsWords = new ArrayList<>(wrongAnswers.subList(0, wrongAnswerCount));
        optionsWords.add(word); // Add correct answer
        Collections.shuffle(optionsWords);
        
        List<String> options = optionsWords.stream()
                .map(Word::getMeaning)
                .collect(Collectors.toList());
        
        int correctIndex = optionsWords.indexOf(word);
        
        return QuizResponse.QuizQuestion.builder()
                .wordId(word.getId())
                .question(word.getEnglishWord())
                .options(options)
                .correctAnswerIndex(correctIndex)
                .word(wordMapper.toResponse(word))
                .build();
    }
    
    private List<Word> getRandomWords(String difficulty, int count) {
        WordLevel level = switch (difficulty != null ? difficulty.toUpperCase() : "MEDIUM") {
            case "EASY" -> WordLevel.BEGINNER;
            case "HARD" -> WordLevel.ADVANCED;
            default -> WordLevel.INTERMEDIATE;
        };
        
        // Get words by level, filter active ones, then get more than needed for randomization
        List<Word> words = wordRepository.findByLevel(level, 
                org.springframework.data.domain.PageRequest.of(0, Math.max(count * 3, 50)))
                .getContent()
                .stream()
                .filter(Word::getIsActive)
                .collect(Collectors.toList());
        
        if (words.isEmpty()) {
            return Collections.emptyList();
        }
        
        Collections.shuffle(words);
        int actualCount = Math.min(count, words.size());
        return words.subList(0, actualCount);
    }
}

