package com.joblink.vocabulary.service;

import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Base64;

@Service
@Slf4j
public class TTSService {

    @Value("${tts.storage.path:${user.home}/audio}")
    private String storagePath;
    private static final String AUDIO_DIR = "audio_files";
    private static final String TTS_URL = "https://ttsmp3.com/mp3/";
    
    private final RestTemplate restTemplate;
    
    public TTSService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    @PostConstruct
    public void init() {
        // Resolve user.home if present in path
        if (storagePath != null && storagePath.contains("${user.home}")) {
            storagePath = storagePath.replace("${user.home}", System.getProperty("user.home"));
        }
        // If path is still not set or is empty, use default
        if (storagePath == null || storagePath.isEmpty()) {
            storagePath = System.getProperty("user.home") + "/audio";
        }
        
        // Ensure the directory exists and is writable
        try {
            Path dir = Paths.get(storagePath);
            Files.createDirectories(dir);
            log.info("Audio storage directory initialized at: {}", storagePath);
        } catch (IOException e) {
            log.error("Failed to create audio storage directory: {}", storagePath, e);
            // Fallback to temp directory
            storagePath = System.getProperty("java.io.tmpdir") + "/audio";
            try {
                Path dir = Paths.get(storagePath);
                Files.createDirectories(dir);
                log.info("Using fallback audio storage directory: {}", storagePath);
            } catch (IOException ex) {
                log.error("Failed to create fallback audio storage directory: {}", storagePath, ex);
            }
        }
    }

    private WebDriver createChromeDriver() {
        // Force WebDriverManager to detect Chrome version and download matching driver
        // For Chrome 131, we need ChromeDriver 131.x
        WebDriverManager.chromedriver()
                .clearResolutionCache()
                .setup();
        
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new", "--disable-gpu", "--no-sandbox", "--disable-dev-shm-usage");
        options.setBinary("/Applications/Google Chrome.app/Contents/MacOS/Google Chrome");
        // Disable CDP to avoid version mismatch warnings (CDP v131 may not be available yet)
        options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
        options.setExperimentalOption("useAutomationExtension", false);
        
        return new ChromeDriver(options);
    }

    public String fetchAndSaveVietnameseAudio(String text, String englishWord) {
        try {
            // CSRF token from curl command
            String csrfToken = "0spBW4HZO7lY2zoQeHsoVOGsGXksw9DNna02B6mM";
            
            // Prepare form data - only text parameter changes
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("_token", csrfToken);
            formData.add("languages", "Vietnamese");
            formData.add("voice", "vi-VN-HoaiMyNeural");
            formData.add("text", text);
            formData.add("recaptcha-response", "");
            
            // Set up headers exactly as in curl command
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            headers.set("accept", "application/json, text/javascript, */*; q=0.01");
            headers.set("accept-language", "en-US,en;q=0.9,vi-VN;q=0.8,vi;q=0.7,ko;q=0.6");
            headers.set("origin", "https://crikk.com");
            headers.set("priority", "u=1, i");
            headers.set("referer", "https://crikk.com/text-to-speech/vietnamese/");
            headers.set("sec-ch-ua", "\"Google Chrome\";v=\"131\", \"Chromium\";v=\"131\", \"Not_A Brand\";v=\"24\"");
            headers.set("sec-ch-ua-mobile", "?0");
            headers.set("sec-ch-ua-platform", "\"macOS\"");
            headers.set("sec-fetch-dest", "empty");
            headers.set("sec-fetch-mode", "cors");
            headers.set("sec-fetch-site", "same-origin");
            headers.set("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36");
            headers.set("x-csrf-token", csrfToken);
            headers.set("x-requested-with", "XMLHttpRequest");
            
            // Cookie header from curl command
            headers.set("cookie", "_ga=GA1.1.740327823.1762327894; __gads=ID=9b6d386bb30f7dcd:T=1762327893:RT=1762327893:S=ALNI_MbuFxOYPUhVlcKvpT9NRLYIwKwpog; __gpi=UID=000011ae498010aa:T=1762327893:RT=1762327893:S=ALNI_MaGCJKm-OcqaqEg8Un5kjEpwmDwkw; __eoi=ID=dd54b53ca56e42e0:T=1762327893:RT=1762327893:S=AA-AfjZc30rgUgkPwo3fLOajMF9J; _fbp=fb.1.1762327894136.931768944234588887; _clck=1xgbqnt%5E2%5Eg0r%5E0%5E2135; cw_conversation=eyJhbGciOiJIUzI1NiJ9.eyJzb3VyY2VfaWQiOiJlZGVlY2IyMS01MDcwLTRlMDctODcwZi0xNTE1NTI2ZTlhYWUiLCJpbmJveF9pZCI6NjU0MzAsImV4cCI6MTc3Nzg3OTg5NSwiaWF0IjoxNzYyMzI3ODk1fQ.EH481IUtrko9xb6j4HuoWnxJZ4SoY055CJN3XLlS1bs; __gsas=ID=aa5a47704312c8a1:T=1762327896:RT=1762327896:S=ALNI_MZ13ue4A9gizaVS4QEUljZB8hcLRQ; elementor_split_test_client_id=4e10c91b-5a4e-4c39-80c3-caa8b4f85839; _ga_0PRQTGTWRS=GS2.1.s1762327893$o1$g0$t1762327903$j50$l0$h0; _ga_KTV8S3RGJE=GS2.1.s1762327894$o1$g0$t1762327903$j51$l0$h0; XSRF-TOKEN=eyJpdiI6IkJKTlF5MkhlQU9reGpLSDVyTDA3ZGc9PSIsInZhbHVlIjoieGZZWituQWhpRXREWTVTYWdOSk54TFFYTDE5Wk9YeFFZZ2Fmd1VsU2lwVTZHUmlQT2ZJaVVnUHFsRmI5UFpNWml4MDNxUlZKb1E5Q3dGOTFaMldzSHRidmZrTlpuaXI4QzQydi9wbFcvTDcvSlNReVB5NnZmeUJIdFZjd1dYVlkiLCJtYWMiOiI1NTM5MDlkOTJkNjVlMWUwMGZhYWQzNWU4NjNlMjQxM2NlOGE2OTE0MzUyZGM1YmI4YmY0YTk4YTQyNjU0MzE3IiwidGFnIjoiIn0%3D; crikk_session=eyJpdiI6IkZTekNzNEFkMjVzYUFEUHBsL3ljanc9PSIsInZhbHVlIjoiR2lrREVlTGgxdmlUWVJuQnhTMEc0ZHdWdERIU3hZR0hYOU8zOXF2TkNRS0tNMXdHd21vcU44Tnh4SmY0bENNUThTMHJ2SWllRHo5OEw0RUNydktuNWd4TlczejU4UlR5aUNGOGRxUU9YaElWS1Evbjl4SXVYazVNLzV4cXZoYzUiLCJtYWMiOiI3YTQ5NWRlZmM0MDE3NTk4NDViZmYwZWIzMGUwYWFlYTgzMmM1ZTk1NmVmMzE4ZWNiZGVlNWIzNDMzYmNmODRhIiwidGFnIjoiIn0%3D; _clsk=1yyhkpe%5E1762335464473%5E1%5E1%5Ee.clarity.ms%2Fcollect");
            
            // Make the API request
            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(formData, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    "https://crikk.com/app/generate-audio-frontend",
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );
            
            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                throw new IOException("Failed to get audio. Status code: " + response.getStatusCode());
            }
            
            String responseBody = response.getBody();
            log.info("API response received, length: {}", responseBody.length());
            
            // Parse JSON response to extract base64 audio
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonResponse = objectMapper.readTree(responseBody);
            
            if (!jsonResponse.has("audio")) {
                throw new RuntimeException("API response does not contain audio field: " + responseBody);
            }
            
            String audioDataUri = jsonResponse.get("audio").asText();
            log.info("Audio data URI received, length: {}", audioDataUri.length());
            
            // Extract base64 data from data URI (format: data:audio/mpeg;base64,<base64data>)
            String base64Data;
            if (audioDataUri.startsWith("data:audio/mpeg;base64,")) {
                base64Data = audioDataUri.substring("data:audio/mpeg;base64,".length());
            } else if (audioDataUri.startsWith("data:audio/wav;base64,")) {
                base64Data = audioDataUri.substring("data:audio/wav;base64,".length());
            } else {
                // Try to extract base64 data after comma
                int commaIndex = audioDataUri.indexOf(',');
                if (commaIndex > 0) {
                    base64Data = audioDataUri.substring(commaIndex + 1);
                } else {
                    throw new RuntimeException("Invalid audio data URI format: " + audioDataUri.substring(0, Math.min(100, audioDataUri.length())));
                }
            }
            
            // Decode base64 to bytes
            byte[] audioBytes = Base64.getDecoder().decode(base64Data);
            log.info("Decoded audio bytes, size: {} bytes", audioBytes.length);
            
            // Save the audio file
            Path dir = Paths.get(storagePath, "vietnamese");
            Files.createDirectories(dir);
            String filename = englishWord.replace(" ", "_") + "_vi.mp3";
            Path filePath = dir.resolve(filename);
            Files.write(filePath, audioBytes);
            log.info("Saved Vietnamese audio file: {}", filePath);
            
            return "/audio/vietnamese/" + filename;
        } catch (Exception e) {
            log.error("Failed to fetch Vietnamese audio for text: {}", text, e);
            throw new RuntimeException("Failed to fetch Vietnamese audio for text: " + text, e);
        }
    }
    
    private static class CsrfTokenAndCookies {
        String csrfToken;
        String cookieString;
    }
    
    private String getCsrfTokenFromPage() {
        // Use Selenium briefly to get the CSRF token from the page
        WebDriver driver = createChromeDriver();
        try {
            driver.get("https://crikk.com/text-to-speech/vietnamese/");
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
            wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
            
            // Wait for JavaScript to finish loading
            wait.until(webDriver -> ((JavascriptExecutor) webDriver)
                    .executeScript("return document.readyState").equals("complete"));
            
            // Extract CSRF token from the page
            String csrfToken = (String) ((JavascriptExecutor) driver)
                    .executeScript("return document.querySelector('meta[name=\"csrf-token\"]')?.content || ''");
            
            // If CSRF token not found in meta, try to get it from form or cookie
            if (csrfToken == null || csrfToken.isEmpty()) {
                try {
                    WebElement tokenInput = driver.findElement(By.name("_token"));
                    csrfToken = tokenInput.getAttribute("value");
                } catch (Exception e) {
                    // Try to get from cookies
                    Cookie xsrfCookie = driver.manage().getCookieNamed("XSRF-TOKEN");
                    if (xsrfCookie != null) {
                        csrfToken = xsrfCookie.getValue();
                    }
                }
            }
            
            if (csrfToken == null || csrfToken.isEmpty()) {
                throw new RuntimeException("Could not extract CSRF token from page");
            }
            
            return csrfToken;
        } finally {
            driver.quit();
        }
    }

    public String fetchAndSaveEnglishAudio(String text) {
        try {
            Path dir = Paths.get(storagePath, "english");
            Files.createDirectories(dir);
            String filename = text.replaceAll("\\W+", "_") + "_en.mp3";
            Path filePath = dir.resolve(filename);
            java.io.File audioFile = filePath.toFile();
            
            // Create parent directories if needed
            if (!audioFile.getParentFile().exists()) {
                audioFile.getParentFile().mkdirs();
            }
            
            // Prepare form data for the first request
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("msg", text);
            formData.add("lang", "Kimberly");
            formData.add("source", "ttsmp3");
            
            // Set up headers for both requests
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("accept", "*/*");
            headers.set("accept-language", "en-US,en;q=0.9,vi-VN;q=0.8,vi;q=0.7,ko;q=0.6");
            headers.set("origin", "https://ttsmp3.com");
            headers.set("referer", "https://ttsmp3.com/");
            headers.set("sec-ch-ua", "\"Google Chrome\";v=\"131\", \"Chromium\";v=\"131\", \"Not_A Brand\";v=\"24\"");
            headers.set("sec-ch-ua-mobile", "?0");
            headers.set("sec-ch-ua-platform", "\"macOS\"");
            headers.set("sec-fetch-dest", "empty");
            headers.set("sec-fetch-mode", "cors");
            headers.set("sec-fetch-site", "same-origin");
            headers.set("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36");
            
            // First request to get the MP3 filename
            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(formData, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                "https://ttsmp3.com/makemp3_new.php",
                HttpMethod.POST,
                requestEntity,
                String.class
            );
            
            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                throw new IOException("Failed to get MP3 filename. Status code: " + response.getStatusCode());
            }
            
            // Extract the MP3 filename from the response
            // The response format should be something like: {"MP3":"filename.mp3"}
            String responseBody = response.getBody();
            String mp3Filename;
            if (responseBody.contains("\"MP3\"")) {
                int startIndex = responseBody.indexOf("\"MP3\"") + 7;
                int endIndex = responseBody.indexOf("\"", startIndex);
                mp3Filename = responseBody.substring(startIndex, endIndex);
            } else {
                throw new IOException("Invalid response format: " + responseBody);
            }
            
            // Second request to download the MP3 file
            HttpHeaders downloadHeaders = new HttpHeaders();
            downloadHeaders.set("accept", "*/*");
            downloadHeaders.set("accept-language", "en-US,en;q=0.9,vi-VN;q=0.8,vi;q=0.7,ko;q=0.6");
            downloadHeaders.set("referer", "https://ttsmp3.com/");
            downloadHeaders.set("sec-ch-ua", "\"Google Chrome\";v=\"131\", \"Chromium\";v=\"131\", \"Not_A Brand\";v=\"24\"");
            downloadHeaders.set("sec-ch-ua-mobile", "?0");
            downloadHeaders.set("sec-ch-ua-platform", "\"macOS\"");
            downloadHeaders.set("sec-fetch-dest", "empty");
            downloadHeaders.set("sec-fetch-mode", "cors");
            downloadHeaders.set("sec-fetch-site", "same-origin");
            downloadHeaders.set("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36");
            
            // Download the MP3 file
            RestTemplate downloadTemplate = new RestTemplate();
            downloadTemplate.execute(
                "https://ttsmp3.com/dlmp3.php?mp3=" + mp3Filename + "&location=direct",
                HttpMethod.GET,
                request -> request.getHeaders().addAll(downloadHeaders),
                downloadResponse -> {
                    try (FileOutputStream fos = new FileOutputStream(audioFile)) {
                        downloadResponse.getBody().transferTo(fos);
                    }
                    return null;
                }
            );
            
            // Verify the file was created and has content
            if (!audioFile.exists() || audioFile.length() == 0) {
                throw new IOException("Failed to download MP3 file or file is empty");
            }
            
            return "/audio/english/" + filename;
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch English audio for text: " + text, e);
        }
    }

    public String generateAudioFile(String text, String fileName) throws IOException {
        String filePath = AUDIO_DIR + "/" + fileName + ".mp3";

        File audioFile = createAudioFile(filePath);

        // Prepare form data for the first request
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("msg", text);
        formData.add("lang", "Joey");
        formData.add("source", "ttsmp3");

        // Set up headers for both requests
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("accept", "*/*");
        headers.set("accept-language", "en-US,en;q=0.9,vi-VN;q=0.8,vi;q=0.7,ko;q=0.6");
        headers.set("origin", "https://ttsmp3.com");
        headers.set("referer", "https://ttsmp3.com/");
        headers.set("sec-ch-ua", "\"Google Chrome\";v=\"131\", \"Chromium\";v=\"131\", \"Not_A Brand\";v=\"24\"");
        headers.set("sec-ch-ua-mobile", "?0");
        headers.set("sec-ch-ua-platform", "\"macOS\"");
        headers.set("sec-fetch-dest", "empty");
        headers.set("sec-fetch-mode", "cors");
        headers.set("sec-fetch-site", "same-origin");
        headers.set("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36");

        try {
            // First request to get the MP3 filename
            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(formData, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    "https://ttsmp3.com/makemp3_new.php",
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                throw new IOException("Failed to get MP3 filename. Status code: " + response.getStatusCode());
            }

            // Log the response for debugging
            String responseBody = response.getBody();
            log.info("Response from makemp3_new.php: {}", responseBody);

            // Extract the MP3 filename from the response
            // The response format should be something like: {"MP3":"filename.mp3"}
            String mp3Filename;
            if (responseBody.contains("\"MP3\"")) {
                int startIndex = responseBody.indexOf("\"MP3\"") + 7;
                int endIndex = responseBody.indexOf("\"", startIndex);
                mp3Filename = responseBody.substring(startIndex, endIndex);
            } else {
                throw new IOException("Invalid response format: " + responseBody);
            }

            log.info("Extracted MP3 filename: {}", mp3Filename);

            // Second request to download the MP3 file
            HttpHeaders downloadHeaders = new HttpHeaders();
            downloadHeaders.set("accept", "*/*");
            downloadHeaders.set("accept-language", "en-US,en;q=0.9,vi-VN;q=0.8,vi;q=0.7,ko;q=0.6");
            downloadHeaders.set("referer", "https://ttsmp3.com/");
            downloadHeaders.set("sec-ch-ua", "\"Google Chrome\";v=\"131\", \"Chromium\";v=\"131\", \"Not_A Brand\";v=\"24\"");
            downloadHeaders.set("sec-ch-ua-mobile", "?0");
            downloadHeaders.set("sec-ch-ua-platform", "\"macOS\"");
            downloadHeaders.set("sec-fetch-dest", "empty");
            downloadHeaders.set("sec-fetch-mode", "cors");
            downloadHeaders.set("sec-fetch-site", "same-origin");
            downloadHeaders.set("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36");

            // Create a new RestTemplate with a custom response extractor
            RestTemplate downloadTemplate = new RestTemplate();
            downloadTemplate.execute(
                    "https://ttsmp3.com/dlmp3.php?mp3=" + mp3Filename + "&location=direct",
                    HttpMethod.GET,
                    request -> request.getHeaders().addAll(downloadHeaders),
                    downloadResponse -> {
                        try (FileOutputStream fos = new FileOutputStream(audioFile)) {
                            downloadResponse.getBody().transferTo(fos);
                        }
                        return null;
                    }
            );

            // Verify the file was created and has content
            if (!audioFile.exists() || audioFile.length() == 0) {
                throw new IOException("Failed to download MP3 file or file is empty");
            }

            log.info("Successfully downloaded MP3 file: {} (size: {} bytes)", filePath, audioFile.length());
            return filePath;
        } catch (Exception e) {
            log.error("Error generating audio file: {}", e.getMessage());
            throw new IOException("Failed to generate audio file", e);
        }
    }

    private File createAudioFile(String filePath) throws IOException {
        File audioFile = new File(filePath);
        if (!audioFile.getParentFile().exists()) {
            audioFile.getParentFile().mkdirs();
        }
        return audioFile;
    }
}
