# Vocabulary Service

A microservice for English vocabulary learning functionality, providing word management, user vocabulary tracking, learning progress, and quiz generation.

## Features

- **Word Management**: CRUD operations for vocabulary words
- **User Vocabulary**: Track user's personal vocabulary list
- **Learning Progress**: Record and track learning progress
- **Quiz Generation**: Generate quizzes based on vocabulary
- **Spaced Repetition**: Automatic scheduling for word review
- **Statistics**: Track learning statistics and progress

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

#### Update Learning Status
```http
PUT /api/vocabulary/users/{userId}/vocabularies/words/{wordId}/status?status=LEARNING&isCorrect=true
```

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

