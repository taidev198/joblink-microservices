package com.autocareerbridge.jobmatch.ai.dto.response;

import com.autocareerbridge.jobmatch.ai.dto.JobDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobMatchResponse {
    private List<JobMatchResult> matches;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JobMatchResult {
        private JobDTO job;
        private double matchScore;
        private List<String> matchingSkills;
        private List<String> missingSkills;
        private String matchReason;
    }
}