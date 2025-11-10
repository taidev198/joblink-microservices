package com.joblink.vocabulary.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class OxfordWordEntry {
    @JsonProperty("word")
    private String word;
    
    @JsonProperty("type")
    private String type;
    
    @JsonProperty("level")
    private String level;
}

