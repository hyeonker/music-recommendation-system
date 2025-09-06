# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Development Commands

### Backend (Spring Boot)
- **Build**: `./gradlew build`
- **Run locally**: `./gradlew bootRun --args='--spring.profiles.active=local'`
- **Run tests**: `./gradlew test`
- **Clean build**: `./gradlew clean build`

### Frontend (React/TypeScript)
- **Install dependencies**: `cd frontend && npm install`
- **Development server**: `cd frontend && npm start` (runs on http://localhost:3000)
- **Build**: `cd frontend && npm run build`
- **Run tests**: `cd frontend && npm test`

### Docker
- **Build and run full stack**: `docker-compose up --build`
- **Run with existing images**: `docker-compose up`
- **Stop services**: `docker-compose down`

### Testing
- **Backend tests**: `./gradlew test`
- **Frontend tests**: `cd frontend && npm test`
- **Backend with coverage**: `./gradlew test jacocoTestReport`

## Architecture Overview

### Full-Stack Music Recommendation System
This is a comprehensive music recommendation platform with the following architecture:

**Backend (Java/Spring Boot)**
- **Framework**: Spring Boot 3.5.4 with Java 21
- **Database**: PostgreSQL with Flyway migrations
- **Security**: OAuth2 integration (Google, Kakao, Naver) + Spring Security
- **External APIs**: Spotify Web API integration for music data
- **Caching**: Caffeine for performance optimization
- **Real-time**: WebSocket support for chat functionality

**Frontend (React/TypeScript)**
- **Framework**: React 19+ with TypeScript
- **Styling**: TailwindCSS
- **State Management**: React Query for server state
- **UI Components**: Framer Motion, Lucide React icons
- **Real-time**: STOMP.js for WebSocket connections

**Key Architectural Patterns**:
- **Domain-driven structure**: Code organized by feature domains (user, music, recommendation, chat, etc.)
- **Repository pattern**: JPA repositories for data access
- **Service layer**: Business logic separation
- **DTO pattern**: Request/Response objects for API boundaries
- **Security layers**: OAuth2 handlers, custom user services
- **Scheduled jobs**: Background processing for recommendations and chat retention

### Core Features
1. **User Management**: OAuth2 authentication, user profiles
2. **Music Catalog**: Song management, Spotify integration
3. **Recommendation Engine**: User preference analysis, similarity matching
4. **Social Features**: User matching, chat rooms, playlist sharing
5. **Review System**: Music reviews with helpful voting
6. **Real-time Chat**: WebSocket-based messaging with encryption
7. **Analytics**: Usage statistics and dashboards

### Package Structure
```
com.example.musicrecommendation/
├── config/          # Spring configuration classes
├── domain/          # JPA entities and repositories  
├── security/        # OAuth2 handlers, encryption
├── service/         # Business logic layer
├── web/            # Controllers and DTOs
├── jobs/           # Scheduled background tasks
└── repository/     # Additional repository interfaces
```

### Database
- **Migration tool**: Flyway (see `src/main/resources/db/migration/`)
- **Connection**: PostgreSQL with Hikari connection pool
- **Profiles**: `local` profile connects to localhost:5432

### Configuration
- **Local development**: Uses `application-local.yml` profile
- **Server port**: 9090 (backend), 3000 (frontend dev server)
- **CORS**: Configured for frontend origin (localhost:3000)
- **Spotify API**: Client credentials configured in application.yml

### Development Workflow
1. Ensure PostgreSQL is running locally (database: `music`, user: `music`, password: `music`)
2. Backend runs on :9090 with `local` profile
3. Frontend proxy configured to backend (see `frontend/package.json`)
4. Both services can run simultaneously for full-stack development

### Key Dependencies & Technologies
**Backend Stack**:
- Spring Boot 3.5.4, Java 21
- PostgreSQL with Flyway migrations
- Spring Security with OAuth2 (Google, Kakao, Naver)
- Caffeine caching, Resilience4j for retries
- WebSocket/STOMP for real-time features
- Spotify Web API integration

**Frontend Stack**:
- React 19+ with TypeScript
- TailwindCSS for styling
- React Query (@tanstack/react-query) for server state
- Framer Motion for animations
- STOMP.js for WebSocket connections
- Axios for HTTP requests

### Important Notes
- Use `local` profile for development: `--spring.profiles.active=local`
- Database migrations are in `src/main/resources/db/migration/`
- Frontend uses proxy to backend in development (port 3000 → 9090)
- Docker setup uses `docker` profile and different port (18080)
- Chat messages are encrypted using AES (configured in application.yml)