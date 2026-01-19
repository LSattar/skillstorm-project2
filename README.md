# ReserveOne – Hotel Booking Platform

A full-stack hotel reservation management system built with Spring Boot and Angular.  
The platform supports OAuth2 authentication, real-time booking management, and comprehensive room inventory control.

---

## 🖥️ Screenshots

### Landing Page
![Landing Page](https://raw.githubusercontent.com/LSattar/skillstorm-project2/main/frontend/public/images/readme-screenshots/landing-page.png)

### Admin Dashboard
![Admin Dashboard](https://raw.githubusercontent.com/LSattar/skillstorm-project2/main/frontend/public/images/readme-screenshots/admin-dashboard.png)

### System Settings
![System Settings](https://raw.githubusercontent.com/LSattar/skillstorm-project2/main/frontend/public/images/readme-screenshots/system-settings.png)

### Profile Settings
![Profile Settings](https://raw.githubusercontent.com/LSattar/skillstorm-project2/main/frontend/public/images/readme-screenshots/profile-settings.png)

### Search Reservations
![Search Reservations](https://raw.githubusercontent.com/LSattar/skillstorm-project2/main/frontend/public/images/readme-screenshots/search-reservations.png)

### Recent Bookings
![Recent Bookings](https://raw.githubusercontent.com/LSattar/skillstorm-project2/main/frontend/public/images/readme-screenshots/recent-bookings.png)

### Rooms Management
![Rooms Management](https://raw.githubusercontent.com/LSattar/skillstorm-project2/main/frontend/public/images/readme-screenshots/manage-rooms.png)

### Room Selection & Booking
![Rooms Selection and Booking](https://raw.githubusercontent.com/LSattar/skillstorm-project2/main/frontend/public/images/readme-screenshots/rooms-page.png)

### Payment Transactions
![Payment Transactions](https://raw.githubusercontent.com/LSattar/skillstorm-project2/main/frontend/public/images/readme-screenshots/payment-transactions.png)




---
```
docs/
└── screenshots/
    ├── landing-page.png
    ├── hotel-search.png
    ├── reservation-flow.png
    ├── payment-confirmation.png
    └── admin-dashboard.png
```
---

## 🏗️ Architecture

### Backend
- **Framework:** Spring Boot 3.x  
- **Database:** PostgreSQL (pgcrypto, citext, btree_gist)  
- **Security:** Spring Security with OAuth2 (Google)  
- **ORM:** Hibernate / JPA  
- **Architecture:** RESTful API with service-layer pattern  

### Frontend
- **Framework:** Angular 21  
- **Language:** TypeScript  
- **Styling:** CSS  
- **Build Tool:** Angular CLI  

---

## ✨ Features

### Authentication and Authorization
- OAuth2 integration with Google Sign-In  
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
- Reservation status tracking:
  - Pending  
  - Confirmed  
  - Checked-In  
  - Checked-Out  
  - Cancelled  
- Special request handling  

### Room and Inventory Management
- Room types with amenities  
- Real-time room availability tracking  
- Room status management:
  - Available  
  - Occupied  
  - Maintenance  
  - Out of Service  
- Reservation holds with expiration  
- Overlap prevention using PostgreSQL exclusion constraints  

### Payment Processing
- Support for multiple payment providers:
  - Stripe  
  - PayPal  
  - Adyen  
- Transaction tracking and status management  
- Multi-currency support  

---

## 🚀 Getting Started

### Prerequisites
- Java 17+  
- Node.js 18+ and npm 10+  
- PostgreSQL 14+  
- Maven 3.8+  

---

### Backend Setup

Navigate to the backend directory:

```bash
cd backend/reserveone

DB_URL=jdbc:postgresql://localhost:5432/reserveone_db
DB_USERNAME=postgres
DB_PASSWORD=your_password
GOOGLE_CLIENT_ID=your_google_client_id
GOOGLE_CLIENT_SECRET=your_google_client_secret
SERVER_PORT=8080
SQL_INIT_MODE=always

psql -U postgres -c "CREATE DATABASE reserveone_db;"

./mvnw clean install
./mvnw spring-boot:run

mvn clean install
mvn spring-boot:run

http://localhost:8080

cd frontend
npm install
ng serve
http://localhost:4200
```

## 📊 Database Schema

### Core Tables
- **users** – User profiles and authentication  
- **roles** – User roles for RBAC  
- **oauth_identities** – OAuth provider links  
- **hotels** – Hotel information  
- **room_types** – Room definitions with pricing  
- **rooms** – Individual room inventory  
- **amenities** – Available amenities  
- **reservations** – Booking records  
- **reservation_holds** – Temporary room holds  
- **payment_transactions** – Payment tracking  

### Key Features
- UUID primary keys using `gen_random_uuid()`  
- Automatic timestamps via database triggers  
- Case-insensitive email handling with `citext`  
- Date range overlap prevention using GiST exclusion constraints  
- Foreign key cascades for data integrity  

---

## 🔐 API Endpoints

### Authentication
- `GET /auth/me` – Get current user  
- `POST /logout` – Logout  

### Users
- `GET /users/{id}` – Get user (Requires GUEST+)  
- `PATCH /users/{id}` – Update user (Requires GUEST+)  
- `DELETE /users/{id}` – Delete user (Requires ADMIN, BUSINESS_OWNER)  
- `PATCH /users/{id}/status` – Update user status (Requires ADMIN)  

### Hotels
- `GET /hotels` – List hotels (Public)  
- `GET /hotels/{id}` – Hotel details (Public)  

### Rooms
- `GET /rooms/hotel/{hotelId}` – Rooms by hotel (Public)  

### Reservations
- `POST /reservations` – Create reservation  
- `GET /reservations/{id}` – Get reservation  
- `GET /reservations` – List reservations  
  - Query params: `userId`, `hotelId`, `roomId`  
- `PUT /reservations/{id}` – Update reservation  
- `DELETE /reservations/{id}` – Delete reservation  
- `POST /reservations/{id}/cancel` – Cancel reservation  

### Reservation Holds
- `POST /reservation-holds` – Create hold  
- `GET /reservation-holds/{id}` – Get hold  
- `GET /reservation-holds` – List holds  
- `PUT /reservation-holds/{id}` – Update hold  
- `DELETE /reservation-holds/{id}` – Delete hold  
- `POST /reservation-holds/{id}/cancel` – Cancel hold  
- `POST /reservation-holds/expire` – Expire old holds  

---

## 🛡️ Security

### CORS Configuration
Allowed origins:
- `http://localhost:4200`  
- `http://127.0.0.1:4200`  

### CSRF Protection
- Cookie-based CSRF token repository  
- Cookie name: `XSRF-TOKEN`  
- Header name: `X-XSRF-TOKEN`  

### Session Management
- Session creation policy: `IF_REQUIRED`  
- Cookie-based sessions  
- Credentials allowed for CORS  

---

## 🧪 Testing

### Backend
```bash
cd backend/reserveone
./mvnw test
```

### Frontend
```bash
cd frontend
ng build
```

## 📝 Exception Handling

Global exception handling returns RFC 7807 Problem Details for:
- 400 Bad Request  
- 401 Unauthorized  
- 403 Forbidden  
- 404 Not Found  
- 409 Conflict  
- 500 Internal Server Error  

---

## 🔄 Data Validation

### Reservation Rules
- End date must be after start date  
- Start date cannot be in the past  
- Guest count must not exceed room capacity  
- No overlapping reservations for the same room  
- Currency must be a valid ISO 4217 code  

### User Constraints
- Unique case-insensitive email addresses  
- Valid two-character state codes  
- ZIP code length between 5 and 10  
- Phone number format validation  

---

## 📂 Project Structure

```text
skillstorm-project2/
├── backend/
│   └── reserveone/
│       ├── src/
│       │   ├── main/
│       │   │   ├── java/com/skillstorm/reserveone/
│       │   │   │   ├── config/
│       │   │   │   ├── controllers/
│       │   │   │   ├── dto/
│       │   │   │   ├── exceptions/
│       │   │   │   ├── models/
│       │   │   │   └── services/
│       │   │   └── resources/
│       │   │       ├── application.yml
│       │   │       ├── schema.sql
│       │   │       └── data.sql
│       │   └── test/
│       ├── pom.xml
│       └── .env
└── frontend/
    ├── src/
    │   ├── app/features/landing/
    │   ├── index.html
    │   └── main.ts
    ├── angular.json
    └── package.json
```

## 🤝 Contributing
1. Fork the repository  
2. Create a feature branch (`git checkout -b feature/amazing-feature`)  
3. Commit your changes (`git commit -m "Add amazing feature"`)  
4. Push to the branch (`git push origin feature/amazing-feature`)  
5. Open a Pull Request  

---

## 📄 License
This project was developed as part of a SkillStorm training program.

---

## 👥 Authors
- Joshua Thompson – Developer  
- Leah Satter – Developer  
