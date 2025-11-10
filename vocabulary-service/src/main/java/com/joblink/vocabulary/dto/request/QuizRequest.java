package com.joblink.vocabulary.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class QuizRequest {
    private List<Long> wordIds; // Optional: if provided, use these words; otherwise generate random quiz
    
    private Integer numberOfQuestions;
    private String difficulty; // EASY, MEDIUM, HARD
}

