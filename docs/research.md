# Research: Requirements Analysis

This file holds the user stories for all three actors (Student, Club Organiser, s-CPD Administrator), covering R1–R6.

**Priority:** **Must** = must-have, built in Part 2. **Nice** = nice-to-have, built if time allows.
**Tags:** ⚠ exceptional case · 🛡 cheating attempt

**Assumptions**
- The administrator works through backend tools (R6-S5).
- Points per event: 1–10.
- Check-in window: from 15 min before the start to 30 min after.
- Booking cut-off: 2 h before the start.

## R1: Event Approval

**Design decisions:**
- Event lifecycle: **Draft → Pending → Approved / Rejected**. An approved event can be **Cancelled**, and a pending one **Expires** if not reviewed in time.
- Clubs **cannot edit** after submission. The backend enforces this, so booked students can rely on what they saw. To make a change, the club cancels and resubmits.

### R1-S1 · Create an event draft · Must
**User Story:** As a club organiser, I want to create an event so that I can request s-CPD points.
**Acceptance Criteria:**
- Given valid details (title, venue, times, capacity ≥ 1, points 1–10), the event saves as **Draft**.
- Invalid fields are highlighted, and the draft can't be saved until they're fixed.

### R1-S2 · Submit for approval · Must
**User Story:** As a club organiser, I want to submit a draft so that the s-CPD office can review it.
**Acceptance Criteria:**
- The status becomes **Pending**, all fields lock, and the event enters the review queue.
- Submitting requires a connection.

### R1-S3 · Track approval status · Must
**User Story:** As a club organiser, I want to track my events' status so that I can plan publicity.
**Acceptance Criteria:**
- Each event shows its status, and a push notification is sent when a decision is made.
- A rejected event shows the administrator's reason.

### R1-S4 · Approve an event · Must
**User Story:** As an administrator, I want to approve an event and confirm its points so that it is published.
**Acceptance Criteria:**
- The event appears in the Marketplace, and its check-in secret is generated (R3-S1).
- Approval requests from non-administrator accounts are denied.

### R1-S5 · Reject an event · Must
**User Story:** As an administrator, I want to reject an event with a reason so that the club can correct it.
**Acceptance Criteria:**
- A reason is required.
- The rejected event can never be listed or booked.

### R1-S6 · ⚠ Editing locked after submission · Must
**User Story:** As a club organiser, I want to know a submitted event is locked so that I know how to handle changes.
**Acceptance Criteria:**
- Pending and Approved events show no Edit button.
- A notice explains how to cancel and resubmit.

### R1-S7 · 🛡 Bypassing approval · Must
**User Story:** As the system, I want changes outside the workflow rejected so that clubs can't self-approve or inflate points.
**Acceptance Criteria:**
- Security rules deny club writes to status, points, time or capacity after submission, including writes from a modified app or direct API call.
- Automated rule tests verify this.

### R1-S8 · ⚠ Pending event never reviewed · Must
**User Story:** As a club organiser, I want to know if my event wasn't reviewed in time so that I don't advertise it.
**Acceptance Criteria:**
- An event still Pending at its start time becomes **Expired**, and the club is notified.

### R1-S9 · Cancel an approved event · Nice
**User Story:** As a club organiser, I want to cancel an event so that students aren't left waiting.
**Acceptance Criteria:**
- A reason is required.
- The event leaves the Marketplace and booked students are notified.
- No points are awarded (R5-S12).

### R1-S10 · ⚠ Drafts kept offline · Nice
**User Story:** As a club organiser, I want drafts saved offline so that I don't lose work.
**Acceptance Criteria:**
- The draft is stored locally, marked *"Not uploaded"*, and uploads when online.

### R1-S11 · Reuse a rejected event · Nice
**User Story:** As a club organiser, I want to copy a rejected event so that I can fix it without retyping.
**Acceptance Criteria:**
- A new Draft is created with the same details, and the original is kept.

---

## R2: Marketplace and Booking

**Design decisions:**
- **Finding events:** search, filters and a "For you" row.
- **Full events:** students can join a waitlist that moves automatically.
- **Clashes:** blocked, with an option to swap one booking for the other.
- **Fairness:** bookings run as server transactions, so capacity is never exceeded.

### R2-S1 · Browse events · Must
**User Story:** As a student, I want to browse upcoming events so that I can find ways to earn points.
**Acceptance Criteria:**
- Only Approved, not-yet-started events are shown, soonest first.
- Each card shows the club, time, venue, points and places left.
- The list has loading, empty and error-with-retry states.

### R2-S2 · Search and filter · Must
**User Story:** As a student, I want to search and filter so that I find relevant events quickly.
**Acceptance Criteria:**
- Keyword search can be combined with filters for category, date, points, club and availability.
- If no results match, the app suggests clearing filters.

### R2-S3 · View event details · Must
**User Story:** As a student, I want full event details so that I can decide whether to attend.
**Acceptance Criteria:**
- The main button reflects my state: **Book**, **Booked ✓**, **Join waitlist**, **Clashes with [event]** or **Booking closed**.

### R2-S4 · Book a place · Must
**User Story:** As a student, I want to book in one tap so that my place is guaranteed.
**Acceptance Criteria:**
- A server transaction reserves exactly one place, and a confirmation is shown.
- The booking is cached for offline use (R4-S9).
- Each student can make only one booking per event.
- Booking closes 2 h before the start.

### R2-S5 · ⚠ Event full · Must
**User Story:** As a student, I want a waitlist for full events so that I can still attend if someone cancels.
**Acceptance Criteria:**
- My waitlist position is shown.
- When someone cancels, the first student in line is booked and notified.
- The waitlist closes at the cut-off.

### R2-S6 · ⚠ Last place taken simultaneously · Must
**User Story:** As a student, I want a fair result if the last place goes while I book so that I'm never wrongly told I have a place.
**Acceptance Criteria:**
- Exactly one booking succeeds.
- The other student sees *"Just filled up"* and is offered the waitlist.

### R2-S7 · ⚠ Booking clash · Must
**User Story:** As a student, I want overlapping bookings flagged so that I don't double-book.
**Acceptance Criteria:**
- I can keep my existing booking or swap it for the new event in one step.
- The server also rejects overlapping bookings.

### R2-S8 · Cancel a booking · Must
**User Story:** As a student, I want to cancel a booking so that my place goes to someone else.
**Acceptance Criteria:**
- Cancelling is allowed until the cut-off, and the waitlist then advances.
- After the cut-off, Cancel is disabled with an explanation.

### R2-S9 · ⚠ Browsing offline · Must
**User Story:** As a student without a connection, I want to see saved events so that I can still browse.
**Acceptance Criteria:**
- The cached list is shown with *"Offline: showing saved data"*.
- Booking is disabled while offline.

### R2-S10 · 🛡 Invalid booking via modified app · Must
**User Story:** As the system, I want invalid bookings rejected server-side so that the booking rules can't be bypassed.
**Acceptance Criteria:**
- The server rejects bookings for events that are unapproved, full, past the cut-off or clashing.
- Students can only book for their own account.
- Rule tests cover each case.

### R2-S11 · Recommendations · Nice
**User Story:** As a student, I want suggested events so that I discover relevant activities.
**Acceptance Criteria:**
- A "For you" row ranks events by my interests, my past attendance and categories where I have fewer points.

### R2-S12 · 🛡 Repeated no-shows · Nice
**User Story:** As a club organiser, I want repeated no-shows limited so that places aren't wasted.
**Acceptance Criteria:**
- After 3 no-shows in a semester, a student can hold at most 2 upcoming bookings, with an explanation.

### R2-S13 · View booking numbers · Nice
**User Story:** As a club organiser, I want to see booking and waitlist counts so that I can prepare the venue.
**Acceptance Criteria:**
- These are visible for my own club's events only (R6-S4).

---

## R3: Secure Attendance

**Design decisions:**
- **Check-in method:** the club's device shows a **QR code that rotates every 15 s**, with a **6-character fallback code**.
    - Each code is signed with a per-event secret that students never receive.
- **Server checks:**
    - the code's signature and window counter (R4-S7);
    - the student's booking;
    - the registered device (R6-S8);
    - duplicates (R4-S5).
- **Offline:** check-ins are saved on the phone and sync later (R4-S1).
- **Remaining weakness:** the design blocks remote, late and forged check-ins. It can't prove *who* holds the phone in the room, so proxy check-ins are made harder and auditable rather than impossible.

### R3-S1 · Start check-in · Must
**User Story:** As a club organiser, I want to display a check-in code so that students check in on arrival.
**Acceptance Criteria:**
- During the check-in window, a full-screen rotating QR code is shown with the fallback code and a countdown.
- Outside the window, Start check-in is disabled.
- Offline operation is covered by R4-S4.

### R3-S2 · Scan to check in · Must
**User Story:** As a student, I want to check in within seconds so that I don't hold up the queue.
**Acceptance Criteria:**
- A booked event shows a **Check in** button during the window.
- The result appears within 2 s: **Verified**, or *"Saved, waiting to sync"* when offline.

### R3-S3 · ⚠ Typing the code · Must
**User Story:** As a student whose camera can't scan, I want to type the code so that I can still check in.
**Acceptance Criteria:**
- A typed code is validated like a scan.
- This is also how check-in is tested on the emulator.

### R3-S4 · ⚠ Clear failure reasons · Must
**User Story:** As a student, I want to know why check-in failed so that I can fix it.
**Acceptance Criteria:**
- The app shows a specific message: *not open*, *closed*, *code expired*, *wrong event*, *already checked in*, or *no booking – sent for review*.
- Each message suggests a next step.

### R3-S5 · Live attendance list · Must
**User Story:** As a club organiser, I want live attendance so that I can spot problems during the event.
**Acceptance Criteria:**
- A count is shown (e.g. **42 / 60**).
- Each attendee shows one status: Checked in, Not yet, Manual or Needs review.

### R3-S6 · Mark present manually · Must
**User Story:** As a club organiser, I want to mark a student present when their phone fails so that genuine attendees aren't penalised.
**Acceptance Criteria:**
- This works only for booked attendees and only within the window.
- A reason is required, and my identity is logged as **Manual**.

### R3-S7 · 🛡 Screenshot sent to an absent friend · Must
**User Story:** As the system, I want copied codes to expire quickly so that nobody checks in from home.
**Acceptance Criteria:**
- Only the current or previous 15-second code is accepted, so a screenshot expires within about 30 s.
- **Weakness:** a code relayed live within 30 s (mitigated by R3-S12 and the club's headcount).

### R3-S8 · 🛡 Checking in outside the event time · Must
**User Story:** As the system, I want check-ins outside the window rejected so that attendance can't be claimed afterwards.
**Acceptance Criteria:**
- The signed window counter is used, not the phone clock, so changing the device time has no effect.
- Offline records must also sync within 72 h (R4-S8).

### R3-S9 · 🛡 Forged code · Must
**User Story:** As the system, I want fake codes rejected so that check-ins can't be generated remotely.
**Acceptance Criteria:**
- Codes with an invalid signature are rejected and logged.
- The secret is readable only by the owning club (R6-S4) and expires after the window.

### R3-S10 · 🛡 Checking in for a friend · Must
**User Story:** As the system, I want each check-in tied to one student and one registered device so that nobody checks in others.
**Acceptance Criteria:**
- Each device can make one check-in per event.
- Records must be signed by the student's registered key (R6-S8).
- Fingerprint or PIN confirmation is used when enabled (R4-S12).
- **Weakness:** someone carrying a friend's own unlocked phone.

### R3-S11 · 🛡 Check-in without a booking · Must
**User Story:** As the system, I want unbooked check-ins held for review so that walk-ins can't claim points automatically.
**Acceptance Criteria:**
- The record is marked **Needs review** (R4-S10).
- No points are given until it's approved (R5-S4).

### R3-S12 · 🛡 Faked location · Nice
**User Story:** As the system, I want suspicious locations flagged so that spoofing is visible.
**Acceptance Criteria:**
- A mock location, or a position more than 200 m from the venue, marks the record **Needs review**.
- A missing location never blocks check-in, since indoor GPS is unreliable.

### R3-S13 · 🛡 Club marking absent friends present · Nice
**User Story:** As an administrator, I want manual check-ins audited so that clubs can't credit absent friends.
**Acceptance Criteria:**
- Events where more than 20% of attendance is Manual are flagged for review.

### R3-S14 · Check-out · Nice
**User Story:** As a club organiser, I want attendees to also check out so that points go only to those who stayed.
**Acceptance Criteria:**
- When this is enabled, attendance is Verified only if both scans are valid.
