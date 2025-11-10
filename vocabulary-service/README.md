# Vocabulary Service

A microservice for English vocabulary learning functionality, providing word management, user vocabulary tracking, learning progress, and quiz generation.

## Features

- **Word Management**: CRUD operations for vocabulary words
- **User Vocabulary**: Track user's personal vocabulary list
- **Learning Progress**: Record and track learning progress
- **Quiz Generation**: Generate quizzes based on vocabulary
- **Spaced Repetition (SM-2 Algorithm)**: Advanced spaced repetition system using the SM-2 algorithm for optimal review scheduling
  - Automatically calculates review intervals based on user performance
  - Adjusts easiness factor dynamically
  - Tracks repetitions and intervals for personalized learning
- **Statistics**: Track learning statistics and progress
- **Shadowing Practice**: Pronunciation practice using Whisper speech recognition with detailed feedback

## API Endpoints

### Word Management

#### Create Word
```http
POST /api/vocabulary/words
Content-Type: application/json

{
  "englishWord": "hello",
  "meaning": "Xin chào",
  "pronunciation": "/həˈloʊ/",
  "exampleSentence": "Hello, how are you?",
  "translation": "Xin chào, bạn khỏe không?",
  "level": "BEGINNER",
  "category": "DAILY_LIFE",
  "partOfSpeech": "interjection",
  "synonyms": "hi, greetings",
  "antonyms": "goodbye",
  "imageUrl": "https://example.com/hello.jpg",
  "audioUrl": "https://example.com/hello.mp3"
}
```

#### Get All Words
```http
GET /api/vocabulary/words?page=0&size=20&sortBy=id&sortDir=ASC
```

#### Get Word by ID
```http
GET /api/vocabulary/words/{id}
```

#### Search Words
```http
GET /api/vocabulary/words/search?keyword=hello&page=0&size=20
```

#### Get Words by Level
```http
GET /api/vocabulary/words/level/BEGINNER?page=0&size=20
```

#### Get Words by Category
```http
GET /api/vocabulary/words/category/DAILY_LIFE?page=0&size=20
```

#### Update Word
```http
PUT /api/vocabulary/words/{id}
Content-Type: application/json

{
  "englishWord": "hello",
  "meaning": "Xin chào",
  ...
}
```

#### Delete Word
```http
DELETE /api/vocabulary/words/{id}
```

### User Vocabulary

#### Add Word to User Vocabulary
```http
POST /api/vocabulary/users/{userId}/vocabularies/words/{wordId}
```

#### Get User Vocabulary
```http
GET /api/vocabulary/users/{userId}/vocabularies?page=0&size=20
```

#### Get Words Due for Review
```http
GET /api/vocabulary/users/{userId}/vocabularies/due-for-review
```

#### Update Learning Status (SM-2 Algorithm)
Update learning status using the SM-2 spaced repetition algorithm with quality parameter (0-5):

```http
PUT /api/vocabulary/users/{userId}/vocabularies/words/{wordId}/review?quality=5
```

**Quality Scale:**
- `5`: Perfect response
- `4`: Correct response after hesitation
- `3`: Correct response with serious difficulty
- `2`: Incorrect response; correct one remembered
- `1`: Incorrect response; correct one seemed familiar
- `0`: Complete blackout

**Response includes SM-2 fields:**
- `easinessFactor`: Current easiness factor (1.3 - 2.5)
- `intervalDays`: Days until next review
- `repetitions`: Number of successful consecutive reviews

#### Update Learning Status (Legacy - Backward Compatible)
```http
PUT /api/vocabulary/users/{userId}/vocabularies/words/{wordId}/status?status=LEARNING&isCorrect=true
```

This endpoint maps `isCorrect` to quality (5 if true, 2 if false) and uses SM-2 algorithm internally.

#### Get User Vocabulary Stats
```http
GET /api/vocabulary/users/{userId}/vocabularies/stats
```

### Learning Progress

#### Record Progress
```http
POST /api/vocabulary/users/{userId}/progress
Content-Type: application/json

{
  "wordId": 1,
  "progressType": "QUIZ",
  "isCorrect": true,
  "timeSpentSeconds": 30,
  "notes": "Answered correctly"
}
```

#### Get User Progress
```http
GET /api/vocabulary/users/{userId}/progress?page=0&size=20
```

#### Get Progress Stats
```http
GET /api/vocabulary/users/{userId}/progress/stats
```

### Quiz

#### Generate Quiz
```http
POST /api/vocabulary/users/{userId}/quiz/generate
Content-Type: application/json

{
  "wordIds": [1, 2, 3, 4, 5],
  "numberOfQuestions": 10,
  "difficulty": "MEDIUM"
}
```

### Shadowing Practice

#### Perform Shadowing Practice
Transcribe user's audio and compare with expected text using Whisper speech recognition.

```http
POST /api/vocabulary/shadowing
Content-Type: multipart/form-data

Form Data:
- audioFile: (file) Audio file containing user's speech
- expectedText: (string) Expected text that user should have spoken
- language: (string, optional) Language code (default: "en")
- wordTimestamps: (boolean, optional) Include word timestamps (default: true)
```

**Response:**
```json
{
  "success": true,
  "message": "Shadowing practice completed",
  "data": {
    "transcribedText": "Hello how are you",
    "expectedText": "Hello, how are you?",
    "accuracy": 100.0,
    "totalExpected": 4,
    "totalCorrect": 4,
    "correctWords": [
      {"word": "hello", "position": 0},
      {"word": "how", "position": 1},
      {"word": "are", "position": 2},
      {"word": "you", "position": 3}
    ],
    "wrongWords": [],
    "missingWords": [],
    "extraWords": [],
    "wordComparison": [
      {"status": "✓", "expectedWord": "hello", "transcribedWord": "hello"},
      {"status": "✓", "expectedWord": "how", "transcribedWord": "how"},
      {"status": "✓", "expectedWord": "are", "transcribedWord": "are"},
      {"status": "✓", "expectedWord": "you", "transcribedWord": "you"}
    ],
    "feedback": "🌟 Excellent! Your pronunciation is very clear!\n\nCorrect words: 4/4 (100.0%)"
  }
}
```

#### Compare Texts (without audio)
Compare two texts directly without audio transcription. Useful for testing the comparison algorithm.

```http
POST /api/vocabulary/shadowing/compare?transcribedText=hello how are you&expectedText=Hello, how are you?
```

**Response:** Same structure as shadowing practice response, but without transcribedText from audio.

**Features:**
- Uses Whisper API for speech-to-text transcription
- Normalizes text (handles contractions, punctuation)
- Sequence alignment algorithm for word-by-word comparison
- Detailed feedback on pronunciation accuracy
- Identifies correct, wrong, missing, and extra words

**Configuration:**
Update `application.yml` to configure Whisper API:
```yaml
whisper:
  api:
    url: http://localhost:8000/api/transcribe
  temp:
    dir: ${java.io.tmpdir}/whisper
```

**Setting up Whisper API Server:**

1. Navigate to the whisper project directory:
```bash
cd whisper
```

2. Install API dependencies:
```bash
pip install -r requirements_api.txt
# Or if you already have whisper installed:
pip install fastapi uvicorn[standard] python-multipart
```

3. Start the API server:
```bash
python api_server.py
# Or use the startup script:
./start_api.sh
```

The server will start on `http://localhost:8000` by default. Make sure this matches the URL in your `application.yml` configuration.

For more details, see `whisper/README_API.md`.

## Word Levels

- `BEGINNER`: Basic vocabulary
- `INTERMEDIATE`: Intermediate vocabulary
- `ADVANCED`: Advanced vocabulary

## Word Categories

- `DAILY_LIFE`: Daily conversation
- `BUSINESS`: Business vocabulary
- `ACADEMIC`: Academic vocabulary
- `TECHNOLOGY`: Technology terms
- `TRAVEL`: Travel-related words
- `FOOD`: Food and cooking
- `SPORTS`: Sports vocabulary
- `ENTERTAINMENT`: Entertainment terms
- `OTHER`: Other categories

## Learning Status

- `NOT_STARTED`: Word added but not studied
- `LEARNING`: Currently learning
- `REVIEWING`: In review phase
- `MASTERED`: Word mastered

## SM-2 Spaced Repetition Algorithm

The service implements the SM-2 (SuperMemo 2) spaced repetition algorithm developed by Piotr Wozniak. This algorithm optimizes the timing of reviews based on user performance.

### How It Works

1. **Quality Assessment (0-5 scale)**: When a user reviews a word, they provide a quality score:
   - **5**: Perfect response
   - **4**: Correct response after hesitation
   - **3**: Correct response with serious difficulty
   - **2**: Incorrect response; correct one remembered
   - **1**: Incorrect response; correct one seemed familiar
   - **0**: Complete blackout

2. **Easiness Factor (EF)**: Adjusts based on quality:
   - Formula: `EF' = EF + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02))`
   - Ranges from 1.3 (minimum) to 2.5 (default for new items)
   - Higher quality responses increase EF, making the word easier to remember
   - Lower quality responses decrease EF, requiring more frequent reviews

3. **Interval Calculation**: Days until next review:
   - **First review** (repetitions = 0): 1 day
   - **Second review** (repetitions = 1): 6 days
   - **Subsequent reviews** (repetitions ≥ 2): `previous_interval * EF`

4. **Repetitions Reset**: If quality < 3, repetitions reset to 0 and the word is reviewed again today (interval = 0)

### Example Flow

1. User adds a new word → EF = 2.5, repetitions = 0, interval = 0
2. First review with quality = 5 → EF = 2.6, repetitions = 1, interval = 1 day
3. Second review with quality = 5 → EF = 2.7, repetitions = 2, interval = 6 days
4. Third review with quality = 4 → EF = 2.7, repetitions = 3, interval = 16 days (6 * 2.7)
5. Fourth review with quality = 2 → EF = 2.38, repetitions = 0, interval = 0 (reset)

### Benefits

- **Personalized Learning**: Each word's review schedule adapts to individual performance
- **Optimal Retention**: Reviews are scheduled at the optimal time to maximize memory retention
- **Efficient Learning**: Reduces unnecessary reviews while ensuring difficult words are reviewed more frequently

## Progress Types

- `QUIZ`: Quiz questions
- `FLASHCARD`: Flashcard practice
- `WRITING`: Writing practice
- `LISTENING`: Listening practice
- `SPEAKING`: Speaking practice

## Database Schema

### Words Table
- `id`: Primary key
- `english_word`: English word
- `meaning`: Meaning/translation
- `pronunciation`: Pronunciation guide
- `example_sentence`: Example usage
- `translation`: Translation
- `level`: BEGINNER, INTERMEDIATE, ADVANCED
- `category`: Word category
- `part_of_speech`: Grammar part
- `synonyms`: Related words
- `antonyms`: Opposite words
- `image_url`: Image URL
- `audio_url`: Audio URL
- `is_active`: Active status
- `created_at`, `updated_at`: Timestamps

### User Vocabularies Table
- `id`: Primary key
- `user_id`: User ID
- `word_id`: Foreign key to words
- `status`: Learning status
- `review_count`: Number of reviews
- `correct_count`: Correct answers
- `incorrect_count`: Incorrect answers
- `last_reviewed_at`: Last review time
- `next_review_at`: Next review scheduled time
- `mastery_score`: Mastery score (0.0-1.0)
- `easiness_factor`: SM-2 easiness factor (1.3-2.5, default: 2.5)
- `interval_days`: SM-2 interval in days until next review
- `repetitions`: SM-2 number of successful consecutive reviews
- `created_at`, `updated_at`: Timestamps

### Learning Progress Table
- `id`: Primary key
- `user_id`: User ID
- `word_id`: Foreign key to words
- `progress_type`: Type of progress
- `is_correct`: Answer correctness
- `time_spent_seconds`: Time spent
- `notes`: Additional notes
- `created_at`: Timestamp

## Configuration

### Database
Update database connection in `application.yml`:
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/vocabulary_db
    username: root
    password: your_password
```

### Create Database
```sql
CREATE DATABASE vocabulary_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### Database Migrations

#### SM-2 Algorithm Fields Migration
After adding SM-2 algorithm fields to the `user_vocabularies` table, you need to run a migration to set default values for existing records:

```bash
# Option 1: Using MySQL command line
mysql -u root -p vocabulary_db < migration_add_sm2_fields_simple.sql

# Option 2: Using MySQL client
mysql -u root -p
USE vocabulary_db;
SOURCE migration_add_sm2_fields_simple.sql;
```

This migration will:
- Update all existing `user_vocabularies` records with NULL values for SM-2 fields
- Set `easiness_factor` to 2.5 (default for new items)
- Set `interval_days` to 0 (not yet reviewed)
- Set `repetitions` to 0 (no successful consecutive reviews)

**Note:** The columns will be automatically created by Hibernate/JPA when you start the application (due to `ddl-auto: update`). The migration script only updates existing records with default values.

## Running the Service

### Build
```bash
mvn clean install
```

### Run
```bash
mvn spring-boot:run
```

Or use the API Gateway:
```bash
# Access through API Gateway
http://localhost:8091/api/vocabulary/words
```

## Integration with Frontend

The frontend app (`English_Project`) can integrate with this service through the API Gateway:

```typescript
// Example API call from frontend
const response = await fetch('http://localhost:8091/api/vocabulary/words', {
  method: 'GET',
  headers: {
    'Content-Type': 'application/json'
  }
});

const data = await response.json();
```

## Swagger Documentation

Access Swagger UI at:
```
http://localhost:8084/swagger-ui.html
```

Or through API Gateway:
```
http://localhost:8091/api/vocabulary/swagger-ui.html
```

## Health Check

```
GET http://localhost:8084/actuator/health
```

## Port

Default port: `8084`

## Service Name

Service name in Eureka: `vocabulary-service`

