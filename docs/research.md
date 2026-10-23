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

# R4: Online and Offline

## R4 S1
**Priority:** Must have

**User story:** As a student, I want to check in at an event even when I have no signal, so that I still get credit for attending.

**Acceptance criteria:**
1. Given I have no internet connection and I am booked on the event
   When I scan the valid event code during the event
   Then my check-in is saved on my phone within 1 second and I see "Saved on this phone, waiting to sync" with the time I scanned.

2. Given I have no internet connection
   When I scan a valid code
   Then the app does not show any network error or ask me to try again later.

## R4 S2
**Priority:** Must have

**User story:** As a student, I want my saved check-ins to upload automatically when I get a connection, so that I do not have to remember to sync them myself.

**Acceptance criteria:**
1. Given I have check-ins waiting to sync
   When my phone gets a connection again
   Then the upload starts within 60 seconds without me opening any screen.

2. Given I closed the app or restarted my phone before syncing
   When I get a connection
   Then my waiting check-ins are still there and are uploaded.

3. Given a check-in was uploaded successfully
   When the app retries or syncs again
   Then it is not uploaded a second time.

## R4 S3
**Priority:** Must have

**User story:** As a student, I want to see which of my check-ins have synced, so that I know my attendance will count.

**Acceptance criteria:**
1. Given I have check-ins in different states
   When I open "My attendance"
   Then each one shows a clear status: Pending, Syncing, Verified, Rejected or Needs review.

2. Given at least one check-in is waiting
   When I use the app
   Then a banner shows how many are waiting, for example "2 check-ins waiting to sync".

3. Given I am online with waiting check-ins
   When I tap "Sync now"
   Then the upload starts immediately and the "Last synced" time updates.

## R4 S4
**Priority:** Must have

**User story:** As a student, I want to see a clear message when a check-in cannot be synced, so that I know what to do and do not wait forever.

**Acceptance criteria:**
1. Given the upload fails 5 times in a row
   When I open "My attendance"
   Then the check-in shows "Couldn't sync", the reason, and a "Retry" button instead of staying on "Syncing".

2. Given a check-in is Rejected
   When I tap it
   Then I see the reason in plain language, for example "Code expired" or "Synced too late", and an "Appeal" button.

## R4 S5
**Priority:** Must have

**User story:** As a student, I want a check-in I made during the event to count even if my phone only synced later, so that a weak signal does not cost me my points.

**Acceptance criteria:**
1. Given I scanned during the event and I am offline
   When my phone syncs within 72 hours after the event ends
   Then the check-in is judged on the time I scanned, not the time it uploaded, and can become Verified.

2. Given I scanned during the event
   When my phone syncs more than 72 hours after the event ends
   Then the check-in shows "Rejected: synced too late" with an option to appeal.

## R4 S6
**Priority:** Must have

**User story:** As a student, I want to be told right away when I scan a wrong or expired code, so that I can scan again while I am still at the event.

**Acceptance criteria:**
1. Given I scan a code that belongs to another event
   When the app reads it
   Then I see "This code is not valid for this event", nothing is saved, and I can scan again.

2. Given I scan a code older than its 30-second window
   When the app reads it
   Then I see "Code expired, please scan the current code" and nothing is saved.

3. Given my camera does not work or permission is denied
   When I tap "Enter code manually"
   Then I can type the 6-digit code shown by the club and check in the same way.

## R4 S7
**Priority:** Must have

**User story:** As a student who could not book because the event was full, I want my attendance to be recorded for the club to review, so that I can still be considered for points if the club lets me in.

**Acceptance criteria:**
1. Given the event is full and I have no booking
   When I scan the code at the event
   Then my check-in is saved with the status "Needs review: no booking" and I am told the club decides whether points are given.

2. Given I have no booking
   When the club approves my record
   Then my status changes to Verified and the normal points rules apply.

3. Given I have no booking
   When the club rejects my record
   Then I see "Rejected by club" and can still read the reason.

## R4 S8
**Priority:** Must have

**User story:** As a student, I want a warning before I log out with unsynced check-ins, so that I do not lose my attendance by accident.

**Acceptance criteria:**
1. Given I have check-ins waiting to sync
   When I tap "Log out"
   Then I see "You have 2 check-ins not synced yet" with the options "Sync now" and "Cancel".

2. Given I am offline with waiting check-ins when I try to log out
   Then I am told to reconnect first and my check-ins are not deleted.

## R4 S9
**Priority:** Must have

**User story:** As a student, I want a check-in to be either safely saved or clearly not saved if my app closes or my battery dies while scanning, so that I never think I checked in when I did not.

**Acceptance criteria:**
1. Given I have just scanned
   When the app closes at any point
   Then on reopening I see either the saved check-in in "My attendance" or no record and a prompt to scan again.

## R4 S10
**Priority:** Nice to have

**User story:** As a student, I want to see my booked events and ticket details offline, so that I can find the venue and show my booking without signal.

**Acceptance criteria:**
1. Given I have booked events and I have opened the app online before
   When I open "My events" without a connection
   Then my bookings, venue and times are shown with the notice "Offline: showing saved data".

## R4 S11
**Priority:** Nice to have

**User story:** As a student, I want to stay able to check in if my login expires while I am offline, so that a timeout does not stop me at the event.

**Acceptance criteria:**
1. Given my login session expired while I am offline
   When I scan a valid code
   Then the check-in is saved on my phone as normal.

2. Given I have saved check-ins and an expired session
   When I get a connection
   Then I am asked to log in again and my check-ins upload after I do.

## R4 S12
**Priority:** Nice to have

**User story:** As a student, I want to confirm my check-in with my fingerprint or PIN, so that nobody else can check in using my unlocked phone.

**Acceptance criteria:**
1. Given biometric confirmation is turned on
   When I scan a valid code
   Then the phone asks for my fingerprint or PIN before the check-in is saved.

2. Given the biometric prompt appears
   When I cancel it
   Then nothing is saved and I can scan again.

## R4 S13
**Priority:** Nice to have

**User story:** As a student who has got a new phone, I want to move my account to it, so that I can keep checking in without losing earlier records.

**Acceptance criteria:**
1. Given I log in on a new phone
   When I confirm the change on the new phone
   Then the new phone is registered and the old phone can no longer check in.

2. Given check-ins are still waiting on the old phone
   When the change is made
   Then I am warned that unsynced check-ins on the old phone must be synced first.

## R4 S14
**Priority:** Must have

**User story:** As a club organiser, I want to run check-in without a connection, so that a weak venue signal does not stop my event.

**Acceptance criteria:**
1. Given I opened the event online before it started
   When the venue has no internet
   Then the changing QR code and the 6-digit manual code keep working.

2. Given I have not opened the event online beforehand
   When I try to start check-in
   Then I see a warning that the event must be loaded while online first.

## R4 S15
**Priority:** Must have

**User story:** As a club organiser, I want check-ins from people who were not at my event to be rejected, so that my attendance list can be trusted.

**Acceptance criteria:**
1. Given a student sends a photo of an old code
   When the code is older than its 30-second window
   Then the check-in is rejected.

2. Given a student scans after the event has ended plus the 15-minute grace period
   When the record reaches the server
   Then it is rejected as "outside event time".

3. Given a student's location is far from the venue
   When the record reaches the server
   Then it is marked Needs review and not rejected automatically.

## R4 S16
**Priority:** Must have

**User story:** As a club organiser, I want a check-in to depend on my event's code and not on the student's phone clock, so that changing the date on a phone does not help anyone cheat.

**Acceptance criteria:**
1. Given a student changes their phone time to a time inside the event
   When the code window in the record falls outside the event time
   Then the check-in is rejected.

## R4 S17
**Priority:** Must have

**User story:** As a club organiser, I want copied or edited check-ins to be rejected, so that nobody can inflate attendance for my event.

**Acceptance criteria:**
1. Given a check-in was already accepted
   When the same check-in is sent again
   Then no second record is created.

2. Given someone edits the saved check-in on their phone
   When it is uploaded
   Then it is rejected and the account is flagged for review.

3. Given a student tries to check in a second time for the same event
   When the record reaches the server
   Then it is rejected as a duplicate.

## R4 S18
**Priority:** Must have

**User story:** As a club organiser, I want to review check-ins that need a decision, so that honest students are not unfairly excluded.

**Acceptance criteria:**
1. Given some records are marked Needs review
   When I open the event's attendance list
   Then I can see each student, the reason it needs review, and "Approve" and "Reject" buttons.

2. Given I approve or reject a record
   When I confirm
   Then the decision is saved with my name and the time.

## R4 S19
**Priority:** Nice to have

**User story:** As a club organiser, I want the app to cope with my phone's clock being wrong, so that valid check-ins are not rejected by mistake.

**Acceptance criteria:**
1. Given my phone clock is a few minutes wrong
   When students' check-ins are verified
   Then small differences are tolerated.

2. Given my phone clock is very wrong
   When I start check-in while online
   Then I am warned to fix it.

## R4 S20
**Priority:** Nice to have

**User story:** As an s-CPD admin, I want accounts with repeated rejected or tampered check-ins to be flagged in the backend, so that I can investigate misuse.

**Acceptance criteria:**
1. Given an account has 3 or more rejected check-ins for tampering or duplicates
   When I look at the backend data
   Then the account appears in a flagged list with the reasons.

# R5: Points and Progress

## R5 S1
**Priority:** Must have

**User story:** As a student, I want to see my total s-CPD points on my home screen, so that I know how far I have got.

**Acceptance criteria:**
1. Given I have earned points
   When I open the app
   Then my total is shown and matches the sum of my credited and reversed entries.

2. Given I have earned no points yet
   When I open the app
   Then I see 0 and a short message explaining how to earn points.

## R5 S2
**Priority:** Must have

**User story:** As a student, I want a history of every points entry, so that I can see where each point came from.

**Acceptance criteria:**
1. Given I have points entries
   When I open "Points history"
   Then each entry shows the event, the date, the points and its status (Pending, Credited or Reversed), newest first.

## R5 S3
**Priority:** Must have

**User story:** As a student, I want to see exactly when my points will be added, so that I am not worried when they do not appear straight away.

**Acceptance criteria:**
1. Given I checked in but the event has not ended or my record is not verified yet
   When I open my points
   Then the points appear as "Pending" with the reason and are not in my total.

2. Given my record is Verified and the event has ended
   When the points are processed
   Then they move from Pending to Credited and my total goes up.

## R5 S4
**Priority:** Must have

**User story:** As a student, I want points to be given only for people who genuinely attended, so that my points are respected by the university and others.

**Acceptance criteria:**
1. Given my record is Verified and the event has ended
   When points are processed
   Then exactly one credit is created for that event.

2. Given my record is Rejected or I have no record
   When points are processed
   Then no credit is created.

3. Given points were already credited
   When the process runs again
   Then I do not receive double points.

## R5 S5
**Priority:** Must have

**User story:** As a student, I want to be told clearly if points are taken away later, so that I am never surprised and can challenge it.

**Acceptance criteria:**
1. Given a credited record is later found invalid
   When the points are reversed
   Then my history shows a "Reversed" entry with the reason and the original credit stays visible.

2. Given points were reversed
   When this happens
   Then my total goes down by that amount and I receive a notification.

## R5 S6
**Priority:** Nice to have

**User story:** As a student, I want to appeal a rejection or reversal, so that a mistake can be corrected.

**Acceptance criteria:**
1. Given a record is Rejected or Reversed
   When I tap "Appeal" and send a note
   Then the record moves to "Needs review" and I can see the appeal status.

2. Given my appeal is accepted
   When the decision is made
   Then I receive the points as a new credit entry and I am notified.

## R5 S7
**Priority:** Must have

**User story:** As a student, I want to see my points when I am offline, so that I can check my progress anywhere.

**Acceptance criteria:**
1. Given I have opened my points online before
   When I open them without a connection
   Then I see my last known total and history with "Last updated" and an offline notice.

## R5 S8
**Priority:** Must have

**User story:** As a student, I want no points for a cancelled event, so that my total stays correct.

**Acceptance criteria:**
1. Given an event I checked in for is cancelled
   When points are processed
   Then no points are credited and my Pending entry shows "Event cancelled".

## R5 S9
**Priority:** Must have

**User story:** As a student, I want nobody, including me, to be able to change points directly, so that my s-CPD record can be trusted.

**Acceptance criteria:**
1. Given I use a modified app or send my own request to change my points
   When the request reaches the backend
   Then it is denied and my points are unchanged.

2. Given a club organiser tries to add points to a student directly
   When the request reaches the backend
   Then it is denied.

## R5 S10
**Priority:** Nice to have

**User story:** As a student, I want to see my progress towards a target and by category, so that I know what kinds of activities I still need.

**Acceptance criteria:**
1. Given I have credited points
   When I open the points screen
   Then a progress bar shows my total against the target and a breakdown by category is available.

## R5 S11
**Priority:** Nice to have

**User story:** As a student, I want a notification when points are credited, so that I get quick feedback for attending.

**Acceptance criteria:**
1. Given points are credited
   When the credit is created
   Then I receive a notification and tapping it opens my points history.

## R5 S12
**Priority:** Must have

**User story:** As a club organiser, I want to see who attended my event and whether they have been credited, so that I can confirm my students were rewarded.

**Acceptance criteria:**
1. Given my event has ended
   When I open its attendance list
   Then I can see counts and each student's status: Verified, Pending, Needs review or Rejected, and whether points were credited.

## R5 S13
**Priority:** Nice to have

**User story:** As a club organiser, I want to flag a suspicious attendee, so that the s-CPD office can check them before points stand.

**Acceptance criteria:**
1. Given I think a student did not really attend
   When I tap "Flag" and add a note
   Then the record moves to Needs review and I cannot remove points myself.

## R5 S14
**Priority:** Must have

**User story:** As an s-CPD admin, I want the points value of an event to be fixed when I approve it, so that nobody can change it after students attend.

**Acceptance criteria:**
1. Given an event was approved with a points value
   When a club tries to change the points
   Then the change is refused and the approved value stays.

## R5 S15
**Priority:** Must have

**User story:** As an s-CPD admin, I want to invalidate a record and reverse its points, so that cheating found later can be corrected.

**Acceptance criteria:**
1. Given I mark an attendance record invalid in the backend
   When the change is saved
   Then a reversal entry is created, the student is notified, and the earlier credit is never deleted.

## R5 S16
**Priority:** Must have

**User story:** As an s-CPD admin, I want a full history of every credit and reversal with its reason, so that I can answer questions and audits.

**Acceptance criteria:**
1. Given points were credited or reversed
   When I check the backend
   Then each entry shows the student, event, amount, type, reason and time.

# R6: Roles and Access

## R6 S1
**Priority:** Must have

**User story:** As a student, I want to log in with my university email and password, so that only I can see my bookings and points.

**Acceptance criteria:**
1. Given I have an account
   When I enter my correct email and password and tap "Log in"
   Then I see my student home screen.

2. Given I enter a wrong password
   When I tap "Log in"
   Then I see "The email or password is incorrect".

3. Given I leave the email empty or type an invalid email
   When I tap "Log in"
   Then I see a field error and nothing is sent.

4. Given I enter a wrong password several times
   When I try again
   Then I am temporarily blocked and told when I can retry.

## R6 S2
**Priority:** Must have

**User story:** As a club organiser, I want to log in with the account the s-CPD office gave me, so that only my club can manage its events.

**Acceptance criteria:**
1. Given I have a club account
   When I log in with the correct details
   Then I see the club home screen and not the student screens.

## R6 S3
**Priority:** Nice to have

**User story:** As a student, I want to reset my password if I forget it, so that I do not get locked out.

**Acceptance criteria:**
1. Given I enter a registered email on "Forgot password"
   When I submit
   Then I receive a reset email.

2. Given I enter an email that is not registered
   When I submit
   Then I see the same message as above, so no one can find out who has an account.

## R6 S4
**Priority:** Must have

**User story:** As a student, I want to see only the student screens, so that the app is simple and I do not see features that are not for me.

**Acceptance criteria:**
1. Given I am logged in as a student
   When I use the app
   Then I see Marketplace, My events, Points and Profile, and no club or admin screens.

## R6 S5
**Priority:** Must have

**User story:** As a club organiser, I want to see the club screens for my events, so that I can create events, run check-in and see attendance.

**Acceptance criteria:**
1. Given I am logged in as a club
   When I use the app
   Then I see My events, Create event, Check-in and Attendance.

## R6 S6
**Priority:** Must have

**User story:** As a student, I want other students unable to see my bookings or points, so that my activity stays private.

**Acceptance criteria:**
1. Given another student tries to open my bookings or points
   When the request reaches the backend
   Then it is denied.

## R6 S7
**Priority:** Must have

**User story:** As a club organiser, I want students unable to use club functions, even with a modified app, so that nobody can create fake events or read my event codes.

**Acceptance criteria:**
1. Given a student account tries to create an event or read an event's attendance secret
   When the request reaches the backend
   Then it is denied with a permission error.

## R6 S8
**Priority:** Must have

**User story:** As a club organiser, I want to manage only my own club's events, so that other clubs cannot interfere with mine.

**Acceptance criteria:**
1. Given I am a member of club A
   When I try to read or change an event or attendee list of club B
   Then it is denied.

## R6 S9
**Priority:** Must have

**User story:** As an s-CPD admin, I want event approvals and role changes done only in the backend, so that no one can give themselves higher access from a phone app.

**Acceptance criteria:**
1. Given any user in the app
   When they try to approve an event or change a role
   Then it is refused because those actions exist only in the backend.

## R6 S10
**Priority:** Must have

**User story:** As an s-CPD admin, I want only the s-CPD office to create club accounts, so that nobody can pretend to be a club.

**Acceptance criteria:**
1. Given someone registers in the app
   When the account is created
   Then it is always a student account.

2. Given a club account is needed
   When the office creates it
   Then it is linked to exactly one club in the backend.

## R6 S11
**Priority:** Must have

**User story:** As an s-CPD admin, I want to disable an account quickly, so that misuse can be stopped.

**Acceptance criteria:**
1. Given I disable an account in the backend
   When that user next opens or refreshes the app
   Then they are signed out and cannot log in again.

## R6 S12
**Priority:** Must have

**User story:** As a student, I want to be signed out safely when I log out or my session ends, so that a shared or lost phone does not expose my data.

**Acceptance criteria:**
1. Given I log out with nothing waiting to sync
   When I confirm
   Then my saved data is removed from the phone and I return to the login screen.

2. Given my session expired or was revoked
   When I open the app online
   Then I see the login screen with a clear message.

## R6 S13
**Priority:** Must have

**User story:** As a student, I want my account to work for check-in on only one phone, so that no one else can check in as me from another device.

**Acceptance criteria:**
1. Given my phone is registered to my account
   When someone tries to register the same phone to a different account
   Then it is refused.

2. Given I try to check in from a phone that is not registered to my account
   When the record reaches the server
   Then it is rejected.

## R6 S14
**Priority:** Nice to have

**User story:** As a student, I want to lock the app with my fingerprint or PIN, so that someone who picks up my phone cannot open it.

**Acceptance criteria:**
1. Given app lock is turned on
   When I return to the app after 5 minutes
   Then I must use my fingerprint or PIN to continue.

## R6 S15
**Priority:** Nice to have

**User story:** As an s-CPD admin, I want to change or remove a club's access when its organiser changes, so that former organisers cannot manage events.

**Acceptance criteria:**
1. Given a club organiser has left
   When I replace their access in the backend
   Then they can no longer see or change the club's events.
