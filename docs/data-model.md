# s-CPD App: Data Model (Firestore)

All times are epoch milliseconds (`Long`). Kotlin models live in `app/src/main/java/com/mdpgroup08/scpd/data/model/`.
Every model has default values so Firestore can create it with `toObject()`.

## Collections

| Collection | Document ID | Kotlin model | Owner       |
|---|---|---|-------------|
| `users` | Firebase Auth uid | `User` | Mark        |
| `events` | auto ID | `Event` | Sanjanaah   |
| `bookings` | `{eventId}_{studentId}` | `Booking` | Sanjanaah   |
| `appeals` | auto ID | `Appeal` | Shreyashree |
| `auditLog` | auto ID, or `event_{eventId}_decision` for approvals | `AuditEntry` | Mark        |

Collections for check-ins, session logs and points entries will be added by Members 2 and 3.

The waitlist is not a separate collection. It is stored on each event (`events.waitlist`), so a
cancellation can promote the next student in the same transaction (see below).

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
| `waitlist` | List<String> | Student IDs waiting for a place, first in line first (R2-S5). Changed only in booking transactions |
| `status` | EventStatus | `DRAFT`, `PENDING`, `APPROVED`, `REJECTED`, `CANCELLED`, `EXPIRED` |
| `rejectionReason`, `cancellationReason` | String? | Required when rejected (R1-S5) or cancelled (R1-S9) |
| `createdAt`, `submittedAt` | Long, Long? | Review is due 5 working days after `submittedAt` |

Computed in the app, not stored: `seatsLeft`, `isFull`.

### Event lifecycle

DRAFT → PENDING (club submits) → APPROVED or REJECTED (admin, in the admin screens).
APPROVED → CANCELLED (club cancels). PENDING → EXPIRED (not reviewed by the start time).
Clubs cannot edit after DRAFT; security rules enforce this (R1-S7).

## bookings

| Field | Type | Notes |
|---|---|---|
| `eventId`, `studentId` | String | The document ID `{eventId}_{studentId}` allows only one booking per student per event (R2-S4) |
| `status` | BookingStatus | `CONFIRMED`, `CANCELLED`, `NO_SHOW` (only after the 72 h sync deadline, R4-S26) |
| `createdAt`, `cancelledAt` | Long, Long? | |
| `fromWaitlist` | Boolean | True when the place came from the waitlist, so the student can be notified (R5-S17) |

A cancelled booking can be booked again; only a `CONFIRMED` booking blocks a new one.

### How bookings change (all in Firestore transactions)

| Action | What changes |
|---|---|
| Book (R2-S4) | New `CONFIRMED` booking, `bookedCount` + 1, student removed from this event's waitlist and from waitlists of overlapping events (R2-S5 EX1) |
| Join waitlist (R2-S5) | Student ID added to the end of `events.waitlist` (only when the event is full) |
| Cancel (R2-S8) | Booking becomes `CANCELLED`. If someone is waiting, `waitlist[0]` gets a `CONFIRMED` booking with `fromWaitlist = true` and leaves the queue, and `bookedCount` stays the same; otherwise `bookedCount` − 1 |
| Swap (R2-S7) | Cancel the old booking (with waitlist promotion, as above) and book the new event, together |

Booking, cancelling and the waitlist all close 2 hours before the event starts.

## users

| Field | Type | Notes |
|---|---|---|
| `name`, `email` | String | |
| `role` | String | `STUDENT`, `CLUB` or `ADMIN` (see `UserRole`). Set only by the backend (R6-S9, R6-S10) |
| `clubId`, `clubName` | String? | Set only for club accounts, linked to exactly one club |

Member 4's `User` model also stores account status, the device key and study details (R6). See `User.kt`.

## Rules for changing this file

Any change to a shared model must be