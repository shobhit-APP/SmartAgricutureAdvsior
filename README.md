# AgriConnect Platform

AgriConnect is a multi-module Spring Boot platform for agricultural advisory features: crop pricing, recommendations, weather, image-based crop disease analysis, community features (blog, videos, soil reports, crop reports) and authentication.

This repository (SmartAgricutureAdvsior) contains three main modules:

- `Agriconnect` — the main Spring Boot application (artifact: `com.example:AgriConnect`). It depends on the other modules and contains the application's entry point (`AgriconnectApplication`).
- `AuthenticationModule` — user authentication, JWT and Redis-backed sessions.
- `common` — shared utilities, DTOs and helpers used by other modules.
- `community` — new module added for community features (blogs, videos, soil reports, crop reports). It contains the `com.smartagriculture.community` package and a `cropreport` subpackage.

Repository structure (important folders):

```
/ (repo root)
├─ Agriconnect/                         # main application module
├─ AuthenticationModule/                # authentication module
├─ common/                              # shared module
├─ community/                           # community module (controllers, models, dto, cropreport, resources)
├─ docs/static/images/                  # project-level place to add images (architecture diagram)
└─ README.md
```

Where to add the architecture image
----------------------------------

Place your architecture diagram (PNG/SVG) here so it appears in the README and is tracked by Git:

- Project-level (recommended for README): `docs/static/images/architecture.png`
- Module-level (optional): `community/src/main/resources/static/images/architecture.png`

There is already a `.gitkeep` in each images folder so they stay tracked. To add your diagram:

1. Save the image file as `architecture.png` (or `.svg`) into `docs/static/images/`.
2. Commit and push the file. GitHub will render PNG/SVG inline in the README if you reference it.

Example README image embedding (already included below):

![Architecture](docs/static/images/architecture.png)

If you want the module-level image served by Spring Boot statically at runtime, place it in `community/src/main/resources/static/images/`.

How to build & run (local development)
-------------------------------------

Requirements:

- Java 21 (project property)
- Maven 3.6+
- MySQL (or configure `spring.datasource.*` for your environment)

Quick commands (Windows PowerShell):

```powershell
cd /d d:\Backup\Agriconnect_Platfrom
mvn -DskipTests package
cd Agriconnect
mvn spring-boot:run
```

Notes about application properties and secrets
---------------------------------------------

- The `community` module has its own `application.properties` at `community/src/main/resources/application.properties` for module-specific settings (upload dirs, thresholds, Cloudinary config, etc.).
- Do NOT commit secrets (database password, Cloudinary API keys). Prefer environment variables or `application-{profile}.properties` that are not checked into version control.

Recommended pattern (example placeholders):

```properties
spring.datasource.password=${DB_PASSWORD}
cloudinary.api.key=${CLOUDINARY_API_KEY}
cloudinary.api.secret=${CLOUDINARY_API_SECRET}
```

Create a local `application.properties` or export env vars in your shell for development.

Adding your architecture image and updating README
-------------------------------------------------

1. Put the image file at `docs/static/images/architecture.png`.
2. Update this README or the `docs/` folder with additional diagrams if needed.
3. Commit and push the image and README changes.

Git help (example commands):

```powershell
cd /d d:\Backup\Agriconnect_Platfrom
git add docs/static/images/architecture.png README.md
git commit -m "docs: add architecture diagram and update README"
git push origin main
```

Where to find the community module code
--------------------------------------

- Java sources: `community/src/main/java/com/smartagriculture/community`
- Properties: `community/src/main/resources/application.properties`
- Static assets (served by Spring Boot): `community/src/main/resources/static/`

Other helpful notes
-------------------
- If you want me to generate the architecture diagram (PlantUML or image) and add it to `docs/static/images`, tell me which style you prefer (PlantUML PNG/SVG, or a stylized image for presentation) and I can create it and commit it for you.
- If you want secrets replaced by environment variable placeholders in your `community` properties, I can edit the file and add `application.properties.example` with examples.

Contact / Contributing
----------------------
If you want me to make further updates (generate the diagram, commit files, or run the build), tell me and I'll proceed with the requested step.

---

Project status: work-in-progress. New `community` module skeleton and `cropreport` package were added and `community` module has a sample `application.properties` under `community/src/main/resources`.

# AgriConnect Platform Backend

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.11-green)
![Maven](https://img.shields.io/badge/Maven-Multi--Module-blue)
![MySQL](https://img.shields.io/badge/MySQL-Database-blue)
![Redis](https://img.shields.io/badge/Redis-Cache-red)

## 📋 Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Modules](#modules)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Configuration](#configuration)
- [Running the Application](#running-the-application)
- [API Documentation](#api-documentation)
- [Features](#features)
- [Database Schema](#database-schema)
- [Development](#development)
- [Deployment](#deployment)
- [Contributing](#contributing)
- [Support](#support)

## 🌾 Overview

AgriConnect Platform is a comprehensive agricultural management system built with Spring Boot and Java 21. The platform provides farmers and agricultural stakeholders with tools for crop management, disease detection, weather monitoring, price tracking, and agricultural recommendations.

## 🏗️ Architecture

The project follows a multi-module Maven architecture with clear separation of concerns:

```
Agriconnect_Platform/
├── Agriconnect/           # Core agricultural services
├── AuthenticationModule/  # Security and user management
├── common/ # Shared utilities and models
|-community frm modulue         
└── pom.xml
            # Parent POM configuration
```

### Technology Stack

- **Backend Framework**: Spring Boot 3.2.11
- **Language**: Java 21
- **Build Tool**: Maven
- **Database**: MySQL
- **Caching**: Redis
- **Security**: Spring Security with JWT
- **Documentation**: Swagger/OpenAPI
- **Image Processing**: Cloudinary
- **External APIs**: Weather API, Gemini AI, Pexels

## 📦 Modules

### 1. Agriconnect (Core Module)
**Port**: 8084

The main agricultural services module containing:

- **Crop Management**: Crop information, disease detection, and recommendations
- **Weather Services**: Real-time weather data and forecasts
- **Market Services**: Crop pricing and market analysis
- **Image Analysis**: AI-powered crop disease detection
- **Export Services**: PDF report generation
- **API Controllers**: RESTful endpoints for all services

**Key Features:**
- Crop disease identification and treatment recommendations
- Weather-based agricultural advice
- Market price tracking and analysis
- Multilingual support (Hindi/English)
- PDF report generation for agricultural data

### 2. AuthenticationModule
**Features**: Security and User Management

Handles all authentication and authorization concerns:

- **User Registration & Login**: Secure user onboarding
- **JWT Token Management**: Stateless authentication
- **OTP Services**: Email-based verification
- **Password Management**: Secure password reset functionality
- **Redis Integration**: Session and token caching
- **Role-Based Access Control**: User permission management

**Security Features:**
- JWT-based authentication
- OTP verification for critical operations
- Secure password hashing
- Session management with Redis
- CORS configuration
- Security middleware

### 3. Common Module
**Purpose**: Shared Resources and Utilities

Contains shared components used across modules:

- **Models**: Common data entities (User, Crop, Location, etc.)
- **DTOs**: Data transfer objects
- **Utilities**: Helper classes for various operations
- **Configuration**: Application-wide configurations
- **Exception Handling**: Global exception management
- **Internationalization**: Multi-language support

## 📋 Prerequisites

Before running the application, ensure you have:

- **Java 21** or higher
- **Maven 3.6+**
- **MySQL 8.0+**
- **Redis Server**
- **Git**

### External Service Accounts
- **Cloudinary Account** (for image storage)
- **Weather API Key** (for weather services)
- **Gemini AI API Key** (for AI-powered features)
- **Pexels API Key** (for image services)

## 🚀 Installation

### 1. Clone the Repository
```bash
git clone <repository-url>
cd Agriconnect_Platform
```

### 2. Database Setup
Create a MySQL database:
```sql
CREATE DATABASE shobhitdatabase;
CREATE USER 'root'@'localhost' IDENTIFIED BY 'local123';
GRANT ALL PRIVILEGES ON shobhitdatabase.* TO 'root'@'localhost';
FLUSH PRIVILEGES;
```

### 3. Redis Setup
Install and start Redis server:
```bash
# Windows (using Chocolatey)
choco install redis-64

# Start Redis
redis-server
```

### 4. Build the Project
```bash
mvn clean install
```

## ⚙️ Configuration

### Application Properties

#### Agriconnect Module (`Agriconnect/src/main/resources/application.properties`)
```properties
# Application
spring.application.name=AgriConnect
server.port=8084

# Database Configuration
spring.datasource.url=${url}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.username=${username}
spring.datasource.password=${password}

# JPA Settings
spring.jpa.show-sql=true
spring.jpa.hibernate.ddl-auto=update
spring.jpa.defer-datasource-initialization=true

# API Keys (Configure these with your actual keys)
cloudinary.cloud.name=your_cloud_name
cloudinary.api.key=your_api_key
cloudinary.api.secret=your_api_secret
```

#### Authentication Module
Configure JWT settings, Redis connection, and email services in the AuthenticationModule's application.properties.

### Environment Variables
Set the following environment variables for production:
```bash
DB_HOST=Your_Host
DB_PORT=3306
DB_NAME=shobhitdatabase
DB_USERNAME=Your_Username
DB_PASSWORD=Your_Password
REDIS_HOST=localhost
REDIS_PORT=6379
CLOUDINARY_CLOUD_NAME=your_cloud_name
CLOUDINARY_API_KEY=your_api_key
CLOUDINARY_API_SECRET=your_api_secret
```

## 🏃‍♂️ Running the Application

### Development Mode

#### Option 1: Run All Modules
```bash
# From the root directory
mvn spring-boot:run
```

#### Option 2: Run Individual Modules
```bash
# Run Agriconnect module
cd Agriconnect
mvn spring-boot:run

# Run Authentication module (in separate terminal)
cd AuthenticationModule
mvn spring-boot:run
```

### Production Mode
```bash
# Build JAR files
mvn clean package

# Run the application
java -jar Agriconnect/target/Agriconnect-0.0.1-SNAPSHOT.jar
```

### Using Docker (Optional)
```dockerfile
# Dockerfile example for the main module
FROM openjdk:21-jdk-slim
COPY target/Agriconnect-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

## 📚 API Documentation

### Base URLs
- **Agriconnect API**: `http://localhost:8084`
- **Authentication API**: `http://localhost:8081` (if running separately)

### Key Endpoints

#### Authentication Endpoints
```
POST /auth/register          # User registration
POST /auth/login            # User login
POST /auth/logout           # User logout
POST /auth/refresh-token    # Refresh JWT token
POST /auth/forgot-password  # Password reset request
POST /auth/reset-password   # Password reset confirmation
```

#### Agricultural Endpoints
```
GET  /api/crops                    # Get all crops
POST /api/crops                    # Add new crop
GET  /api/crops/{id}              # Get crop by ID
GET  /api/crop-diseases           # Get crop diseases
POST /api/crop-diseases           # Report crop disease
GET  /api/crop-recommendations    # Get crop recommendations
GET  /api/weather                 # Get weather information
GET  /api/market-prices          # Get market prices
POST /api/image-analysis         # Analyze crop images
GET  /api/export/pdf             # Export data to PDF
```

### Swagger Documentation
Access interactive API documentation at:
- `http://localhost:8084/swagger-ui.html`

## 🌟 Features

### Core Agricultural Features
- **Crop Disease Detection**: AI-powered image analysis for disease identification
- **Weather Integration**: Real-time weather data and agricultural forecasts
- **Market Price Tracking**: Current market prices for various crops
- **Crop Recommendations**: Personalized crop suggestions based on location and conditions
- **Multilingual Support**: Interface and data available in Hindi and English

### User Management
- **Secure Authentication**: JWT-based authentication with refresh tokens
- **User Profiles**: Comprehensive user profile management
- **Role-Based Access**: Different access levels for farmers, experts, and administrators
- **OTP Verification**: Secure email-based verification system

### Data Management
- **Export Functionality**: Generate PDF reports of agricultural data
- **Data Persistence**: Reliable MySQL database storage
- **Caching**: Redis-based caching for improved performance
- **File Upload**: Cloudinary integration for image storage

### Development Features
- **Comprehensive Logging**: Detailed application logging
- **Error Handling**: Global exception handling
- **Validation**: Input validation across all endpoints
- **Testing**: Unit and integration tests

## 🗄️ Database Schema

### Key Entities
- **UserDetails1**: User information and authentication data
- **Crop**: Crop types and information
- **CropDisease**: Disease reports and diagnoses
- **CropRecommendation**: Agricultural recommendations
- **LocationMapping**: Geographic location data
- **Otpdata**: OTP verification records

### Relationships
- Users can have multiple crop disease reports
- Crop recommendations are linked to users and locations
- Location mapping supports geographic-based services

## 🛠️ Development

### Project Structure
```
src/main/java/com/example/
├── agriconnect/
│   ├── Controller/         # REST controllers
│   ├── Service/           # Business logic
│   ├── Repository/        # Data access layer
│   └── AgriconnectApplication.java
├── Authentication/
│   ├── Controller/        # Auth controllers
│   ├── Service/          # Auth services
│   ├── Configuration/    # Security config
│   └── Model/           # Auth models
└── common/
    ├── Model/           # Shared entities
    ├── DTO/            # Data transfer objects
    ├── util/           # Utility classes
    └── config/         # Common configurations
```

### Coding Standards
- Follow Java naming conventions
- Use Spring Boot best practices
- Implement proper error handling
- Write comprehensive unit tests
- Document all public APIs

### Adding New Features
1. Create feature branch from main
2. Implement feature in appropriate module
3. Add unit tests
4. Update documentation
5. Submit pull request

## 🚀 Deployment

### Production Checklist
- [ ] Configure production database
- [ ] Set up Redis cluster
- [ ] Configure external API keys
- [ ] Set up monitoring and logging
- [ ] Configure reverse proxy (Nginx)
- [ ] Set up SSL certificates
- [ ] Configure backup strategies

### Environment-Specific Configurations
Use Spring profiles for different environments:
```properties
# application-prod.properties
spring.profiles.active=prod
spring.jpa.hibernate.ddl-auto=validate
logging.level.com.example=WARN
```

### Docker Deployment
```bash
# Build and run with Docker Compose
docker-compose up -d
```

## 🤝 Contributing

### Development Process
1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Ensure all tests pass
6. Submit a pull request

### Code Review Guidelines
- Code must pass all tests
- Follow established coding standards
- Include appropriate documentation
- Update README if necessary

## 📞 Support

### Getting Help
- **Documentation**: Check this README and inline code comments
- **Issues**: Report bugs and feature requests via GitHub Issues
- **Discussions**: Join community discussions for questions and ideas

### Common Issues
1. **Database Connection**: Ensure MySQL is running and credentials are correct
2. **Redis Connection**: Verify Redis server is running
3. **Port Conflicts**: Check if ports 8084 and 8081 are available
4. **API Keys**: Ensure all external service API keys are configured

### Performance Optimization
- Monitor database query performance
- Use Redis caching effectively
- Optimize image processing workflows
- Implement pagination for large datasets

---

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🙏 Acknowledgments

- Spring Boot team for the excellent framework
- OpenWeather for weather API services
- Cloudinary for image management services
- All contributors to the open-source libraries used

---

**Built with ❤️ for the agricultural community**
