package com.joblink.vocabulary.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
@Slf4j
public class PDFExtractionService {

    public List<WordData> extractWordsFromPDF(String pdfPath) throws IOException {
        List<WordData> words = new ArrayList<>();
        
        try (PDDocument document = Loader.loadPDF(new File(pdfPath))) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            
            log.info("Extracted text from PDF, length: {}", text.length());
            
            // Parse the text to extract words
            words = parseTextToWords(text);
            
            log.info("Extracted {} words from PDF", words.size());
        }
        
        return words;
    }

    private List<WordData> parseTextToWords(String text) {
        List<WordData> words = new ArrayList<>();
        
        // Split by lines and process each line
        String[] lines = text.split("\n");
        
        String currentWord = null;
        String currentMeaning = null;
        String currentExample = null;
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) {
                // Empty line might indicate end of current word entry
                if (currentWord != null && !currentWord.isEmpty()) {
                    words.add(WordData.builder()
                            .englishWord(currentWord.trim())
                            .meaning(currentMeaning != null ? currentMeaning.trim() : "")
                            .exampleSentence(currentExample != null ? currentExample.trim() : "")
                            .build());
                    currentWord = null;
                    currentMeaning = null;
                    currentExample = null;
                }
                continue;
            }
            
            // Pattern 1: Line contains both word and meaning separated by dash (common format: "word - meaning")
            if (line.contains(" - ") || line.contains("–") || line.contains("—") || line.contains(":")) {
                String[] parts = line.split("[-–—:]", 2);
                if (parts.length >= 2) {
                    String wordPart = parts[0].trim();
                    String meaningPart = parts[1].trim();
                    
                    // Save previous word if exists
                    if (currentWord != null && !currentWord.isEmpty()) {
                        words.add(WordData.builder()
                                .englishWord(currentWord.trim())
                                .meaning(currentMeaning != null ? currentMeaning.trim() : "")
                                .exampleSentence(currentExample != null ? currentExample.trim() : "")
                                .build());
                    }
                    
                    if (isWordLine(wordPart)) {
                        currentWord = wordPart;
                        currentMeaning = meaningPart;
                        currentExample = null;
                        continue;
                    }
                }
            }
            
            // Pattern 2: Check if line is a word (usually contains only English letters, spaces, hyphens, apostrophes)
            if (isWordLine(line)) {
                // Save previous word if exists
                if (currentWord != null && !currentWord.isEmpty()) {
                    words.add(WordData.builder()
                            .englishWord(currentWord.trim())
                            .meaning(currentMeaning != null ? currentMeaning.trim() : "")
                            .exampleSentence(currentExample != null ? currentExample.trim() : "")
                            .build());
                }
                
                // Start new word
                currentWord = line;
                currentMeaning = null;
                currentExample = null;
            }
            // Pattern 3: Check if line is Vietnamese (contains Vietnamese characters)
            else if (isVietnameseLine(line) && currentWord != null) {
                if (currentMeaning == null) {
                    currentMeaning = line;
                } else {
                    // Append to meaning if it continues
                    currentMeaning += " " + line;
                }
            }
            // Pattern 4: Check if line is an example sentence (contains the word and is a sentence)
            else if (isExampleSentence(line, currentWord)) {
                if (currentExample == null) {
                    currentExample = line;
                } else {
                    currentExample += " " + line;
                }
            }
            // Pattern 5: If current word exists and line doesn't match any pattern, it might be continuation
            else if (currentWord != null && !isWordLine(line) && !isVietnameseLine(line)) {
                // Check if it could be an example or meaning continuation
                if (line.length() > 5 && line.length() < 150) {
                    if (currentExample == null && line.contains(currentWord.toLowerCase())) {
                        currentExample = line;
                    } else if (currentMeaning == null) {
                        currentMeaning = line;
                    }
                }
            }
        }
        
        // Add the last word
        if (currentWord != null && !currentWord.isEmpty()) {
            words.add(WordData.builder()
                    .englishWord(currentWord.trim())
                    .meaning(currentMeaning != null ? currentMeaning.trim() : "")
                    .exampleSentence(currentExample != null ? currentExample.trim() : "")
                    .build());
        }
        
        return words;
    }

    private boolean isWordLine(String line) {
        // Word line usually:
        // - Contains only English letters, spaces, hyphens, apostrophes
        // - Not too long (usually < 50 chars)
        // - May start with a number (for numbered lists)
        if (line.length() > 100) {
            return false;
        }
        
        // Remove leading numbers and dots
        String cleaned = line.replaceFirst("^\\d+\\.?\\s*", "").trim();
        
        // Check if it's mostly English characters
        Pattern englishPattern = Pattern.compile("^[a-zA-Z\\s\\-'’]+$");
        return englishPattern.matcher(cleaned).matches() && cleaned.length() > 0 && cleaned.length() < 50;
    }

    private boolean isVietnameseLine(String line) {
        // Vietnamese text contains Vietnamese characters
        Pattern vietnamesePattern = Pattern.compile(".*[àáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđĐ].*");
        return vietnamesePattern.matcher(line).matches();
    }

    private boolean isExampleSentence(String line, String word) {
        if (word == null || word.isEmpty()) {
            return false;
        }
        
        // Example sentence usually:
        // - Starts with capital letter
        // - Contains the word (case insensitive)
        // - Ends with punctuation
        // - Is a complete sentence (has spaces)
        
        if (line.length() < 10 || line.length() > 200) {
            return false;
        }
        
        String lowerLine = line.toLowerCase();
        String lowerWord = word.toLowerCase().trim();
        
        // Check if line contains the word
        boolean containsWord = lowerLine.contains(lowerWord);
        
        // Check if it starts with capital letter
        boolean startsWithCapital = Character.isUpperCase(line.charAt(0));
        
        // Check if it ends with punctuation
        boolean endsWithPunctuation = line.matches(".*[.!?]$");
        
        // Check if it has spaces (is a sentence)
        boolean hasSpaces = line.contains(" ");
        
        return containsWord && startsWithCapital && (endsWithPunctuation || hasSpaces);
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class WordData {
        private String englishWord;
        private String meaning; // Vietnamese meaning
        private String exampleSentence;
    }
}

