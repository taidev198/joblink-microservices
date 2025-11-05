package com.autocareerbridge.jobmatch.ai.client;

import com.autocareerbridge.jobmatch.ai.dto.JobDTO;
import com.autocareerbridge.jobmatch.ai.dto.StudentDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(name = "joblink-service", url = "${joblink.service.url}")
public interface JobLinkClient {

    @GetMapping("/api/ai/jobs")
    Map<String, Object> getAllJobs(
            @RequestParam(value = "status", defaultValue = "APPROVED") String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "100") int size
    );

    @GetMapping("/api/ai/students/{studentId}")
    Map<String, Object> getStudentById(@PathVariable("studentId") Integer studentId);

    @GetMapping("/api/ai/jobs/{jobId}")
    Map<String, Object> getJobById(@PathVariable("jobId") Integer jobId);

    @GetMapping("/api/ai/fields")
    Map<String, Object> getAllFields();
}