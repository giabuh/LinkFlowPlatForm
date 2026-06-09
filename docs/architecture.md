# LinkFlowPlatform Architecture

## 1. Architecture Style

LinkFlowPlatform uses a modular monolith architecture.

This means the application is deployed as one Spring Boot application, but the code is organized by business features.

This is simpler than microservices and more suitable for a portfolio-level production-style backend project.

## 2. Package Structure

Recommended package structure:

```txt
com.giabao.linkflow
├── LinkFlowPlatformApplication.java
├── common
├── auth
├── user
├── link
├── click
└── analytics
```
## 3. Exam Structure
```txt
├── controller
├── service
├── repository
├── entity
├── dto
│   ├── request
│   └── response
└── mapper