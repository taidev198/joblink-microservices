package com.autocareerbridge.jobmatch.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EducationDTO {
    private Integer id;
    private String institution;
    private String degree;
    private String fieldOfStudy;
    private Double gpa;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean currentlyStudying;
}