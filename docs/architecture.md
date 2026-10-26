# s-CPD App: Architecture

## 1. Architecture Overview

The s-CPD app uses **MVVM (Model-View-ViewModel) with Clean Architecture**. The architecture separates the application into three layers: Presentation, Domain and Data. This separation improves maintainability, testability and separation of concerns.

The general architecture of the application is illustrated in Figure 1. The Presentation layer handles user interactions and UI state, the Domain layer contains use cases and repository interfaces, and the Data layer provides repository implementations that interact with local and remote data sources.

![General architecture of the s-CPD application showing the Presentation, Domain and Data layers](scpd_general_architecture.png)

**Figure 1:** General architecture of the s-CPD application.

The check-in feature is a core feature of the application and provides a more detailed example of how the architectural layers interact.

![Check-in feature architecture showing the interaction between the presentation, domain and data layers](scpd_architecture_diagram.png)

**Figure 2:** Detailed architecture of the check-in feature.

## 2. How a Check-In Moves Through the Layers

The check-in process follows these steps:

1. **User interaction:** The student scans the club's QR code or manually enters the six-character check-in code. `CheckInScreen` passes the action to `CheckInViewModel`.

2. **ViewModel processing:** `CheckInViewModel` invokes `CheckInUseCase` using a suspend function so that the screen does not freeze while the operation is running.

3. **Business rules:** `CheckInUseCase` validates the check-in request. The code must belong to the correct event and fall within its valid time window. The attendance record is also signed using the phone's key. The use case then saves the record through the `AttendanceRepository` interface.

4. **Data persistence and synchronisation:** `AttendanceRepositoryImpl` saves the attendance record locally in Room first, allowing the application to work without an internet connection. When connectivity returns, the record is uploaded to Firebase.

5. **UI state update:** `CheckInViewModel` exposes a `CheckInUiState` through a `StateFlow`. The interface can display a status such as "Saved on this phone, waiting to sync". `CheckInScreen` updates automatically when the state changes.

This flow separates user interaction, business logic and data operations, making the check-in process easier to maintain and test.

## 3. Architecture of Other Features

Each feature has its own View, ViewModel, use cases and repository, following the same architectural pattern.

| Feature | View | ViewModel | Use Cases | Repository |
|---|---|---|---|---|
| Log in (student or club) | `LoginScreen` | `LoginViewModel` | `LoginUseCase` | `AuthRepository` |
| Browse and book events | `MarketplaceScreen`, `EventDetailsScreen` | `MarketplaceViewModel` | `GetEvents`, `BookEvent`, `CancelBooking` | `EventRepository`, `BookingRepository` |
| Create and submit an event (club) | `CreateEventScreen` | `CreateEventViewModel` | `SaveDraft`, `SubmitEvent` | `EventRepository` |
| Show check-in code (club) | `StartCheckInScreen` | `StartCheckInViewModel` | `GenerateCheckInCode` | `EventRepository` |
| Check in (student) | `CheckInScreen` | `CheckInViewModel` | `CheckInUseCase` | `AttendanceRepository` |
| See sync status | `MyAttendanceScreen` | `MyAttendanceViewModel` | `SyncAttendance`, `AppealRecord` | `AttendanceRepository` |
| Live attendance and review (club) | `AttendanceListScreen` | `AttendanceViewModel` | `MarkPresent`, `ReviewRecord` | `AttendanceRepository` |
| Points and history | `PointsScreen` | `PointsViewModel` | `GetPoints` | `PointsRepository` |

**Table 1:** Architecture components for each application feature.

## 4. Why This Architecture Suits Our App

### 4.1 Single Source of Truth

Attendance records are stored in a Room table managed by `AttendanceRepository`. Both `MyAttendanceScreen` and the "waiting to sync" banner observe the same data. Consequently, changes to attendance status can be reflected consistently across the interface.

### 4.2 State Is Not an Event

The "waiting to sync" message is represented as state in `CheckInUiState`. Since the interface observes this state, it can be redrawn after a screen rotation.

Navigation to a result screen, on the other hand, occurs as a one-time event. Separating persistent UI state from one-time events helps prevent repeated navigation when the interface is recreated.

### 4.3 Dependency Inversion

`CheckInUseCase` depends on the `AttendanceRepository` interface rather than directly depending on Firebase or another concrete data source.

This allows the use case to be tested using a fake repository without requiring a Firebase connection.

### 4.4 Testability

`BookEvent` and `CheckInUseCase` are implemented as plain Kotlin use cases and can be tested using JVM unit tests.

This is useful because the emulator cannot scan a physical QR code. The business logic can therefore be tested independently of the device's camera.

### 4.5 Alternatives Considered

**Model-View-Controller (MVC)** was rejected because a single `CheckInActivity` could end up handling QR scanning, check-in rules and uploading attendance records. This concentrates responsibilities in one component and makes state management during screen rotation more difficult.

**Model-View-Presenter (MVP)** was rejected because the Presenter could retain a reference to an Activity after its lifecycle ends, potentially causing memory leaks if lifecycle management is not handled correctly.

MVVM with Clean Architecture was selected to provide clearer separation of responsibilities, easier testing and more maintainable feature development.

## 5. Summary

The s-CPD application uses MVVM with Clean Architecture to separate the user interface, business rules and data operations. The general architecture illustrates how the application's features are organised across the Presentation, Domain and Data layers, while the check-in architecture demonstrates how these layers interact in a core workflow.

Local persistence using Room supports offline check-ins, while synchronisation with Firebase allows attendance records to be uploaded when connectivity returns. The architecture also supports independent testing of business logic and consistent UI state management.
