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
public class StudentDTO {
    private Integer id;
    private String fullName;
    private String email;
    private String phoneNumber;
    private LocalDate dateOfBirth;
    private String gender;
    private String avatar;
    private String major;
    private String university;
    private String faculty;
    private List<SkillDTO> skills;
    private List<ExperienceDTO> experiences;
    private List<EducationDTO> educations;
    private String resumeUrl;
}