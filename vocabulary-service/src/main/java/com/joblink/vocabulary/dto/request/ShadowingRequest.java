package com.joblink.vocabulary.dto.request;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class ShadowingRequest {
    /**
     * Audio file containing user's speech
     */
    private MultipartFile audioFile;
    
    /**
     * Expected text that user should have spoken
     */
    private String expectedText;
    
    /**
     * Language code (default: "en" for English)
     */
    private String language = "en";
    
    /**
     * Word timestamps flag
     */
    private Boolean wordTimestamps = true;
}

