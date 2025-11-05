package com.autocareerbridge.jobmatch.ai.service;

import com.autocareerbridge.jobmatch.ai.dto.request.JobMatchRequest;
import com.autocareerbridge.jobmatch.ai.dto.response.JobMatchResponse;

public interface JobMatchService {
    /**
     * Match jobs based on student profile and preferences
     * 
     * @param request The job match request containing student ID and preferences
     * @return JobMatchResponse containing matched jobs with scores
     */
    JobMatchResponse matchJobsForStudent(JobMatchRequest request);
    
    /**
     * Match students for a specific job
     * 
     * @param jobId The ID of the job
     * @param maxResults Maximum number of results to return
     * @return JobMatchResponse containing matched students with scores
     */
    JobMatchResponse matchStudentsForJob(Integer jobId, Integer maxResults);
}