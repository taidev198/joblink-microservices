package com.autocareerbridge.jobmatch.ai.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobMatchRequest {
    @NotNull
    private Integer studentId;
    
    private List<Integer> fieldIds;
    
    private String location;
    
    private String jobLevel;
    
    private String jobType;
    
    private Double minSalary;
    
    private Integer maxResults;
}