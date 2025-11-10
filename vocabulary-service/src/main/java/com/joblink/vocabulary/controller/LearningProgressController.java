package com.joblink.vocabulary.controller;

import com.joblink.vocabulary.dto.request.LearningProgressRequest;
import com.joblink.vocabulary.dto.response.ApiResponse;
import com.joblink.vocabulary.model.entity.LearningProgress;
import com.joblink.vocabulary.service.LearningProgressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/vocabulary/users/{userId}/progress")
@RequiredArgsConstructor
public class LearningProgressController {
    
    private final LearningProgressService learningProgressService;
    
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<LearningProgress> recordProgress(
            @PathVariable Long userId,
            @Valid @RequestBody LearningProgressRequest request) {
        return learningProgressService.recordProgress(userId, request);
    }
    
    @GetMapping
    public ApiResponse<Page<LearningProgress>> getUserProgress(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return learningProgressService.getUserProgress(userId, pageable);
    }
    
    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> getUserProgressStats(@PathVariable Long userId) {
        return learningProgressService.getUserProgressStats(userId);
    }
}

