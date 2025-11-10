package com.joblink.vocabulary.controller;

import com.joblink.vocabulary.dto.request.ShadowingRequest;
import com.joblink.vocabulary.dto.response.ApiResponse;
import com.joblink.vocabulary.dto.response.ShadowingResponse;
import com.joblink.vocabulary.service.ShadowingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/vocabulary/shadowing")
@RequiredArgsConstructor
public class ShadowingController {
    
    private final ShadowingService shadowingService;
    
    /**
     * Perform shadowing practice: transcribe audio and compare with expected text
     * 
     * @param audioFile Audio file containing user's speech
     * @param expectedText Expected text that user should have spoken
     * @param language Language code (default: "en")
     * @param wordTimestamps Whether to include word timestamps (default: true)
     * @return ShadowingResponse with transcription and comparison results
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<ShadowingResponse> performShadowing(
            @RequestPart("audioFile") MultipartFile audioFile,
            @RequestPart("expectedText") String expectedText,
            @RequestParam(required = false, defaultValue = "en") String language,
            @RequestParam(required = false, defaultValue = "true") Boolean wordTimestamps) {
        
        ShadowingRequest request = new ShadowingRequest();
        request.setAudioFile(audioFile);
        request.setExpectedText(expectedText);
        request.setLanguage(language);
        request.setWordTimestamps(wordTimestamps);
        
        ShadowingResponse response = shadowingService.performShadowing(request);
        return ApiResponse.success("Shadowing practice completed", response);
    }
    
    /**
     * Compare two texts without audio transcription
     * Useful for testing the comparison algorithm
     * 
     * @param transcribedText The transcribed text
     * @param expectedText The expected text
     * @return ShadowingResponse with comparison results
     */
    @PostMapping("/compare")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<ShadowingResponse> compareTexts(
            @RequestParam String transcribedText,
            @RequestParam String expectedText) {
        
        ShadowingResponse response = shadowingService.compareTexts(transcribedText, expectedText);
        return ApiResponse.success("Text comparison completed", response);
    }
}

