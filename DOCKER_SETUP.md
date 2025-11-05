# Docker Setup Guide

## Prerequisites

1. **Docker Desktop** must be installed and running
   - Download from: https://www.docker.com/products/docker-desktop
   - Make sure Docker Desktop is started before running commands

2. **Verify Docker is running**:
   ```powershell
   docker --version
   docker ps
   ```

## Common Issues

### Issue 1: Docker Desktop Not Running

**Error**: `unable to get image... open //./pipe/dockerDesktopLinuxEngine: The system cannot find the file specified`

**Solution**:
1. Start Docker Desktop application
2. Wait for it to fully start (check system tray icon)
3. Verify with: `docker ps`

### Issue 2: Version Warning

**Warning**: `the attribute 'version' is obsolete`

**Solution**: Already fixed in docker-compose.yml (version removed)

## Starting Services

### Option 1: Start All Services with Docker Compose

```powershell
cd joblink-microservices
docker-compose up -d
```

This will start:
- Eureka Server (port 8761)
- API Gateway (port 8091)
- Main Service (port 8082)
- Matching Service (port 8081)
- Print Service (port 8083)
- Vocabulary Service (port 8084)
- MySQL Database (port 3306)
- Redis (port 6379)

### Option 2: Start Services Individually

If you prefer to run services manually without Docker:

```powershell
# Terminal 1 - Eureka Server
cd joblink-microservices\eureka-server
mvn spring-boot:run

# Terminal 2 - Main Service
cd joblink-microservices\main-service
mvn spring-boot:run

# Terminal 3 - Matching Service
cd joblink-microservices\matching-service
mvn spring-boot:run

# Terminal 4 - Print Service
cd joblink-microservices\print-service
mvn spring-boot:run

# Terminal 5 - Vocabulary Service
cd joblink-microservices\vocabulary-service
mvn spring-boot:run

# Terminal 6 - API Gateway
cd joblink-microservices\api-gateway
mvn spring-boot:run
```

## Database Setup

### Create Database

Before starting vocabulary-service, create the database:

```sql
CREATE DATABASE vocabulary_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Or using MySQL command line:

```powershell
mysql -u root -p
CREATE DATABASE vocabulary_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### Update Database Configuration

Update `vocabulary-service/src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/vocabulary_db
    username: root
    password: your_password
```

## Checking Services

### Check Running Containers

```powershell
docker ps
```

### Check Logs

```powershell
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f vocabulary-service
```

### Stop Services

```powershell
docker-compose down
```

### Restart Services

```powershell
docker-compose restart
```

## Service URLs

Once services are running:

- **Eureka Dashboard**: http://localhost:8761
- **API Gateway**: http://localhost:8091
- **Main Service**: http://localhost:8082
- **Matching Service**: http://localhost:8081
- **Print Service**: http://localhost:8083
- **Vocabulary Service**: http://localhost:8084

### Verify Services are Registered

1. Open http://localhost:8761
2. You should see all services listed under "Instances currently registered with Eureka"

## Troubleshooting

### Services Not Starting

1. **Check Docker Desktop is running**
2. **Check ports are available**:
   ```powershell
   netstat -ano | findstr :8761
   netstat -ano | findstr :8091
   ```

3. **Check logs for errors**:
   ```powershell
   docker-compose logs vocabulary-service
   ```

### Database Connection Issues

1. **Verify MySQL is running**:
   ```powershell
   mysql -u root -p
   ```

2. **Check database exists**:
   ```sql
   SHOW DATABASES;
   ```

3. **Verify connection string in application.yml**

### Build Issues

If services fail to build:

```powershell
# Build all services
cd joblink-microservices
mvn clean install

# Build specific service
cd vocabulary-service
mvn clean install
```

## Next Steps

1. Start Docker Desktop
2. Create vocabulary database
3. Update database configuration
4. Start services: `docker-compose up -d`
5. Verify services in Eureka: http://localhost:8761
6. Start frontend: `cd English_Project && npm run dev`

