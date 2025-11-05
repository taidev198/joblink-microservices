package com.autocareerbridge.jobmatch.ai.service.impl;

import com.autocareerbridge.jobmatch.ai.client.JobLinkClient;
import com.autocareerbridge.jobmatch.ai.dto.JobDTO;
import com.autocareerbridge.jobmatch.ai.dto.SkillDTO;
import com.autocareerbridge.jobmatch.ai.dto.StudentDTO;
import com.autocareerbridge.jobmatch.ai.dto.request.JobMatchRequest;
import com.autocareerbridge.jobmatch.ai.dto.response.JobMatchResponse;
import com.autocareerbridge.jobmatch.ai.dto.response.JobMatchResponse.JobMatchResult;
import com.autocareerbridge.jobmatch.ai.service.JobMatchService;
import com.autocareerbridge.jobmatch.ai.service.NLPService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobMatchServiceImpl implements JobMatchService {

    private final JobLinkClient jobLinkClient;
    private final NLPService nlpService;
    private final ObjectMapper objectMapper;

    @Value("${ai.model.similarity.threshold:0.75}")
    private double similarityThreshold;

    @Value("${ai.model.max-recommendations:10}")
    private int defaultMaxRecommendations;

    @Override
    public JobMatchResponse matchJobsForStudent(JobMatchRequest request) {
        try {
            // Get student data from main service
            Map<String, Object> studentResponse = jobLinkClient.getStudentById(request.getStudentId());
            StudentDTO student = objectMapper.convertValue(studentResponse.get("data"), StudentDTO.class);
            
            if (student == null) {
                return JobMatchResponse.builder()
                        .matches(Collections.emptyList())
                        .build();
            }
            
            // Get all available jobs
            Map<String, Object> jobsResponse = jobLinkClient.getAllJobs("APPROVED", 0, 1000);
            List<JobDTO> allJobs = objectMapper.convertValue(jobsResponse.get("data"), List.class);
            
            // Extract student skills
            List<String> studentSkills = student.getSkills().stream()
                    .map(SkillDTO::getSkillName)
                    .collect(Collectors.toList());
            
            // Extract student experience keywords
            List<String> experienceKeywords = extractExperienceKeywords(student);
            
            // Combine all student profile keywords
            List<String> studentKeywords = new ArrayList<>(studentSkills);
            studentKeywords.addAll(experienceKeywords);
            studentKeywords.add(student.getMajor());
            
            // Filter jobs based on request criteria
            List<JobDTO> filteredJobs = filterJobs(allJobs, request);
            
            // Calculate match scores for each job
            List<JobMatchResult> matchResults = new ArrayList<>();
            
            for (JobDTO job : filteredJobs) {
                // Extract job requirements
                List<String> jobRequirements = extractJobRequirements(job);
                
                // Calculate similarity score
                double score = calculateMatchScore(studentKeywords, jobRequirements);
                
                // Find matching and missing skills
                List<String> matchingSkills = findMatchingSkills(studentSkills, jobRequirements);
                List<String> missingSkills = findMissingSkills(studentSkills, jobRequirements);
                
                // Generate match reason
                String matchReason = generateMatchReason(score, matchingSkills, missingSkills);
                
                // Add to results if score is above threshold
                if (score >= similarityThreshold) {
                    matchResults.add(JobMatchResult.builder()
                            .job(job)
                            .matchScore(score)
                            .matchingSkills(matchingSkills)
                            .missingSkills(missingSkills)
                            .matchReason(matchReason)
                            .build());
                }
            }
            
            // Sort by match score (descending)
            matchResults.sort(Comparator.comparing(JobMatchResult::getMatchScore).reversed());
            
            // Limit results
            int maxResults = request.getMaxResults() != null ? request.getMaxResults() : defaultMaxRecommendations;
            if (matchResults.size() > maxResults) {
                matchResults = matchResults.subList(0, maxResults);
            }
            
            return JobMatchResponse.builder()
                    .matches(matchResults)
                    .build();
            
        } catch (Exception e) {
            log.error("Error matching jobs for student: {}", e.getMessage(), e);
            return JobMatchResponse.builder()
                    .matches(Collections.emptyList())
                    .build();
        }
    }

    @Override
    public JobMatchResponse matchStudentsForJob(Integer jobId, Integer maxResults) {
        // Implementation for matching students to a job
        // This would be similar to the above but in reverse
        // For now, return empty response
        return JobMatchResponse.builder()
                .matches(Collections.emptyList())
                .build();
    }
    
    private List<String> extractExperienceKeywords(StudentDTO student) {
        // Extract keywords from student's experience
        List<String> keywords = new ArrayList<>();
        
        if (student.getExperiences() != null) {
            student.getExperiences().forEach(exp -> {
                keywords.add(exp.getPosition());
                keywords.add(exp.getCompanyName());
                
                // Use NLP to extract keywords from description
                if (exp.getDescription() != null && !exp.getDescription().isEmpty()) {
                    keywords.addAll(nlpService.extractKeywords(exp.getDescription()));
                }
            });
        }
        
        return keywords;
    }
    
    private List<String> extractJobRequirements(JobDTO job) {
        List<String> requirements = new ArrayList<>();
        
        // Add job title
        requirements.add(job.getJobTitle());
        
        // Add fields
        if (job.getFields() != null) {
            requirements.addAll(job.getFields());
        }
        
        // Extract keywords from job description and requirements
        if (job.getJobDescription() != null) {
            requirements.addAll(nlpService.extractKeywords(job.getJobDescription()));
        }
        
        if (job.getRequirements() != null) {
            requirements.addAll(nlpService.extractKeywords(job.getRequirements()));
        }
        
        return requirements;
    }
    
    private double calculateMatchScore(List<String> studentKeywords, List<String> jobRequirements) {
        // Calculate semantic similarity between student profile and job requirements
        return nlpService.calculateSimilarity(studentKeywords, jobRequirements);
    }
    
    private List<String> findMatchingSkills(List<String> studentSkills, List<String> jobRequirements) {
        return studentSkills.stream()
                .filter(skill -> jobRequirements.stream()
                        .anyMatch(req -> nlpService.calculateWordSimilarity(skill, req) > similarityThreshold))
                .collect(Collectors.toList());
    }
    
    private List<String> findMissingSkills(List<String> studentSkills, List<String> jobRequirements) {
        return jobRequirements.stream()
                .filter(req -> studentSkills.stream()
                        .noneMatch(skill -> nlpService.calculateWordSimilarity(skill, req) > similarityThreshold))
                .collect(Collectors.toList());
    }
    
    private String generateMatchReason(double score, List<String> matchingSkills, List<String> missingSkills) {
        StringBuilder reason = new StringBuilder();
        
        if (score >= 0.9) {
            reason.append("Excellent match! ");
        } else if (score >= 0.8) {
            reason.append("Very good match. ");
        } else if (score >= 0.7) {
            reason.append("Good match. ");
        } else {
            reason.append("Potential match. ");
        }
        
        if (!matchingSkills.isEmpty()) {
            reason.append("You have relevant skills: ")
                  .append(String.join(", ", matchingSkills.subList(0, Math.min(3, matchingSkills.size()))))
                  .append(". ");
        }
        
        if (!missingSkills.isEmpty()) {
            reason.append("Consider developing: ")
                  .append(String.join(", ", missingSkills.subList(0, Math.min(3, missingSkills.size()))))
                  .append(".");
        }
        
        return reason.toString();
    }
    
    private List<JobDTO> filterJobs(List<JobDTO> allJobs, JobMatchRequest request) {
        return allJobs.stream()
                .filter(job -> {
                    // Filter by job level if specified
                    if (request.getJobLevel() != null && !request.getJobLevel().isEmpty() && 
                        !job.getJobLevel().equalsIgnoreCase(request.getJobLevel())) {
                        return false;
                    }
                    
                    // Filter by job type if specified
                    if (request.getJobType() != null && !request.getJobType().isEmpty() && 
                        !job.getJobType().equalsIgnoreCase(request.getJobType())) {
                        return false;
                    }
                    
                    // Filter by minimum salary if specified
                    if (request.getMinSalary() != null && job.getMinSalary() != null && 
                        job.getMinSalary() < request.getMinSalary()) {
                        return false;
                    }
                    
                    // Filter by location if specified
                    if (request.getLocation() != null && !request.getLocation().isEmpty() && 
                        job.getCompany() != null && job.getCompany().getAddress() != null) {
                        String jobLocation = job.getCompany().getAddress().getProvinceName();
                        if (!jobLocation.toLowerCase().contains(request.getLocation().toLowerCase())) {
                            return false;
                        }
                    }
                    
                    // Filter by fields if specified
                    if (request.getFieldIds() != null && !request.getFieldIds().isEmpty()) {
                        // This is a simplified check - in reality, you'd need to match field IDs
                        return true;
                    }
                    
                    return true;
                })
                .collect(Collectors.toList());
    }
}