package com.autocareerbridge.jobmatch.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyDTO {
    private Integer id;
    private String companyName;
    private String description;
    private String website;
    private String phoneNumber;
    private String email;
    private String logo;
    private AddressDTO address;
}