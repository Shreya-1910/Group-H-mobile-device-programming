# s-CPD App: Data Model (Firestore)

All times are epoch milliseconds (`Long`). Kotlin models live in `app/src/main/java/com/mdpgroup08/scpd/data/model/`.
Every model has default values so Firestore can create it with `toObject()`.

## Collections

| Collection | Document ID | Kotlin model | Owner     |
|---|---|---|-----------|
| `users` | Firebase Auth uid | `User` | Mark      |
| `events` | auto ID | `Event` | Sanjanaah |
| `bookings` | `{eventId}_{studentId}` | `Booking` | Sanjanaah |
| `waitlist` | `{eventId}_{studentId}` | `WaitlistEntry` | Sanjanaah |
| `appeals` | auto ID | `Appeal` | Member 3  |
| `auditLog` | auto ID | `AuditEntry` | Mark      |

Collections for check-ins, session logs and points entries will be added by Members 2 and 3.

## events

| Field | Type | Notes |
|---|---|---|
| `title`, `description`, `category`, `location` | String | `category` is used by filters (R2-S2) and points by category (R5-S10) |
| `clubId`, `clubName` | String | `clubName` is stored so Marketplace cards need no extra lookup (R2-S1) |
| `venueLat`, `venueLng` | Double? | Optional, for the location check (R3-S12) |
| `startTime`, `endTime` | Long | Used for clashes (R2-S7), cut-off (R2-S4) and check-in window (R3-S8) |
| `requestedPoints` | Int | 1–10, set by the club (R1-S1) |
| `approvedPoints` | Int? | Set by the admin on approval, ≤ `requestedPoints` (R1-S4). Points shown to students and credited |
| `capacity`, `bookedCount` | Int | `bookedCount` changes only inside a booking transaction (R2-S4) |
| `status` | EventStatus | `DRAFT`, `PENDING`, `APPROVED`, `REJECTED`, `CANCELLED`, `EXPIRED` |
| `rejectionReason`, `cancellationReason` | String? | Required when rejected (R1-S5) or cancelled (R1-S9) |
| `createdAt`, `submittedAt` | Long, Long? | Review is due 5 working days after `submittedAt` |

Computed in the app, not stored: `seatsLeft`, `isFull`.

### Event lifecycle

DRAFT → PENDING (club submits) → APPROVED or REJECTED (admin, in the backend).
APPROVED → CANCELLED (club cancels). PENDING → EXPIRED (not reviewed by the start time).
Clubs cannot edit after DRAFT; security rules enforce this (R1-S7).

## bookings

| Field | Type | Notes |
|---|---|---|
| `eventId`, `studentId` | String | The document ID `{eventId}_{studentId}` allows only one booking per student per event (R2-S4) |
| `status` | BookingStatus | `CONFIRMED`, `CANCELLED`, `NO_SHOW` (only after the 72 h sync deadline, R4-S26) |
| `createdAt`, `cancelledAt` | Long, Long? | |

## waitlist

| Field | Type | Notes |
|---|---|---|
| `eventId`, `studentId` | String | Same ID pattern as bookings |
| `joinedAt` | Long | Position is the order of `joinedAt` (R2-S5) |

## users

| Field | Type | Notes |
|---|---|---|
| `name`, `email` | String | |
| `role` | String | `STUDENT`, `CLUB` or `ADMIN` (see `UserRole`). Set only by the backend (R6-S9, R6-S10) |
| `clubId` | String? | Set only for club accounts, linked to exactly one club |

## Rules for changing this file

Any change to a shared model must be agreed by the group first and updated here in the same PR.