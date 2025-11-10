package com.joblink.vocabulary.service;

import com.joblink.vocabulary.model.entity.UserVocabulary;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * SM-2 Spaced Repetition Algorithm Service
 * 
 * The SM-2 algorithm is a spaced repetition algorithm developed by Piotr Wozniak.
 * It calculates optimal intervals between reviews based on the user's performance.
 * 
 * Algorithm details:
 * - Quality (Q): 0-5 scale representing how well the user recalled the item
 *   - 5: Perfect response
 *   - 4: Correct response after hesitation
 *   - 3: Correct response with serious difficulty
 *   - 2: Incorrect response; correct one remembered
 *   - 1: Incorrect response; correct one seemed familiar
 *   - 0: Complete blackout
 * 
 * - Easiness Factor (EF): Adjusts based on quality, ranges from 1.3 to 2.5
 * - Interval: Days until next review
 * - Repetitions: Number of successful consecutive reviews
 */
@Slf4j
@Service
public class SM2SpacedRepetitionService {
    
    // Minimum easiness factor (EF cannot go below this)
    private static final double MIN_EF = 1.3;
    
    // Default easiness factor for new items
    private static final double DEFAULT_EF = 2.5;
    
    /**
     * Calculate next review date using SM-2 algorithm
     * 
     * @param userVocabulary The user vocabulary item to update
     * @param quality Quality of response (0-5)
     * @return Updated UserVocabulary with new interval and next review date
     */
    public UserVocabulary calculateNextReview(UserVocabulary userVocabulary, int quality) {
        if (quality < 0 || quality > 5) {
            throw new IllegalArgumentException("Quality must be between 0 and 5");
        }
        
        double currentEF = userVocabulary.getEasinessFactor();
        int currentRepetitions = userVocabulary.getRepetitions();
        int currentInterval = userVocabulary.getIntervalDays();
        
        // Update Easiness Factor based on quality
        double newEF = updateEasinessFactor(currentEF, quality);
        
        // Calculate new interval based on quality
        int newInterval;
        int newRepetitions;
        
        if (quality < 3) {
            // If quality < 3, reset repetitions and interval
            newRepetitions = 0;
            newInterval = 0; // Review again today
        } else {
            // If quality >= 3, calculate new interval
            if (currentRepetitions == 0) {
                newInterval = 1; // First review: 1 day
                newRepetitions = 1;
            } else if (currentRepetitions == 1) {
                newInterval = 6; // Second review: 6 days
                newRepetitions = 2;
            } else {
                // Subsequent reviews: interval = previous interval * EF
                newInterval = (int) Math.round(currentInterval * newEF);
                newRepetitions = currentRepetitions + 1;
            }
        }
        
        // Update the user vocabulary
        userVocabulary.setEasinessFactor(newEF);
        userVocabulary.setIntervalDays(newInterval);
        userVocabulary.setRepetitions(newRepetitions);
        userVocabulary.setLastReviewedAt(LocalDateTime.now());
        userVocabulary.setNextReviewAt(LocalDateTime.now().plusDays(newInterval));
        
        // Update review count
        userVocabulary.setReviewCount(userVocabulary.getReviewCount() + 1);
        
        // Update correct/incorrect counts
        if (quality >= 3) {
            userVocabulary.setCorrectCount(userVocabulary.getCorrectCount() + 1);
        } else {
            userVocabulary.setIncorrectCount(userVocabulary.getIncorrectCount() + 1);
        }
        
        // Update mastery score
        int totalAttempts = userVocabulary.getCorrectCount() + userVocabulary.getIncorrectCount();
        if (totalAttempts > 0) {
            double masteryScore = (double) userVocabulary.getCorrectCount() / totalAttempts;
            userVocabulary.setMasteryScore(masteryScore);
        }
        
        // Update status based on repetitions and mastery
        updateLearningStatus(userVocabulary);
        
        log.debug("SM-2 calculation: Quality={}, EF={}->{}, Repetitions={}->{}, Interval={}->{} days",
                quality, currentEF, newEF, currentRepetitions, newRepetitions, currentInterval, newInterval);
        
        return userVocabulary;
    }
    
    /**
     * Update Easiness Factor based on quality
     * 
     * Formula: EF' = EF + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02))
     * 
     * @param currentEF Current easiness factor
     * @param quality Quality of response (0-5)
     * @return New easiness factor
     */
    private double updateEasinessFactor(double currentEF, int quality) {
        double newEF = currentEF + (0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02));
        
        // Ensure EF doesn't go below minimum
        if (newEF < MIN_EF) {
            newEF = MIN_EF;
        }
        
        return newEF;
    }
    
    /**
     * Update learning status based on repetitions and mastery score
     */
    private void updateLearningStatus(UserVocabulary userVocabulary) {
        if (userVocabulary.getRepetitions() == 0) {
            userVocabulary.setStatus(UserVocabulary.LearningStatus.NOT_STARTED);
        } else if (userVocabulary.getRepetitions() < 3) {
            userVocabulary.setStatus(UserVocabulary.LearningStatus.LEARNING);
        } else if (userVocabulary.getMasteryScore() >= 0.8 && userVocabulary.getRepetitions() >= 5) {
            userVocabulary.setStatus(UserVocabulary.LearningStatus.MASTERED);
        } else {
            userVocabulary.setStatus(UserVocabulary.LearningStatus.REVIEWING);
        }
    }
    
    /**
     * Initialize SM-2 fields for a new user vocabulary item
     * New words should be immediately due for review
     */
    public void initializeSM2Fields(UserVocabulary userVocabulary) {
        userVocabulary.setEasinessFactor(DEFAULT_EF);
        userVocabulary.setIntervalDays(0);
        userVocabulary.setRepetitions(0);
        // Set nextReviewAt to now so the word is immediately due for review
        userVocabulary.setNextReviewAt(LocalDateTime.now());
    }
    
    /**
     * Get words due for review based on next_review_at date
     * This is already handled by the repository query, but this method
     * can be used for additional filtering if needed
     */
    public boolean isDueForReview(UserVocabulary userVocabulary) {
        if (userVocabulary.getNextReviewAt() == null) {
            return true; // Never reviewed, so it's due
        }
        return LocalDateTime.now().isAfter(userVocabulary.getNextReviewAt()) ||
               LocalDateTime.now().isEqual(userVocabulary.getNextReviewAt());
    }
}

