package com.joblink.vocabulary.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizResponse {
    private Long quizId;
    private List<QuizQuestion> questions;
    private Integer totalQuestions;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class QuizQuestion {
        private Long wordId;
        private String question;
        private List<String> options;
        private Integer correctAnswerIndex;
        private WordResponse word;
    }
}

