<img width="3554" height="1104" alt="Frame 6 (1)" src="https://github.com/user-attachments/assets/5cb94a7a-c50e-4a9c-94e0-6231f13e5656" />

## Project Overview - A Personal Finance Tracking App

The Vero - Personal Finance Tracker is an Android application designed to help users manage their personal finances by tracking income, expenses, freelance invoices, recurring payments, cryptocurrency profits and losses, and savings goals.

The application supports multiple income sources and currencies, including LKR and USD, allowing users to record transactions and maintain accurate financial records. It also provides a dashboard with monthly financial summaries, expense breakdowns, and savings goal progress.

[![GitHub last commit](https://img.shields.io/github/last-commit/sakith03/vero-mobile.svg)](https://github.com/sakith03/vero-mobile/commits/main)

### Key Features

- **User Authentication:** Register and log in securely using Firebase Authentication.
- **Income Tracking:** Record income from salaries, freelance projects, AdSense, and other sources.
- **Expense Management:** Record, edit, delete, categorize, and filter expenses.
- **Multi-Currency Support:** Track transactions in LKR and USD while preserving exchange-rate snapshots.
- **Freelance Invoice Management:** Create invoices, track due dates, and identify overdue payments.
- **Recurring Payments:** Manage recurring bills and subscriptions.
- **Savings Goals:** Set financial goals and monitor progress toward target amounts.
- **Cryptocurrency Tracking:** Record completed crypto trades and calculate realized profits or losses.
- **Financial Dashboard:** View income, expenses, surplus, savings progress, and spending breakdowns.
- **Offline Support:** Create, edit, and view records offline, with synchronization when connectivity returns.
- **Real-Time Updates:** Use Cloud Firestore listeners to update dashboard information when data changes.

## Technologies Used

- **Programming Language:** Kotlin
- **UI Framework:** Jetpack Compose
- **Design System:** Material Design 3
- **Architecture:** Model-View-ViewModel (MVVM)
- **Navigation:** Navigation Compose
- **Authentication:** Firebase Authentication
- **Cloud Database:** Cloud Firestore
- **Local Database:** Room with SQLite
- **Dependency Injection:** Hilt
- **Asynchronous Operations:** Kotlin Coroutines
- **UI State Management:** StateFlow or LiveData
- **Build System:** Gradle with Kotlin DSL

## Architectural Summary

The application follows the **Model-View-ViewModel (MVVM)** architecture to separate the user interface, application logic, and data management.

<img width="847" height="949" alt="Screenshot 2026-10-21 000358" src="https://github.com/user-attachments/assets/7d904530-a842-4501-9aa7-aa816f3d67f0" />

### Architecture Layers

- **View (Jetpack Compose):** Displays screens and UI components and sends user actions to the ViewModel. It does not access databases or Firebase directly.
- **ViewModel:** Handles UI-related logic and validation, communicates with repositories, and exposes UI state through StateFlow or LiveData.
- **Repository:** Acts as the bridge between ViewModels and data sources, managing data retrieval, storage, synchronization, and data-layer errors.
- **Model:** Represents application data, including income, expenses, invoices, recurring items, cryptocurrency trades, and savings goals.
- **Financial Calculation Classes:** Pure Kotlin classes that calculate financial totals, spending summaries, savings progress, and realized cryptocurrency profits or losses.
- **Room and SQLite:** Provide local persistence for cached and offline data.
- **Firebase Authentication:** Handles user authentication and session management.
- **Cloud Firestore:** Provides cloud persistence and real-time data updates.

### Data Flow

**User Actions:**

`View` → `ViewModel` → `Repository` → `Data Source`

**UI Updates:**

`Data Source` → `Repository` → `ViewModel` → `View`

### Offline Data and Synchronization

<img width="1194" height="238" alt="image" src="https://github.com/user-attachments/assets/5b05f3aa-b42c-46db-9f6d-699327292968" />

The application supports creating, editing, and viewing records without an active network connection. Local persistence allows users to access cached information and save changes while offline.

When connectivity returns, pending changes are synchronized with Cloud Firestore. The interface displays a pending-synchronization indicator until the server confirms the changes.

Concurrent edits to the same record are resolved using a last-write-wins policy based on server timestamps.

### Architecture Principles

- Separate UI, ViewModel, Repository, and Model layers.
- Keep business logic out of Jetpack Compose UI components.
- Prevent direct database and Firebase calls from the UI layer.
- Use StateFlow or LiveData to expose observable UI state.
- Use Kotlin Coroutines and `viewModelScope` for asynchronous operations.
- Implement financial calculations in pure Kotlin classes and cover them with unit tests.
- Scope stored records to the authenticated user's ID.
- Clear the local cache when a user signs out.
- Handle loading, empty, and error states without displaying blank screens or allowing unhandled exceptions.

## Firebase Configuration

> **Status:** Firebase configuration is not yet completed.

### Prerequisites

- A Google account.
- Access to the Firebase Console.
- An Android development environment with Android Studio.

### Setup Instructions

1. Create a Firebase project in the Firebase Console.
2. Register the Android application using its package name.
3. Download the `google-services.json` configuration file.
4. Place the downloaded file in the Android application's `app/` directory.
5. Configure the Google Services Gradle plugin and required Firebase dependencies.
6. Enable Email/Password authentication in Firebase Authentication.
7. Configure Cloud Firestore and its security rules.
8. Ensure Firestore access is restricted to the authenticated owner of each user's data.
9. Build and run the application to verify the Firebase integration.

### Firebase Configuration Details

**Firebase Project ID:** 

**Android Package Name:** 

**Authentication Providers:** 

**Cloud Firestore Configuration:** 

**Firestore Security Rules:** 

**Additional Configuration:** 

## Build Instructions

### Prerequisites

Ensure the following are installed:

- Android Studio
- Android SDK
- JDK compatible with the project's Gradle configuration
- Git

### 1. Clone the Repository

```bash
git clone https://github.com/sakith03/vero-mobile.git
```

Navigate to the project directory:

```bash
cd <your_project_directory>
```

### 2. Configure Firebase

Complete the Firebase configuration steps described above.

Ensure that `google-services.json` is placed in the appropriate application module directory if the project is configured to use the Google Services Gradle plugin.

### 3. Open the Project

1. Open Android Studio.
2. Select **Open**.
3. Choose the cloned project directory.
4. Allow Gradle to synchronize the project and download its dependencies.

### 4. Build the Application

On Windows, run:

```powershell
.\gradlew.bat build
```

On macOS or Linux, run:

```bash
./gradlew build
```

### 5. Run the Application

1. Start an Android emulator running Android 8.0 (API 26) or later, or connect a compatible Android device.
2. Select the application configuration in Android Studio.
3. Click **Run** to build and launch the application.

### 6. Run Unit Tests

On Windows:

```powershell
.\gradlew.bat test
```

On macOS or Linux:

```bash
./gradlew test
```

> **Note:** Build and test commands may need to be adjusted according to the project's actual Gradle configuration and module structure.


## Platform Requirements

- **Platform:** Android
- **Minimum Android Version:** Android 8.0 (API 26)
- **Target SDK:** API 34 or higher
- **Language:** Kotlin
- **UI:** Jetpack Compose with Material Design 3
- **Backend Services:** Firebase Authentication and Cloud Firestore
