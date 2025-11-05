# JobLink AI Matching Service

This microservice provides AI/ML-based job matching capabilities for the JobLink platform. It uses natural language processing and machine learning algorithms to match job seekers with suitable job opportunities based on their skills, experience, and preferences.

## Features

- AI-powered job recommendations for students/job seekers
- Candidate matching for employers
- Skill gap analysis
- Personalized match explanations
- Integration with the main JobLink service

## Technology Stack

- Java 17
- Spring Boot 3.3.5
- Spring Cloud OpenFeign for service communication
- Natural Language Processing (NLP) libraries:
  - Apache OpenNLP
  - DeepLearning4J
- MySQL database
- Swagger/OpenAPI for API documentation

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven
- MySQL

### Installation

1. Clone the repository:
   ```
   git clone https://github.com/your-organization/joblink-ai-matching.git
   cd joblink-ai-matching
   ```

2. Configure the database in `application.properties`:
   ```
   spring.datasource.url=jdbc:mysql://localhost:3306/joblink_ai
   spring.datasource.username=your_username
   spring.datasource.password=your_password
   ```

3. Configure the main JobLink service URL:
   ```
   joblink.service.url=http://localhost:8080
   ```

4. Build the application:
   ```
   mvn clean install
   ```

5. Run the application:
   ```
   java -jar target/jobmatch-ai-0.0.1-SNAPSHOT.jar
   ```

## API Documentation

Once the application is running, you can access the Swagger UI at:
```
http://localhost:8081/swagger-ui.html
```

## Docker Support

To build and run the application using Docker:

```
docker build -t joblink-ai-matching .
docker run -p 8081:8081 joblink-ai-matching
```

## Integration with Main JobLink Service

This microservice communicates with the main JobLink service using Feign clients. The main service needs to expose the following endpoints:

- `/api/ai/jobs` - Get all jobs
- `/api/ai/students/{studentId}` - Get student by ID
- `/api/ai/jobs/{jobId}` - Get job by ID
- `/api/ai/fields` - Get all fields

## License

This project is licensed under the Apache License 2.0 - see the LICENSE file for details.