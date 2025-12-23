# PROJECT SELECTION PHASE

## 1.1 Software Bid

### Project Information
- **Project Name:** CarDealer2 - Car Dealership Management System
- **Client:** Car Dealership Business
- **Proposed Technology Stack:** Android (Kotlin), Jetpack Compose, Firebase (Firestore, Storage)
- **Estimated Duration:** 12-16 weeks
- **Team Size:** 3-5 developers
- **Budget Range:** $25,000 - $40,000

### Executive Summary
We propose to develop a comprehensive Car Dealership Management System (CarDealer2) that will digitize and streamline all operations of a car dealership business. The system will manage vehicle inventory, customer relationships, broker networks, purchase and sales transactions, payment tracking, and generate catalogs for marketing purposes.

### Technical Approach
- **Frontend:** Android native application using Kotlin and Jetpack Compose for modern, responsive UI
- **Backend:** Firebase Firestore for real-time database operations
- **Storage:** Firebase Storage for images and PDF documents
- **Architecture:** Repository Pattern with MVVM architecture
- **State Management:** StateFlow for reactive data flow
- **Real-time Updates:** Firestore snapshot listeners for automatic data synchronization

### Key Features
1. **Vehicle Management**
   - Brand and model management
   - Vehicle inventory tracking
   - Vehicle details (chassis number, condition, images, documents)
   - Vehicle search and filtering

2. **Customer Management**
   - Customer registration and profile management
   - ID proof verification
   - Customer transaction history
   - Amount tracking (owed/receivable)

3. **Broker Management**
   - Broker registration and management
   - Broker transaction tracking
   - Commission management

4. **Purchase Management**
   - Vehicle purchase from owners/brokers
   - Multiple payment methods (cash, bank, credit)
   - GST calculation
   - Order number generation
   - Capital transaction tracking

5. **Sales Management**
   - Vehicle sales to customers
   - Full payment, down payment, and EMI options
   - EMI schedule management
   - Payment tracking

6. **Financial Management**
   - Payment history
   - EMI due tracking
   - Capital transaction records

7. **Catalog Generation**
   - PDF catalog generation
   - Customizable vehicle selection
   - Brand-based catalog filtering

### Competitive Advantages
- **Real-time Synchronization:** Automatic data updates across all devices
- **Offline Support:** Firebase offline persistence for uninterrupted operations
- **Scalable Architecture:** Repository pattern ensures maintainability and testability
- **Modern UI/UX:** Jetpack Compose provides smooth, native Android experience
- **Document Management:** Integrated storage for images and PDFs
- **Financial Tracking:** Comprehensive payment and transaction management

### Deliverables
1. Fully functional Android application
2. Complete source code with documentation
3. User manual and training materials
4. Deployment guide
5. Maintenance and support documentation

### Timeline
- **Phase 1 (Weeks 1-4):** Requirements analysis and design
- **Phase 2 (Weeks 5-8):** Core development (Vehicle, Customer, Broker management)
- **Phase 3 (Weeks 9-12):** Transaction management and financial features
- **Phase 4 (Weeks 13-14):** Testing and bug fixes
- **Phase 5 (Weeks 15-16):** Deployment and documentation

### Risk Mitigation
- **Technology Risks:** Mitigated through use of proven Firebase platform
- **Data Loss Risks:** Firebase automatic backups and offline persistence
- **Performance Risks:** Optimized queries and caching strategies
- **Security Risks:** Firebase authentication and security rules

### Support and Maintenance
- 3 months post-deployment support included
- Bug fixes and minor enhancements
- Training sessions for end users
- Documentation updates

---

## 1.2 Project Overview

### Project Description
CarDealer2 is a comprehensive mobile application designed to manage all aspects of a car dealership business. The system provides a centralized platform for managing vehicle inventory, customer relationships, broker networks, purchase and sales transactions, and financial operations.

### Business Objectives
1. **Digital Transformation:** Replace manual record-keeping with digital system
2. **Efficiency Improvement:** Streamline operations and reduce paperwork
3. **Data Accuracy:** Eliminate human errors in data entry and calculations
4. **Real-time Tracking:** Monitor inventory, sales, and payments in real-time
5. **Customer Service:** Improve customer relationship management
6. **Financial Control:** Better tracking of payments, EMI schedules, and capital transactions

### Target Users
- **Primary Users:** Car dealership staff (salespersons, managers, administrators)
- **Secondary Users:** Business owners (for reports and analytics)

### System Scope

#### In Scope
- Vehicle inventory management
- Customer management
- Broker management
- Purchase transaction processing
- Sales transaction processing (Full payment, Down payment, EMI)
- Payment tracking and EMI schedule management
- Document management (images, PDFs)
- Catalog generation
- Real-time data synchronization

#### Out of Scope
- Accounting system integration
- Third-party payment gateway integration
- Advanced analytics and reporting dashboards
- Multi-language support
- Web application version
- Mobile app for customers (B2C)

### Technology Stack

#### Frontend
- **Language:** Kotlin
- **UI Framework:** Jetpack Compose
- **Architecture:** MVVM (Model-View-ViewModel)
- **Navigation:** Jetpack Navigation Component
- **State Management:** StateFlow, LiveData
- **Image Loading:** Coil (Compose Image Loader)

#### Backend
- **Database:** Firebase Firestore (NoSQL)
- **Storage:** Firebase Storage
- **Real-time Updates:** Firestore Snapshot Listeners
- **Offline Support:** Firebase Offline Persistence

#### Development Tools
- **IDE:** Android Studio
- **Build System:** Gradle (Kotlin DSL)
- **Version Control:** Git
- **Testing:** JUnit, Espresso

### System Architecture

#### High-Level Architecture
```
┌─────────────────────────────────────────┐
│         Android Application            │
│  (Jetpack Compose UI + ViewModels)     │
└──────────────┬──────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────┐
│         Repository Layer                 │
│  (Vehicle, Customer, Broker, Purchase,   │
│   Sale Repositories)                    │
└──────────────┬──────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────┐
│         Firebase Services               │
│  (Firestore + Storage)                  │
└─────────────────────────────────────────┘
```

#### Key Components
1. **UI Layer (Screens)**
   - HomeScreen
   - Vehicle management screens
   - Customer management screens
   - Broker management screens
   - Transaction screens
   - Payment screens

2. **ViewModel Layer**
   - HomeScreenViewModel
   - BrandVehicleViewModel
   - CustomerFormViewModel
   - PurchaseVehicleViewModel
   - SellVehicleViewModel
   - PaymentsViewModel

3. **Repository Layer**
   - VehicleRepository
   - CustomerRepository
   - BrokerRepository
   - PurchaseRepository
   - SaleRepository

4. **Data Layer**
   - Brand, Product, Customer, Broker
   - Purchase, VehicleSale
   - CapitalTransaction

### Key Features Overview

#### 1. Vehicle Management
- Add vehicles with complete details (brand, model, chassis number, condition, images, documents)
- Edit and delete vehicles
- View vehicles by brand
- Search and filter vehicles
- Track vehicle status (available/sold)

#### 2. Customer Management
- Register new customers with ID proof
- View customer list
- Edit customer details
- Track customer transactions and amounts

#### 3. Broker Management
- Register brokers
- Manage broker information
- Track broker transactions

#### 4. Purchase Management
- Record vehicle purchases from owners or brokers
- Support for multiple payment methods
- Automatic GST calculation
- Order number generation
- Capital transaction recording

#### 5. Sales Management
- Record vehicle sales
- Support for three payment types:
  - Full Payment
  - Down Payment
  - EMI (Equated Monthly Installments)
- EMI schedule generation
- Payment tracking

#### 6. Financial Management
- View all payments
- Track EMI due dates
- Monitor capital transactions
- Payment history

#### 7. Catalog Generation
- Generate PDF catalogs
- Select vehicles for catalog
- Brand-based filtering

### Data Flow
The application follows a reactive data flow pattern:
```
Firestore Database
    ↓ (Real-time listeners)
Repository StateFlow
    ↓ (Observed by)
ViewModel StateFlow
    ↓ (Observed by)
UI Components (Compose)
```

### Security Considerations
- Firebase Security Rules for data access control
- Document validation before storage
- Chassis number uniqueness enforcement
- Transaction-based operations for data consistency

### Performance Optimizations
- In-memory caching using StateFlow
- Firestore query optimization
- Image compression before upload
- Lazy loading of data
- Offline persistence support

### Future Enhancements (Post-MVP)
- Advanced reporting and analytics
- Multi-user role management
- Push notifications for EMI due dates
- Export functionality (Excel, PDF reports)
- Integration with accounting software
- Customer mobile app
- Web dashboard for management

### Success Criteria
1. All core features implemented and tested
2. Application runs smoothly on Android 7.0+ devices
3. Real-time data synchronization working correctly
4. Offline functionality operational
5. User acceptance testing passed
6. Documentation complete

### Project Constraints
- **Platform:** Android only (iOS not included)
- **Minimum Android Version:** Android 7.0 (API 24)
- **Internet Dependency:** Requires internet for real-time sync (offline mode available)
- **Firebase Dependency:** System requires Firebase account and configuration

### Assumptions
- Users have Android devices (7.0+)
- Stable internet connection available
- Firebase account and project setup completed
- Users are trained on basic Android app usage


