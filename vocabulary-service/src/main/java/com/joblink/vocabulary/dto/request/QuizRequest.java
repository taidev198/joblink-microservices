package com.joblink.vocabulary.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class QuizRequest {
    @NotNull(message = "Word IDs are required")
    private List<Long> wordIds;
    
    private Integer numberOfQuestions;
    private String difficulty; // EASY, MEDIUM, HARD
}

