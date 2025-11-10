package com.joblink.vocabulary.controller;

import com.joblink.vocabulary.dto.request.QuizRequest;
import com.joblink.vocabulary.dto.response.ApiResponse;
import com.joblink.vocabulary.dto.response.QuizResponse;
import com.joblink.vocabulary.service.QuizService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/vocabulary/users/{userId}/quiz")
@RequiredArgsConstructor
public class QuizController {
    
    private final QuizService quizService;
    
    @PostMapping("/generate")
    public ApiResponse<QuizResponse> generateQuiz(
            @PathVariable Long userId,
            @Valid @RequestBody QuizRequest request) {
        return quizService.generateQuiz(userId, request);
    }
}

