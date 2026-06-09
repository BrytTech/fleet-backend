# Fleet Backend

A delivery management system built with Spring Boot.

## Features

- JWT Authentication
- Role-based access (Customer, Rider, Admin)
- Order management (create, accept, pickup, deliver, cancel)
- Rider verification with document upload
- PostgreSQL database

## Tech Stack

- Java 21
- Spring Boot 3
- Spring Security
- JWT
- PostgreSQL
- Maven

## API Endpoints

### Auth
- POST `/auth/register/customer`
- POST `/auth/register/rider`
- POST `/auth/login`

### Orders
- POST `/orders`
- GET `/orders/me`
- GET `/orders/available`
- POST `/orders/{id}/accept`
- PATCH `/orders/{id}/pickup`
- PATCH `/orders/{id}/deliver`

### Admin
- GET `/admin/riders/pending`
- POST `/admin/riders/{id}/approve`
- POST `/admin/riders/{id}/reject`

## Setup

1. Create PostgreSQL database
2. Set environment variables: `DB_PASSWORD`, `JWT_SECRET`
3. Run `mvn spring-boot:run`
