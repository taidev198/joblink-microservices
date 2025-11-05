package com.autocareerbridge.jobmatch.ai.controller;

import com.autocareerbridge.jobmatch.ai.dto.request.JobMatchRequest;
import com.autocareerbridge.jobmatch.ai.dto.response.ApiResponse;
import com.autocareerbridge.jobmatch.ai.dto.response.JobMatchResponse;
import com.autocareerbridge.jobmatch.ai.service.JobMatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/match")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Job Matching", description = "API endpoints for AI/ML job matching")
public class JobMatchController {

    private final JobMatchService jobMatchService;

    @PostMapping("/jobs-for-student")
    @Operation(summary = "Match jobs for a student", 
               description = "Find suitable jobs for a student based on their profile and preferences")
    public ApiResponse<JobMatchResponse> matchJobsForStudent(@Valid @RequestBody JobMatchRequest request) {
        try {
            JobMatchResponse response = jobMatchService.matchJobsForStudent(request);
            return ApiResponse.success("Jobs matched successfully", response);
        } catch (Exception e) {
            log.error("Error matching jobs for student: {}", e.getMessage(), e);
            return ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), 
                    "Error matching jobs: " + e.getMessage());
        }
    }

    @GetMapping("/students-for-job/{jobId}")
    @Operation(summary = "Match students for a job", 
               description = "Find suitable students for a specific job")
    public ApiResponse<JobMatchResponse> matchStudentsForJob(
            @PathVariable Integer jobId,
            @RequestParam(required = false, defaultValue = "10") Integer maxResults) {
        try {
            JobMatchResponse response = jobMatchService.matchStudentsForJob(jobId, maxResults);
            return ApiResponse.success("Students matched successfully", response);
        } catch (Exception e) {
            log.error("Error matching students for job: {}", e.getMessage(), e);
            return ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), 
                    "Error matching students: " + e.getMessage());
        }
    }
}