# 🌾 AgriConnect - Smart Agricultural Management Platform

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.11-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-blue.svg)](https://www.mysql.com/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

## 📋 Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Technology Stack](#technology-stack)
- [Prerequisites](#prerequisites)
- [Installation & Setup](#installation--setup)
- [Configuration](#configuration)
- [API Endpoints](#api-endpoints)
- [Usage](#usage)
- [External Dependencies](#external-dependencies)
- [Contributing](#contributing)
- [License](#license)
- [Contact](#contact)

## 🌟 Overview

**AgriConnect** is a comprehensive smart agricultural management platform that empowers farmers with AI-driven insights, crop recommendations, price predictions, and disease detection capabilities. Built with modern web technologies, it provides an intuitive interface for agricultural decision-making.

### Key Highlights

- 🤖 **AI-Powered Recommendations**: Machine learning-based crop suggestions
- 💰 **Price Prediction**: Market price forecasting with trend analysis
- 🔍 **Disease Detection**: Image-based crop disease identification
- 🌍 **Multi-Language Support**: Available in English and Hindi
- 📱 **Responsive Design**: Works seamlessly across all devices
- ☁️ **Weather Integration**: Real-time weather data and agricultural advice

## ✨ Features

### 🔐 User Management
- Secure user registration and authentication
- Email verification with OTP
- Profile management with account controls
- Password reset functionality
- Multi-status user accounts (Active, Inactive, Deleted, Blocked)

### 🌱 Crop Recommendation System
- **Input Parameters**: Nitrogen (N), Phosphorus (P), Potassium (K), Temperature, Humidity, pH, Rainfall
- **AI Analysis**: Machine learning algorithms for optimal crop selection
- **Bilingual Results**: Recommendations in English and Hindi
- **Historical Tracking**: View past recommendations and their outcomes

### 💹 Price Prediction Engine
- **Market Analysis**: Real-time crop price predictions
- **Location-Based**: State, District, and Market-specific pricing
- **Multiple Price Points**: Min, Max, and Suggested pricing
- **Trend Visualization**: Historical price data and forecasting

### 🦠 Disease Management
- **Image Upload**: Upload crop images for disease detection
- **AI Diagnosis**: Automated disease identification using computer vision
- **Comprehensive Database**: Detailed disease information with causes, symptoms, and treatments
- **Critical Alerts**: Monitoring and alerts for severe disease conditions
- **Export Functionality**: Download disease data as CSV

### 🖼️ Image Analysis
- **Crop Analysis**: AI-powered crop image analysis
- **Cloud Storage**: Secure image storage via Cloudinary
- **Gemini Integration**: Advanced AI analysis capabilities

### 🌤️ Weather Services
- **Location-Based**: GPS-enabled weather data
- **Agricultural Advice**: Weather-based farming recommendations
- **Multi-Language**: Weather information in preferred language

### 💬 Intelligent Chatbot
- **24/7 Support**: AI-powered agricultural assistant
- **FAQ Handling**: Common agricultural queries
- **Multi-Language**: Support in English and Hindi

## 🛠️ Technology Stack

### Backend
- **Framework**: Spring Boot 3.2.11
- **Language**: Java 21
- **Security**: Spring Security 6
- **Database**: MySQL 8.0
- **ORM**: Spring Data JPA with Hibernate
- **Build Tool**: Maven
- **Template Engine**: Thymeleaf

### Frontend
- **CSS Framework**: Tailwind CSS
- **JavaScript**: Vanilla JS with jQuery
- **Animations**: Animate.css
- **Icons**: Font Awesome 6
- **Notifications**: SweetAlert2
- **Charts**: Chart.js

### External Services
- **Cloud Storage**: Cloudinary
- **AI Services**: Gemini API
- **Image Services**: Pexels API
- **Email**: Spring Mail with Gmail SMTP
- **ML Services**: Custom Flask APIs

### Development Tools
- **Hot Reload**: Spring DevTools
- **Code Generation**: Lombok
- **Validation**: Jakarta Validation
- **Logging**: SLF4J
- **Web Scraping**: JSoup

## 📋 Prerequisites

Before you begin, ensure you have the following installed:

- ☕ **Java 21** or higher
- 🗄️ **MySQL Server 8.0** or higher
- 🔧 **Maven 3.6** or higher
- 🌐 **Git** (for cloning the repository)

## 🚀 Installation & Setup

### 1. Clone the Repository
```bash
git clone https://github.com/yourusername/agriconnect.git
cd agriconnect
```

### 2. Database Setup
```sql
-- Create database
CREATE DATABASE shobhitdatabase;

-- Create user (optional)
CREATE USER 'agriconnect_user'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON shobhitdatabase.* TO 'agriconnect_user'@'localhost';
FLUSH PRIVILEGES;
```

### 3. Environment Configuration
Create a `.env` file in the root directory or set environment variables:

```env
# Database Configuration
DB_HOST_IP=localhost
DB_USERNAME=root
DB_PASS=your_mysql_password

# API Keys
GEMINI_KEY=your_gemini_api_key
PEXELS_KEY=your_pexels_api_key

# Email Configuration
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_app_password

# SSL Configuration (optional)
SSL_PASSWORD=your_ssl_password

# External Services
HOST_IP=localhost
```

### 4. Build and Run
```bash
# Using Maven Wrapper (Recommended)
./mvnw clean install
./mvnw spring-boot:run

# Or using Maven directly
mvn clean install
mvn spring-boot:run

# For Windows
mvnw.cmd clean install
mvnw.cmd spring-boot:run
```

### 5. Access the Application
Open your browser and navigate to:
```
http://localhost:8084
```

## ⚙️ Configuration

### Application Properties
Key configuration options in `application.properties`:

```properties
# Server Configuration
server.port=8084

# Database Settings
spring.datasource.url=jdbc:mysql://${DB_HOST_IP:localhost}:3306/shobhitdatabase
spring.datasource.username=${DB_USERNAME:root}
spring.datasource.password=${DB_PASS:password}

# JPA/Hibernate Settings
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# File Upload Configuration
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB

# External API URLs
flask.api.url=http://${HOST_IP:localhost}:8080/recommend
flask.api.url2=http://${HOST_IP:localhost}:8081/predict
flask.api.url3=http://${HOST_IP:localhost}:8082
```

### SSL Configuration (Optional)
To enable HTTPS, uncomment and configure:
```properties
server.ssl.enabled=true
server.ssl.key-store=classpath:keystore.p12
server.ssl.key-store-type=PKCS12
server.ssl.key-store-password=${SSL_PASSWORD}
server.ssl.key-alias=myserver
```

## 🌐 API Endpoints

### Authentication
- `GET /api/login` - Login page
- `POST /api/login` - Authenticate user
- `GET /api/register` - Registration page
- `POST /api/register` - Register new user
- `GET /api/logout-success` - Logout confirmation

### User Management
- `GET /api/profile` - User profile page
- `POST /api/profile/update-info` - Update profile information
- `POST /api/profile/change-password` - Change password
- `POST /api/profile/activate` - Activate account
- `POST /api/profile/deactivate` - Deactivate account

### Crop Services
- `GET /api/recommend` - Crop recommendation form
- `POST /api/recommend` - Submit recommendation request
- `GET /api/dashboard1` - Recommendation dashboard
- `GET /api/predict` - Price prediction form
- `POST /api/predict` - Submit price prediction
- `GET /api/dashboard` - Price dashboard

### Disease Management
- `GET /api/diseases/dashboard` - Disease tracking dashboard
- `GET /api/diseases/export` - Export disease data

### Image Analysis
- `GET /api/ImageAnalysis` - Image analysis page
- `POST /api/ImageAnalysis/analyze` - Analyze uploaded image

### Utility Services
- `GET /api/weather` - Weather data API
- `GET /api/ChatBot` - Chatbot interface
- `GET /api/get-api-key` - Get API key

## 📱 Usage

### Getting Started
1. **Register**: Create a new account with email verification
2. **Login**: Access your dashboard
3. **Choose Service**: Select from available agricultural services

### Crop Recommendation
1. Navigate to **Crop Recommendation**
2. Enter soil parameters (N, P, K values)
3. Provide environmental data (temperature, humidity, rainfall, pH)
4. Get AI-powered crop suggestions with detailed descriptions

### Price Prediction
1. Go to **Price Prediction**
2. Select location (State, District, Market)
3. Choose crop type and enter current market data
4. Receive price forecasts and trend analysis

### Disease Detection
1. Access **Disease Dashboard**
2. Upload crop images
3. Get automated disease identification
4. View treatment recommendations and preventive measures

### Weather Services
1. Enable location services or enter coordinates
2. Get real-time weather data
3. Receive agricultural advice based on weather conditions

## 🔗 External Dependencies

### Machine Learning Services
The application integrates with external Flask APIs:

- **Recommendation Service**: `http://localhost:8080/recommend`
- **Prediction Service**: `http://localhost:8081/predict`
- **Additional Service**: `http://localhost:8082`

### Required API Keys
- **Gemini API**: For AI-powered analysis
- **Pexels API**: For image services
- **Gmail SMTP**: For email services

## 🤝 Contributing

We welcome contributions! Please follow these steps:

1. **Fork** the repository
2. **Create** a feature branch (`git checkout -b feature/AmazingFeature`)
3. **Commit** your changes (`git commit -m 'Add some AmazingFeature'`)
4. **Push** to the branch (`git push origin feature/AmazingFeature`)
5. **Open** a Pull Request

### Development Guidelines
- Follow Java coding standards
- Write comprehensive tests
- Update documentation
- Ensure responsive design
- Support multi-language features

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 📞 Contact

**Project Maintainer**: Shobhit Srivastava  
**Email**: shobhitsrivastava2004@gmail.com  
**Project Link**: [https://github.com/yourusername/agriconnect](https://github.com/yourusername/agriconnect)

---

## 🙏 Acknowledgments

- Spring Boot community for the excellent framework
- Agricultural experts for domain knowledge
- Open source contributors
- AI/ML service providers

---

## 🔄 Version History

- **v1.0.0** - Initial release with core features
- **v1.1.0** - Added disease detection
- **v1.2.0** - Enhanced UI/UX with multi-language support
- **v1.3.0** - Integrated weather services and chatbot

---

**Happy Farming! 🌾**
