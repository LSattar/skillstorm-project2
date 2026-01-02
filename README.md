# ReserveOne - Hotel Booking Platform

A full-stack hotel reservation management system built with Spring Boot and Angular, featuring OAuth2 authentication, real-time booking management, and comprehensive room inventory control.

## 🏗️ Architecture

### Backend
- **Framework**: Spring Boot 3.x
- **Database**: PostgreSQL with advanced features (pgcrypto, citext, btree_gist)
- **Security**: Spring Security with OAuth2 (Google)
- **ORM**: Hibernate/JPA
- **Architecture**: RESTful API with service layer pattern

### Frontend
- **Framework**: Angular 21
- **Language**: TypeScript
- **Styling**: CSS
- **Build Tool**: Angular CLI

## ✨ Features

### Authentication & Authorization
- OAuth2 integration (Google Sign-In)
- Role-Based Access Control (RBAC)
  - Guest
  - Employee
  - Manager
  - Business Owner
  - Admin
- Session management with CSRF protection

### Reservation Management
- Create, read, update, and cancel reservations
- Guest count validation
- Date range conflict detection
- Reservation status tracking (Pending, Confirmed, Checked-In, Checked-Out, Cancelled)
- Special requests handling

### Room & Inventory Management
- Room types with amenities
- Room availability tracking
- Status management (Available, Occupied, Maintenance, Out of Service)
- Reservation holds with expiration
- Overlap prevention using PostgreSQL exclusion constraints

### Payment Processing
- Support for multiple payment providers (Stripe, PayPal, Adyen)
- Transaction tracking and status management
- Multi-currency support

## 🚀 Getting Started

### Prerequisites
- Java 17+
- Node.js 18+ & npm 10+
- PostgreSQL 14+
- Maven 3.8+

### Backend Setup

1. **Navigate to the backend directory**
```bash
cd backend/reserveone
```

2. **Configure environment variables**

Create a `.env` file in the `backend/reserveone` directory:
```properties
DB_URL=jdbc:postgresql://localhost:5432/reserveone_db
DB_USERNAME=postgres
DB_PASSWORD=your_password
GOOGLE_CLIENT_ID=your_google_client_id
GOOGLE_CLIENT_SECRET=your_google_client_secret
SERVER_PORT=8080
SQL_INIT_MODE=always
```

3. **Initialize the database**
```bash
# Create the database
psql -U postgres -c "CREATE DATABASE reserveone_db;"

# The schema and initial data will be loaded automatically on first run
```

4. **Build and run**
```bash
# Using Maven wrapper
./mvnw clean install
./mvnw spring-boot:run

# Or using Maven
mvn clean install
mvn spring-boot:run
```

The backend API will be available at `http://localhost:8080`

### Frontend Setup

1. **Navigate to the frontend directory**
```bash
cd frontend
```

2. **Install dependencies**
```bash
npm install
```

3. **Start the development server**
```bash
ng serve
```

The frontend application will be available at `http://localhost:4200`

## 📊 Database Schema

### Core Tables
- **users**: User profiles and authentication
- **roles**: User roles for RBAC
- **oauth_identities**: OAuth provider links
- **hotels**: Hotel information
- **room_types**: Room type definitions with pricing
- **rooms**: Individual room inventory
- **amenities**: Available amenities
- **reservations**: Booking records
- **reservation_holds**: Temporary room holds
- **payment_transactions**: Payment tracking

### Key Features
- UUID primary keys generated via `gen_random_uuid()`
- Automatic timestamp management with triggers
- Case-insensitive email handling with `citext`
- Date range overlap prevention using GiST exclusion constraints
- Foreign key cascades for data integrity

## 🔐 API Endpoints

### Authentication
- `GET /auth/me` - Get current user info
- `POST /logout` - User logout

### Users
- `GET /users/{id}` - Get user by ID (Requires: GUEST+)
- `PATCH /users/{id}` - Update user (Requires: GUEST+)
- `DELETE /users/{id}` - Delete user (Requires: ADMIN, BUSINESS_OWNER)
- `PATCH /users/{id}/status` - Update user status (Requires: ADMIN)

### Hotels
- `GET /hotels` - List all hotels (Public)
- `GET /hotels/{id}` - Get hotel details (Public)

### Rooms
- `GET /rooms/hotel/{hotelId}` - Get rooms by hotel (Public)

### Reservations
- `POST /reservations` - Create reservation (Requires: GUEST+)
- `GET /reservations/{id}` - Get reservation (Requires: GUEST+)
- `GET /reservations` - List reservations (Requires: GUEST+)
  - Query params: `userId`, `hotelId`, `roomId`
- `PUT /reservations/{id}` - Update reservation (Requires: GUEST+)
- `DELETE /reservations/{id}` - Delete reservation (Requires: GUEST+)
- `POST /reservations/{id}/cancel` - Cancel reservation (Requires: GUEST+)

### Reservation Holds
- `POST /reservation-holds` - Create hold
- `GET /reservation-holds/{id}` - Get hold
- `GET /reservation-holds` - List holds
  - Query params: `userId`, `roomId`
- `PUT /reservation-holds/{id}` - Update hold
- `DELETE /reservation-holds/{id}` - Delete hold
- `POST /reservation-holds/{id}/cancel` - Cancel hold
- `POST /reservation-holds/expire` - Expire old holds

## 🛡️ Security

### CORS Configuration
The backend is configured to accept requests from:
- `http://localhost:4200`
- `http://127.0.0.1:4200`

### CSRF Protection
- Cookie-based CSRF token repository
- Required for state-changing operations
- Cookie name: `XSRF-TOKEN`
- Header name: `X-XSRF-TOKEN`

### Session Management
- Session creation: `IF_REQUIRED`
- Cookie-based sessions
- Credentials allowed for CORS

## 🧪 Testing

### Backend Tests
```bash
cd backend/reserveone
./mvnw test
```

### Frontend Tests
```bash
cd frontend
ng test
```

## 📦 Building for Production

### Backend
```bash
cd backend/reserveone
./mvnw clean package -DskipTests
java -jar target/reserveone-0.0.1-SNAPSHOT.jar
```

### Frontend
```bash
cd frontend
ng build
# Build artifacts will be in dist/
```

## 📝 Exception Handling

The application uses a global exception handler that returns RFC 7807 Problem Details for:
- 404 Not Found - `ResourceNotFoundException`
- 409 Conflict - `ResourceConflictException`
- 400 Bad Request - Validation errors
- 401 Unauthorized - Authentication failures
- 403 Forbidden - Authorization failures
- 500 Internal Server Error - Unexpected errors

## 🔄 Data Validation

### Reservation Business Rules
- End date must be after start date
- Start date cannot be in the past
- Guest count must not exceed room type capacity
- No overlapping active reservations for the same room
- Currency must be a valid 3-letter ISO code

### User Constraints
- Unique email addresses (case-insensitive)
- Valid state codes (2 characters)
- ZIP code length: 5-10 characters
- Phone number format validation

## 📂 Project Structure

```
skillstorm-project2/
├── backend/
│   └── reserveone/                 # Main Spring Boot application
│       ├── src/
│       │   ├── main/
│       │   │   ├── java/com/skillstorm/reserveone/
│       │   │   │   ├── config/     # Security & app configuration
│       │   │   │   ├── controllers/# REST endpoints
│       │   │   │   ├── dto/        # Data transfer objects
│       │   │   │   ├── exceptions/ # Custom exceptions
│       │   │   │   ├── models/     # JPA entities
│       │   │   │   └── services/   # Business logic
│       │   │   └── resources/
│       │   │       ├── application.yml
│       │   │       ├── schema.sql  # Database schema
│       │   │       └── data.sql    # Seed data
│       │   └── test/
│       ├── pom.xml
│       └── .env
└── frontend/
    ├── src/
    │   ├── app/
    │   │   └── features/
    │   │       └── landing/        # Landing page components
    │   ├── index.html
    │   └── main.ts
    ├── angular.json
    └── package.json
```

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is part of a SkillStorm training program.

## 👥 Authors

- Joshua Thompson - Developer
- Leah Satter - Developer

## 🙏 Acknowledgments

- SkillStorm for project requirements and guidance
- Spring Boot and Angular communities for excellent documentation
