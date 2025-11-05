# Quick Start Guide - JobLink Microservices

## Prerequisites Checklist

- [ ] Java 17 or higher installed
- [ ] Maven 3.6+ installed
- [ ] MySQL 8.0 running (or update connection strings)
- [ ] Redis running (optional, for caching)
- [ ] Docker and Docker Compose (optional, for containerized deployment)

## Quick Start - Option 1: Docker Compose (Easiest)

1. **Start all services with Docker Compose:**
   ```bash
   docker-compose up -d
   ```

2. **Access Services:**
   - Eureka Dashboard: http://localhost:8761
   - API Gateway: http://localhost:8091
   - Main Service: http://localhost:8082
   - Matching Service: http://localhost:8081
   - Print Service: http://localhost:8083

3. **Verify Services are Registered:**
   - Open http://localhost:8761
   - You should see all services listed under "Instances currently registered with Eureka"

## Quick Start - Option 2: Manual Setup

### Step 1: Build All Services

```bash
cd joblink-microservices
mvn clean install
```

### Step 2: Start Services in Order

**Terminal 1 - Eureka Server:**
```bash
cd eureka-server
mvn spring-boot:run
```
Wait until you see: "Started EurekaServerApplication"

**Terminal 2 - Main Service:**
```bash
cd main-service
mvn spring-boot:run
```

**Terminal 3 - Matching Service:**
```bash
cd matching-service
mvn spring-boot:run
```

**Terminal 4 - Print Service:**
```bash
cd print-service
mvn spring-boot:run
```

**Terminal 5 - API Gateway:**
```bash
cd api-gateway
mvn spring-boot:run
```

### Step 3: Verify Setup

1. **Check Eureka Dashboard:**
   - Open http://localhost:8761
   - All services should be registered

2. **Test API Gateway:**
   - Main Service: http://localhost:8091/api/main/actuator/health
   - Matching Service: http://localhost:8091/api/matching/actuator/health
   - Print Service: http://localhost:8091/api/print/actuator/health

## Service Endpoints

### Through API Gateway (Port 8091)
- Main Service: `http://localhost:8091/api/main/**`
- Matching Service: `http://localhost:8091/api/matching/**`
- Print Service: `http://localhost:8091/api/print/**`

### Direct Access
- Main Service: `http://localhost:8082/**`
- Matching Service: `http://localhost:8081/**`
- Print Service: `http://localhost:8083/**`

## Configuration Files to Update

### Database Configuration

**Main Service:** `main-service/src/main/resources/application.yml`
- Update `spring.datasource.url`
- Update `spring.datasource.username`
- Update `spring.datasource.password`

**Matching Service:** `matching-service/src/main/resources/application.properties`
- Update `spring.datasource.url`
- Update `spring.datasource.username`
- Update `spring.datasource.password`

### Redis Configuration (if using)

**Main Service:** `main-service/src/main/resources/application.yml`
- Update `spring.data.redis.host`
- Update `spring.data.redis.port`

## Troubleshooting

### Service Not Starting

1. **Check Java Version:**
   ```bash
   java -version
   ```
   Should be 17 or higher

2. **Check Port Availability:**
   - Ensure ports 8761, 8091, 8082, 8081, 8083 are not in use
   - On Windows: `netstat -ano | findstr :8761`
   - On Linux/Mac: `lsof -i :8761`

### Service Not Registering with Eureka

1. **Check Eureka Server is Running:**
   - Open http://localhost:8761
   - Should see Eureka dashboard

2. **Check Service Configuration:**
   - Verify `eureka.client.service-url.defaultZone` is correct
   - Verify `spring.application.name` is set

3. **Check Network Connectivity:**
   - Ensure services can reach Eureka server
   - Check firewall settings

### API Gateway Not Routing

1. **Verify Services are Registered:**
   - Check Eureka dashboard at http://localhost:8761

2. **Check Gateway Configuration:**
   - Verify service names in gateway routes match Eureka service names
   - Check gateway logs for errors

### Database Connection Issues

1. **Verify MySQL is Running:**
   ```bash
   mysql -u root -p
   ```

2. **Check Connection String:**
   - Format: `jdbc:mysql://host:port/database`
   - Verify database exists

3. **Check Credentials:**
   - Verify username and password are correct

## Next Steps

1. **Add Authentication:**
   - Configure security in API Gateway
   - Add JWT token validation

2. **Add Monitoring:**
   - Integrate with Prometheus
   - Set up Grafana dashboards

3. **Add Logging:**
   - Configure centralized logging
   - Set up ELK Stack

4. **Add Configuration Server:**
   - Set up Spring Cloud Config Server
   - Externalize configuration

## Support

For issues or questions, refer to:
- Full README: `README.md`
- Spring Cloud Documentation: https://spring.io/projects/spring-cloud
- Spring Boot Documentation: https://spring.io/projects/spring-boot

