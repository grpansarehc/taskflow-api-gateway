# API Gateway

## Overview
The **API Gateway** serves as the single entry point for all client requests in the TaskFlow microservices architecture. It handles routing, authentication, load balancing, and CORS configuration.

## Technology Stack
- **Spring Boot** 3.4.1
- **Spring Cloud Gateway** 2024.0.0
- **JWT Authentication**
- **Java** 17

## Port
- **Default Port**: `8080`

## Features
- ✅ Centralized routing to microservices
- ✅ JWT-based authentication
- ✅ CORS configuration
- ✅ Load balancing via Eureka
- ✅ Request/Response filtering
- ✅ API documentation aggregation (Swagger)
- ✅ Duplicate header deduplication

## Architecture

### Request Flow
```
Client → API Gateway (8080) → Service Discovery (Eureka) → Microservices
```

### Routes Configuration

#### User Management Service (UMS)
```properties
Path: /api/auth/**, /api/users/**
Service: UMS-SERVICE
Filters: DedupeResponseHeader
```

#### Project Service
```properties
Path: /api/projects/**
Service: PROJECT-SERVICE
Filters: AuthenticationFilter, DedupeResponseHeader
```

#### Kanban Service
```properties
Path: /api/kanban/**
Service: KANBAN-SERVICE
Filters: AuthenticationFilter, DedupeResponseHeader
```

#### Task Service
```properties
Path: /api/tasks/**
Service: TASK-SERVICE
Filters: AuthenticationFilter, DedupeResponseHeader
```

## Configuration

### Application Properties
Located at: `src/main/resources/application.properties`

#### Key Configurations
```properties
server.port=8080
spring.application.name=api-gateway
eureka.client.service-url.defaultZone=http://localhost:8761/eureka

# JWT Secret
app.jwtSecret=YOUR_SECRET_KEY

# CORS
spring.cloud.gateway.globalcors.corsConfigurations.[/**].allowedOrigins=http://localhost:5173
spring.cloud.gateway.globalcors.corsConfigurations.[/**].allowedMethods=GET,POST,PUT,DELETE,OPTIONS
spring.cloud.gateway.globalcors.corsConfigurations.[/**].allowedHeaders=*
spring.cloud.gateway.globalcors.corsConfigurations.[/**].allowCredentials=true
```

## Running the Service

### Prerequisites
- Java 17 or higher
- Maven 3.6+
- Eureka Service Registry running on port 8761

### Using Maven
```bash
cd api-gateway
mvn clean install
mvn spring-boot:run
```

### Using JAR
```bash
mvn clean package
java -jar target/api-gateway-0.0.1-SNAPSHOT.jar
```

## Authentication Filter

### How It Works
1. Extracts JWT token from `Authorization` header
2. Validates token using `JwtUtils`
3. Extracts user information (userId, email)
4. Adds custom headers for downstream services:
   - `X-User-Id`: User's UUID
   - `X-User-Email`: User's email

### Bypassed Paths
- `/v3/api-docs/**` - API documentation
- `/swagger-ui/**` - Swagger UI
- `OPTIONS` requests - CORS preflight

## API Documentation

### Swagger UI
Access aggregated API documentation at:
```
http://localhost:8080/swagger-ui.html
```

### Available Service Docs
- User Management Service: `/v3/api-docs/ums`
- Project Service: `/v3/api-docs/projects`

## Health Check
```bash
curl http://localhost:8080/actuator/health
```

## Gateway Endpoints
```bash
curl http://localhost:8080/actuator/gateway/routes
```

## Testing Routes

### Test UMS Route (No Auth Required)
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"password"}'
```

### Test Project Route (Auth Required)
```bash
curl http://localhost:8080/api/projects \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Test Kanban Route (Auth Required)
```bash
curl http://localhost:8080/api/kanban/projects/{projectId}/board \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## Troubleshooting

### 404 Not Found
- Verify the service is registered in Eureka
- Check route configuration in `application.properties`
- Ensure service name matches Eureka registration (UPPERCASE)

### 401 Unauthorized
- Check if JWT token is valid
- Verify `Authorization` header format: `Bearer <token>`
- Check JWT secret matches across services

### 403 Forbidden
- Verify downstream service security configuration
- Check if service permits the request path

### CORS Errors
- Verify `allowedOrigins` includes your frontend URL
- Check if `allowCredentials` is set to `true`
- Ensure `DedupeResponseHeader` filter is applied

## Dependencies
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-gateway</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
</dependency>
```

## Security Best Practices
- ✅ Use strong JWT secret (minimum 256 bits)
- ✅ Enable HTTPS in production
- ✅ Implement rate limiting
- ✅ Add request logging for auditing
- ✅ Use environment variables for secrets

---

**Status**: ✅ Core Infrastructure Service  
**Maintained by**: TaskFlow Team