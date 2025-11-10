package com.joblink.vocabulary.service;

import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class CambridgeDictionaryCrawler {
    
    private static final String BASE_URL = "https://dictionary.cambridge.org/vi/dictionary/english/";
    
    /**
     * Get word information from Cambridge Dictionary
     * @param word The word to look up
     * @return Map containing word information: ipa, definition, examples, audioUrl, vietnameseMeaning
     */
    public Map<String, Object> getWordInfo(String word) {
        WebDriver driver = null;
        Map<String, Object> result = new HashMap<>();
        try {
            // Set up Chrome options
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless=new");
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments("--disable-gpu");
            options.addArguments("--window-size=1920,1080");
            options.addArguments("--remote-allow-origins=*");
            
            // Use WebDriverManager to handle ChromeDriver
            WebDriverManager.chromedriver().setup();
            
            // Initialize the Chrome driver
            driver = new ChromeDriver(options);
            
            // Navigate to the word's page
            String url = BASE_URL + word.toLowerCase().trim();
            log.info("Navigating to Cambridge Dictionary URL: {}", url);
            driver.get(url);
            
            // Wait for the page to load
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
            
            // Wait for the page to be fully loaded
            wait.until(webDriver -> ((JavascriptExecutor) webDriver)
                .executeScript("return document.readyState").equals("complete"));
            
            // Get IPA (pronunciation)
            try {
                WebElement ipaElement = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("span.ipa")
                ));
                String ipa = ipaElement.getText().trim();
                result.put("ipa", ipa);
                log.info("Found IPA: {}", ipa);
            } catch (Exception e) {
                log.warn("Could not find IPA for word {}: {}", word, e.getMessage());
            }
            
            // Get definition (English)
            try {
                WebElement defElement = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("div.def.ddef_d.db")
                ));
                String definition = defElement.getText().trim();
                result.put("definition", definition);
                log.info("Found definition: {}", definition);
            } catch (Exception e) {
                log.warn("Could not find definition for word {}: {}", word, e.getMessage());
            }
            
            // Get Vietnamese meaning/translation
            try {
                // Try to find Vietnamese translation
                List<WebElement> vietnameseElements = driver.findElements(
                    By.cssSelector("span.trans.dtrans.dtrans-se")
                );
                if (!vietnameseElements.isEmpty()) {
                    String vietnameseMeaning = vietnameseElements.get(0).getText().trim();
                    result.put("vietnameseMeaning", vietnameseMeaning);
                    log.info("Found Vietnamese meaning: {}", vietnameseMeaning);
                }
            } catch (Exception e) {
                log.warn("Could not find Vietnamese meaning for word {}: {}", word, e.getMessage());
            }
            
            // Get examples (English) - try multiple selectors
            try {
                List<String> examples = new ArrayList<>();
                
                // Try primary selector: span.examp
                List<WebElement> exampleElements = driver.findElements(
                    By.cssSelector("span.examp")
                );
                for (WebElement example : exampleElements) {
                    String exampleText = example.getText().trim();
                    if (!exampleText.isEmpty() && !examples.contains(exampleText)) {
                        examples.add(exampleText);
                    }
                }
                
                // If no examples found, try alternative selectors
                if (examples.isEmpty()) {
                    // Try: span.eg
                    exampleElements = driver.findElements(By.cssSelector("span.eg"));
                    for (WebElement example : exampleElements) {
                        String exampleText = example.getText().trim();
                        if (!exampleText.isEmpty() && !examples.contains(exampleText)) {
                            examples.add(exampleText);
                        }
                    }
                }
                
                // Try: div.examp
                if (examples.isEmpty()) {
                    exampleElements = driver.findElements(By.cssSelector("div.examp"));
                    for (WebElement example : exampleElements) {
                        String exampleText = example.getText().trim();
                        if (!exampleText.isEmpty() && !examples.contains(exampleText)) {
                            examples.add(exampleText);
                        }
                    }
                }
                
                // Try: span[class*="examp"]
                if (examples.isEmpty()) {
                    exampleElements = driver.findElements(By.cssSelector("span[class*='examp']"));
                    for (WebElement example : exampleElements) {
                        String exampleText = example.getText().trim();
                        if (!exampleText.isEmpty() && !examples.contains(exampleText)) {
                            examples.add(exampleText);
                        }
                    }
                }
                
                if (!examples.isEmpty()) {
                    result.put("examples", examples);
                    result.put("example", examples.get(0)); // First example as primary
                    log.info("Found {} examples for word: {}", examples.size(), word);
                } else {
                    log.warn("No examples found for word: {}", word);
                }
            } catch (Exception e) {
                log.warn("Could not find examples for word {}: {}", word, e.getMessage());
            }
            
            // Get Vietnamese examples
            try {
                List<WebElement> vietnameseExampleElements = driver.findElements(
                    By.cssSelector("span.trans.dtrans.dtrans-se.hdb")
                );
                List<String> vietnameseExamples = new ArrayList<>();
                for (WebElement example : vietnameseExampleElements) {
                    String exampleText = example.getText().trim();
                    if (!exampleText.isEmpty()) {
                        vietnameseExamples.add(exampleText);
                    }
                }
                if (!vietnameseExamples.isEmpty()) {
                    result.put("vietnameseExamples", vietnameseExamples);
                    result.put("vietnameseExample", vietnameseExamples.get(0)); // First example as primary
                    log.info("Found {} Vietnamese examples", vietnameseExamples.size());
                }
            } catch (Exception e) {
                log.warn("Could not find Vietnamese examples for word {}: {}", word, e.getMessage());
            }
            
            // Get US audio URL
            try {
                // Wait for the US pronunciation section to be present
                String xpath = "//*[@id=\"page-content\"]/div[2]/div[1]/div[2]/div/div[3]/div/div/div/div[2]/span[2]/span[2]/div";
                WebElement usButton = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath(xpath)
                ));
                
                if (usButton != null) {
                    log.info("Found US pronunciation button");
                    
                    // Click the button to trigger audio loading
                    log.info("Clicking US pronunciation button");
                    usButton.click();
                    
                    // Wait a bit for the audio to load
                    Thread.sleep(2000);
                    
                    // Try to find the audio URL using JavaScript
                    JavascriptExecutor js = (JavascriptExecutor) driver;
                    String script = "return document.querySelector('div.us.dpron-i audio')?.src || " +
                                  "document.querySelector('div.us.dpron-i source')?.src || " +
                                  "document.querySelector('audio#audio2')?.src || " +
                                  "document.querySelector('audio[src*=\"us_pron\"]')?.src || " +
                                  "document.querySelector('source[src*=\"us_pron\"]')?.src";
                    
                    String audioUrl = (String) js.executeScript(script);
                    
                    if (audioUrl != null && !audioUrl.isEmpty()) {
                        result.put("audioUrl", audioUrl);
                        log.info("Found US audio URL: {}", audioUrl);
                    } else {
                        // Try to find the audio URL in the page source
                        String pageSource = driver.getPageSource();
                        if (pageSource.contains("us_pron")) {
                            int startIndex = pageSource.indexOf("us_pron");
                            int endIndex = pageSource.indexOf(".mp3", startIndex);
                            if (endIndex != -1) {
                                // Extract full URL
                                int urlStart = pageSource.lastIndexOf("https://", startIndex);
                                if (urlStart == -1) {
                                    urlStart = pageSource.lastIndexOf("http://", startIndex);
                                }
                                if (urlStart != -1) {
                                    audioUrl = pageSource.substring(urlStart, endIndex + 4);
                                    result.put("audioUrl", audioUrl);
                                    log.info("Found US audio URL in page source: {}", audioUrl);
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to find US audio URL for word {}: {}", word, e.getMessage());
            }
            
            // Add the word itself
            result.put("word", word);
            
        } catch (Exception e) {
            log.error("Error getting word info for word {}: {}", word, e.getMessage(), e);
        } finally {
            if (driver != null) {
                try {
                    driver.quit();
                } catch (Exception e) {
                    log.error("Error closing WebDriver: {}", e.getMessage());
                }
            }
        }
        return result;
    }
}

