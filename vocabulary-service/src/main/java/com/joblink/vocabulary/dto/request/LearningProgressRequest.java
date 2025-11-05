package com.joblink.vocabulary.dto.request;

import com.joblink.vocabulary.model.entity.LearningProgress;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LearningProgressRequest {
    @NotNull(message = "Word ID is required")
    private Long wordId;
    
    @NotNull(message = "Progress type is required")
    private LearningProgress.ProgressType progressType;
    
    @NotNull(message = "Is correct is required")
    private Boolean isCorrect;
    
    private Integer timeSpentSeconds;
    private String notes;
}

