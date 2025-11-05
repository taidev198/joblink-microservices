package com.autocareerbridge.jobmatch.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressDTO {
    private Integer id;
    private String addressDetail;
    private String provinceName;
    private String districtName;
    private String wardName;
}