# DMRC Helper – Authentication Backend

A Java Spring Boot 3 backend for user authentication in the DMRC Helper Android app.
Supports registration via **email** or **phone number**, with verification, JWT-based login,
and refresh token support. Data is persisted in a local **SQLite** database.

---

## Technology Stack

| Layer            | Technology                                 |
|------------------|--------------------------------------------|
| Language         | Java 17                                    |
| Framework        | Spring Boot 3.2                            |
| Security         | Spring Security + BCrypt                   |
| Tokens           | JWT (io.jsonwebtoken 0.11.5)               |
| Database         | SQLite (via Hibernate Community Dialects)  |
| ORM              | Spring Data JPA / Hibernate                |
| Build tool       | Maven                                      |
| Boilerplate      | Lombok                                     |

---

## Project Structure

```
dmrc-helper-backend/
├── src/main/java/com/dmrc/helper/
│   ├── DmrcHelperApplication.java          # Main entry point
│   ├── config/
│   │   ├── SecurityConfig.java             # Spring Security + JWT filter
│   │   ├── JwtConfig.java                  # JWT property binding
│   │   └── CorsConfig.java                 # CORS configuration
│   ├── controller/
│   │   └── AuthController.java             # REST endpoints
│   ├── service/
│   │   ├── AuthService.java                # Business logic
│   │   ├── EmailService.java               # Mock email sender
│   │   └── OtpService.java                 # OTP generator + mock SMS sender
│   ├── repository/
│   │   ├── UserRepository.java
│   │   └── VerificationTokenRepository.java
│   ├── entity/
│   │   ├── User.java                       # User JPA entity
│   │   └── VerificationToken.java          # Token JPA entity
│   ├── dto/
│   │   ├── RegisterRequest.java
│   │   ├── LoginRequest.java
│   │   ├── AuthResponse.java
│   │   └── UserProfileResponse.java
│   ├── security/
│   │   ├── JwtTokenProvider.java           # JWT generation + validation
│   │   └── CustomUserDetailsService.java   # Loads user by email or phone
│   └── exception/
│       └── AuthException.java
├── src/main/resources/
│   └── application.properties
├── pom.xml
└── README.md
```

---

## API Endpoints

### Public Endpoints

| Method | URL                         | Description                                |
|--------|-----------------------------|--------------------------------------------|
| POST   | `/api/auth/register`        | Register with email or phone + password    |
| POST   | `/api/auth/verify-email`    | Verify email using the token from email    |
| POST   | `/api/auth/verify-phone`    | Verify phone using OTP                     |
| POST   | `/api/auth/login`           | Login with email/phone + password → JWT    |
| POST   | `/api/auth/refresh-token`   | Get new access token using refresh token   |

### Protected Endpoints (require `Authorization: Bearer <token>`)

| Method | URL                         | Description                    |
|--------|-----------------------------|--------------------------------|
| GET    | `/api/auth/user-profile`    | Get current user's profile     |

---

## Request / Response Examples

### Register with Email

**POST** `/api/auth/register`
```json
{
  "email": "user@example.com",
  "password": "MySecret123"
}
```

**Response 201:**
```json
{
  "accessToken": "<jwt>",
  "refreshToken": "<refresh-token>",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "userId": 1,
  "registrationType": "EMAIL"
}
```

### Register with Phone

**POST** `/api/auth/register`
```json
{
  "phone": "+911234567890",
  "password": "MySecret123"
}
```

### Verify Email

**POST** `/api/auth/verify-email`
```json
{ "token": "<verification-token-from-email>" }
```

### Verify Phone (OTP)

**POST** `/api/auth/verify-phone`
```json
{ "phone": "+911234567890", "otp": "123456" }
```

### Login

**POST** `/api/auth/login`
```json
{
  "loginId": "user@example.com",
  "password": "MySecret123"
}
```

### Refresh Token

**POST** `/api/auth/refresh-token`
```json
{ "refreshToken": "<refresh-token>" }
```

### Get Profile

**GET** `/api/auth/user-profile`
Headers: `Authorization: Bearer <access-token>`

**Response 200:**
```json
{
  "userId": 1,
  "email": "user@example.com",
  "phone": null,
  "isEmailVerified": true,
  "isPhoneVerified": false,
  "registrationType": "EMAIL",
  "createdAt": "2024-01-01T12:00:00"
}
```

---

## Database Schema

### `users`

| Column             | Type     | Notes                        |
|--------------------|----------|------------------------------|
| user_id            | INTEGER  | Primary key, auto-increment  |
| email              | TEXT     | Unique, nullable             |
| phone              | TEXT     | Unique, nullable             |
| password_hash      | TEXT     | BCrypt hash                  |
| is_email_verified  | INTEGER  | Boolean (0/1)                |
| is_phone_verified  | INTEGER  | Boolean (0/1)                |
| registration_type  | TEXT     | `EMAIL` or `PHONE`           |
| created_at         | TEXT     | ISO timestamp                |
| updated_at         | TEXT     | ISO timestamp                |

### `verification_tokens`

| Column      | Type     | Notes                                        |
|-------------|----------|----------------------------------------------|
| id          | INTEGER  | Primary key, auto-increment                  |
| token       | TEXT     | Unique token / OTP                           |
| user_id     | INTEGER  | FK → users.user_id                           |
| token_type  | TEXT     | `EMAIL_VERIFICATION`, `PHONE_OTP`, `REFRESH_TOKEN` |
| expires_at  | TEXT     | ISO timestamp                                |
| used        | INTEGER  | Boolean (0/1)                                |
| created_at  | TEXT     | ISO timestamp                                |

---

## Getting Started

### Prerequisites
- Java 17+
- Maven 3.8+

### Run

```bash
cd dmrc-helper-backend
mvn spring-boot:run
```

The server starts on `http://localhost:8080`.

### Build JAR

```bash
mvn clean package
java -jar target/dmrc-helper-backend-1.0.0.jar
```

### Run Tests

```bash
mvn test
```

---

## Security Notes

- Passwords are **never stored in plaintext** — BCrypt hashing is used.
- `passwordHash` and verification tokens are **never returned in API responses**.
- JWT access tokens expire in **15 minutes**; refresh tokens in **7 days**.
- Email and SMS services are **mock implementations** — replace with real providers
  (SendGrid, Twilio, AWS SES/SNS) before deploying to production.
- Change `jwt.secret` to a strong random value before deploying.
