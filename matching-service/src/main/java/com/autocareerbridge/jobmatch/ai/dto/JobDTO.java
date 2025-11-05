package com.autocareerbridge.jobmatch.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobDTO {
    private Integer id;
    private String jobTitle;
    private String jobDescription;
    private String requirements;
    private String workTime;
    private String benefits;
    private String jobLevel;
    private Integer memberOfCandidate;
    private List<String> fields;
    private LocalDate expirationDate;
    private CompanyDTO company;
    private String jobType;
    private String jobStatus;
    private String salaryType;
    private Double maxSalary;
    private Double minSalary;
}