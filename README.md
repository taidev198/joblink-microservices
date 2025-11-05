# JobLink Microservices Architecture

This project implements a microservices architecture for the JobLink application using Spring Cloud.

## Architecture Overview

The microservices architecture consists of the following services:

1. **Eureka Server** (Port 8761) - Service Discovery Server
2. **API Gateway** (Port 8091) - Single entry point for all client requests
3. **Main Service** (Port 8082) - Core business logic service
4. **Matching Service** (Port 8081) - AI/ML job matching service
5. **Print Service** (Port 8083) - Printing service

## Technology Stack

- **Spring Boot 3.2.12**
- **Spring Cloud 2023.0.0**
- **Eureka** - Service Discovery
- **Spring Cloud Gateway** - API Gateway
- **OpenFeign** - Declarative HTTP Client
- **Resilience4j** - Circuit Breaker
- **Spring Cloud LoadBalancer** - Client-side Load Balancing
- **MySQL 8.0** - Database
- **Redis** - Caching

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- Docker and Docker Compose (for containerized deployment)
- MySQL 8.0
- Redis (optional, for caching)

## Building the Project

To build all services:

```bash
mvn clean install
```

To build individual services:

```bash
cd eureka-server && mvn clean install
cd api-gateway && mvn clean install
cd main-service && mvn clean install
cd matching-service && mvn clean install
cd print-service && mvn clean install
```

## Running Services Locally

### Option 1: Run with Docker Compose (Recommended)

1. Start all services with Docker Compose:

```bash
docker-compose up -d
```

2. Access Eureka Dashboard: http://localhost:8761
3. Access API Gateway: http://localhost:8091

### Option 2: Run Services Manually

**Start services in the following order:**

1. **Eureka Server** (Service Discovery):
```bash
cd eureka-server
mvn spring-boot:run
```
Access: http://localhost:8761

2. **Main Service**:
```bash
cd main-service
mvn spring-boot:run
```

3. **Matching Service**:
```bash
cd matching-service
mvn spring-boot:run
```

4. **Print Service**:
```bash
cd print-service
mvn spring-boot:run
```

5. **API Gateway** (should be started last):
```bash
cd api-gateway
mvn spring-boot:run
```

## API Gateway Routes

All services are accessible through the API Gateway at port 8091:

- **Main Service**: `http://localhost:8091/api/main/**`
- **Matching Service**: `http://localhost:8091/api/matching/**`
- **Print Service**: `http://localhost:8091/api/print/**`

## Service Discovery

All services register themselves with Eureka Server. You can view registered services at:
http://localhost:8761

## Direct Service Access (for development)

- Eureka Server: http://localhost:8761
- Main Service: http://localhost:8082
- Matching Service: http://localhost:8081
- Print Service: http://localhost:8083
- API Gateway: http://localhost:8091

## Configuration

### Service Discovery Configuration

All services are configured to register with Eureka Server:
```yaml
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
    fetch-registry: true
    register-with-eureka: true
```

### Circuit Breaker Configuration

Resilience4j Circuit Breaker is configured for fault tolerance:
- Failure Rate Threshold: 50%
- Sliding Window Size: 10
- Wait Duration in Open State: 10 seconds
- Minimum Number of Calls: 5

## Health Checks

All services expose health endpoints via Spring Boot Actuator:
- Health: `http://localhost:{port}/actuator/health`
- Metrics: `http://localhost:{port}/actuator/metrics`
- Info: `http://localhost:{port}/actuator/info`

## Inter-Service Communication

Services communicate using:
- **OpenFeign** - For synchronous HTTP calls
- **Service Discovery** - Services are discovered via Eureka by service name

Example: Matching Service calls Main Service using:
```java
@FeignClient(name = "main-service")
public interface JobLinkClient {
    // ...
}
```

## Development

### Adding a New Service

1. Create a new module in the root `pom.xml`
2. Add Spring Cloud dependencies
3. Enable Eureka Client: `@EnableEurekaClient`
4. Configure service registration in `application.yml`
5. Add route in API Gateway `application.yml`

### Database Configuration

Update database connection strings in each service's `application.yml` or `application.properties`:
- Main Service: `main-service/src/main/resources/application.yml`
- Matching Service: `matching-service/src/main/resources/application.properties`

## Troubleshooting

### Service Not Registering with Eureka

1. Check if Eureka Server is running
2. Verify service configuration (application name and Eureka URL)
3. Check network connectivity between services

### Circuit Breaker Not Working

1. Verify Resilience4j dependencies are added
2. Check Circuit Breaker configuration in `application.yml`
3. Ensure sufficient failures are triggered for circuit to open

### API Gateway Not Routing

1. Verify services are registered with Eureka
2. Check gateway route configuration
3. Ensure service names match in Eureka and Gateway

## Production Considerations

1. **Security**: Add authentication/authorization to API Gateway
2. **Monitoring**: Integrate with monitoring tools (Prometheus, Grafana)
3. **Logging**: Implement centralized logging (ELK Stack)
4. **Configuration**: Use Spring Cloud Config Server for centralized configuration
5. **Database**: Use connection pooling and read replicas
6. **Caching**: Implement distributed caching with Redis
7. **Load Balancing**: Configure multiple instances of each service

## License

This project is part of the JobLink application.

