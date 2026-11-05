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

<img width="847" height="949" alt="Screenshot 2026-10-21 000358" src="https://github.com/user-attachments/assets/22da1a16-50b4-4393-b62c-ca10155640c5" />

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

> **Status:** Firebase is configured for the Vero Mobile Android project. Email/Password Authentication and the user profile document in Cloud Firestore have been tested. Individual feature synchronization should be verified separately before treating it as fully tested.

### Prerequisites

- A Google account with access to the **Vero-Finance-App** Firebase project.
- Android Studio, Android SDK, and the JDK required by the project's Gradle configuration.
- Internet access for initial account registration, sign-in, and Firebase configuration.

### Setup Instructions

1. Open the [Firebase Console](https://console.firebase.google.com/) and select **Vero-Finance-App**.
2. Confirm that the Android application is registered with the package name `com.example.veromobile`.
3. Place the Firebase project's Android configuration file at `app/google-services.json`.
4. Sync the project with Gradle. The Google Services plugin and Firebase Authentication/Firestore dependencies are already declared in the project configuration.
5. In **Authentication → Sign-in method**, confirm that **Email/Password** is enabled.
6. In **Cloud Firestore**, use the `(default)` database and review the active security rules under **Rules**.
7. Test account registration and sign-in, then confirm that the authenticated user's profile appears under `users/{uid}`.
8. Test saving and loading income, expenses, and other records separately to verify the full Firestore/Room synchronization flow.

### Firebase Configuration Details

| Setting | Configuration |
| --- | --- |
| Firebase project name | **Vero-Finance-App** |
| Firebase project ID | `vero-finance-app` |
| Android package name | `com.example.veromobile` |
| Firebase Android configuration | `app/google-services.json` |
| Authentication SDK | Firebase Authentication |
| Enabled authentication provider | **Email/Password** (confirmed) |
| Google Sign-In | Listed in the SRS; do not assume it is enabled or tested |
| Cloud database | **Cloud Firestore** — `(default)` database |
| Firestore data layout | User-scoped documents under `users/{uid}` |
| Firestore rules file | `firestore.rules` at the repository root |
| Rules version | `rules_version = '2'` |
| Local database | Room (SQLite) for offline persistence |
| Build configuration | Google Services Gradle plugin, Firebase BoM, Firebase Authentication and Firestore dependencies (Kotlin DSL) |

### Firebase Authentication

The app uses Firebase Authentication for account registration and sign-in. Each account is identified by its Firebase user ID (`uid`), which is used to scope its financial records. Password-reset, session persistence, and sign-out behavior should be checked using the final APK. Firebase Authentication is not replaced by local client-side checks when offline.

### Cloud Firestore Data Structure

The project defines the following user-scoped collection paths. Firestore collections appear in the console when documents have been written.

| Collection | Document path | Purpose |
| --- | --- | --- |
| User profile | `users/{uid}` | User account details |
| Incomes | `users/{uid}/incomes/{id}` | Salary, freelance, AdSense, and other income |
| Expenses | `users/{uid}/expenses/{id}` | Categorized expense transactions |
| Categories | `users/{uid}/categories/{id}` | User-defined spending categories |
| Invoices | `users/{uid}/invoices/{id}` | Freelance invoice records |
| Crypto trades | `users/{uid}/cryptoTrades/{id}` | Realized cryptocurrency profit/loss records |
| Recurring rules | `users/{uid}/recurringRules/{id}` | Recurring bills and subscriptions |
| Savings goals | `users/{uid}/savingsGoals/{id}` | Financial savings targets |
| Goal contributions | `users/{uid}/goalContributions/{id}` | Contributions linked to savings goals |

Financial amounts are represented using integer minor units to avoid floating-point rounding issues. The schema includes ownership fields and supports dates, category references, update times, and deletion state where defined by the corresponding models.

### Firestore Security Rules

The repository includes `firestore.rules`, using Firestore Security Rules version 2. Rules require an authenticated user and verify ownership of `users/{userId}` documents and their supported subcollections using the Firebase UID. The repository rules also define field and data-type validation for supported financial document types.

**Important:** Updating the local `firestore.rules` file does not automatically publish it to Firebase. The rules deployed in the Firebase Console must be reviewed and published separately. Android-side validation is not a substitute for server-side Firestore Security Rules.

### Offline Storage and Synchronization

Room stores local financial data, while Firestore provides the cloud data source. The app is designed to preserve cached records and pending local changes when the device is offline, then synchronize eligible changes on reconnection. A previously authenticated session may access its own cached data offline; a new account cannot be authenticated for the first time without contacting Firebase.

To verify end-to-end synchronization, test record creation, editing, deletion, offline changes, reconnection, and cross-user isolation. A local “saved” confirmation does not by itself prove that the cloud write succeeded.

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
