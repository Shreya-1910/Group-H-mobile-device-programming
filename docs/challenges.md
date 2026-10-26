# Challenges and Assumptions

## R1: Event Approval

**Risk 1: Clubs get around the approval workflow**
Why it matters: A mistake in the backend security rules could let a club approve its own event, raise its points or change the time after students have booked. This would undermine the whole s-CPD system.
How we handle it: All access is denied by default and only allowed where a story needs it. Status, points, time and capacity are locked once an event is submitted (R1-S7). Every 🛡 story has an automated rule test in the Firebase Emulator Suite, and the tests run before each merge.

**Risk 2: Events are not reviewed in time**
Why it matters: Approval happens in a separate admin tool (R6-S21), which is extra work to build. If the s-CPD office is slow, a club can't advertise its event.
How we handle it: The admin tool is kept simple: a list of pending events with Approve and Reject buttons, plus an audit log. Each event shows a 5-working-day due date, with overdue events first. An event still Pending at its start time expires automatically and the club is notified (R1-S8).

## R2: Marketplace and Booking

**Risk 1: An event is overbooked**
Why it matters: If two students take the last place at once, or the waitlist books someone twice, a club could get more students than the venue holds.
How we handle it: Each booking and cancellation runs as one server transaction, so only one request can take a place (R2-S6). We test this with 100 requests for 10 places (R2 NFR3). If automatic waitlist booking proves unreliable, we'll switch to a "place available" offer that the student accepts.

**Risk 2: Server functions need a paid Firebase plan**
Why it matters: Booking, check-in validation and event expiry use Cloud Functions, which need Firebase's paid Blaze plan.
How we handle it: We develop and demo with the free Firebase Emulator Suite. If we deploy, a USD 1 budget alert stops unexpected charges. As a last resort, simple checks move into Firestore security rules.

## R3: Secure Attendance

**Risk 1: The emulator can't scan QR codes**
Why it matters: The Pixel 2 emulator has no real camera, so the main check-in flow can't be shown during marking.
How we handle it: The 6-digit code is built from the start and validated exactly like a scan (R3-S3), so check-in can be shown and tested on the emulator. Scanning is also tested on one real phone.

**Risk 2: Signed rotating codes are new to us**
Why it matters: The HMAC signing, the 15-second window counter and the clock rules are the hardest part of R3. If the club phone's clock is wrong, genuine students could be rejected.
How we handle it: We build a small prototype in week 1, before any screens, using standard libraries (`javax.crypto` and ZXing). The current and previous codes are both accepted, and the club app syncs its time whenever it's online (R4-S19). If a student is still wrongly rejected, the organiser can mark them present, with an audit trail (R3-S6).

**Risk 3: Copied codes can't be fully stopped offline**
Why it matters: The server can't tell when an offline scan happened. A code sent live to an absent friend and scanned offline therefore looks genuine (R3-S7).
How we handle it: Offline records must match the club's session log, far-away locations are flagged, and organisers see the number of scans for each code display (R3-S5). We list this as a known weakness in the security analysis rather than claiming it is solved.

## Assumptions (R1–R3)

- Students register with their university email, and the s-CPD office creates club accounts (R6-S10, R6-S16).
- Clubs run check-in on their own Android phone and open the event online beforehand (R4-S14).
- Events are in person, in Malaysia Time, and shorter than one day.
- Points are 1–10, check-in runs from the event start to 15 min after the end, and booking closes 2 h before the start. These are placeholder values for the s-CPD office to confirm, stored as settings so they can change without a redesign.
- Students who join a waitlist accept an automatic booking.
- Most students' phones can scan a QR code. Those that can't use the 6-digit code, or the organiser marks the student present.


# **R4: Online and offline**

**Risk 1: Offline check-ins are lost, stuck or uploaded twice**

**Why it matters:** A check-in saved without a connection could disappear after the app is closed, stay on syncing forever, or be counted twice after a retry. It could also be lost if the phone storage is full or the app is uninstalled before it uploads.

**How we handle it:** Check-ins go into a local database before anything else, with an all-or-nothing save, and are uploaded in the background with retries (30 seconds doubling for 5 attempts, then every 6 hours until 72 hours after the event). The server accepts each record only once. Every check-in shows one clear status from a single list (Pending, Syncing, Verified, Rejected, Needs review, Couldn't sync), and a failed upload shows a retry button. The app warns when storage is low, warns before logout or uninstall with unsynced records, and lets a student send a missing check-in claim. We test offline, restart, retry, full-storage and duplicate cases automatically.

**Risk 2: Offline records are faked or replayed**

**Why it matters:** Someone could edit a saved record, resend an old one, or change the phone clock to look like they were on time.

**How we handle it:** Each record is signed by the phone with a key in the Android Keystore and checked by the server, which also rejects repeats. Validity depends on the club's changing code (15-second step, current and previous accepted, so up to 30 seconds) and on the club device's session log, and not on the phone clock. An offline record whose code does not appear in the session log is marked Needs review. A friend inside the room can still relay a code within 30 seconds, and a forwarded screenshot scanned offline still appears in the session log. We reduce this with the venue location check and organiser review, and we admit it as a known weakness in the security analysis.

**Risk 3: The emulator cannot scan or locate**

**Why it matters:** The Pixel 2 emulator has no real camera view or GPS, and its key storage is software-based, so offline attendance and signing are hard to demonstrate.

**How we handle it:** We use a physical Android phone for camera, GPS and hardware-backed Keystore testing, while retaining the emulator for repeatable tests. Offline attendance is tested by disabling network connectivity, verifying that check-ins are stored locally, and confirming that they synchronise when connectivity is restored. A six-digit manual code is provided as an alternative check-in method.

**Risk 4: Genuine attendees are marked as no-shows**

**Why it matters:** Offline check-ins can reach the server up to 72 hours after the event, so a student who really attended could look like a no-show and be penalised too early.

**How we handle it:** A booking shows "Waiting for sync period" after the event and is marked as a no-show only after the 72-hour deadline, and never while a record is still Needs review. Organisers see "Possible no-show until [time]" until then, and an approved missing check-in claim removes the mark.

**Risk 5: A student cannot cancel when the app says they can**

**Why it matters:** Cancelling a booking needs the server to release the place. A cancellation that is only saved on the phone would leave the place taken and make the student believe it was cancelled.

**How we handle it:** The "Can't make it" button works only when the phone is online and before the 2-hour cut-off. Offline or after the cut-off it is disabled and shows the reason, and nothing is queued. The button enables itself again if the connection returns before the cut-off.

# **R5: Points and Progress**

**Risk 6: Points are given twice or to the wrong person**

**Why it matters:** Running the crediting process more than once, or a late record arriving after processing, could create extra points.

**How we handle it:** Points are an append-only list with one credit per student per event. Running the process again, or two runs at the same time, changes nothing. Only server code can write to it, and processing runs at least every hour with a warning to the admin if it fails for 24 hours.

**Risk 7: A credited record is found invalid later**

**Why it matters:** Removing points can confuse students and leave the history unclear. It can also change levels and badges that depend on points.

**How we handle it:** We never delete an entry. A reversal entry with a reason is added, the student is notified, and levels and badges are recalculated and explained. The student can appeal, and the appeal is decided by the s-CPD admin within 5 working days, never by the club whose decision is being appealed.

**Risk 8: Students miss important updates because notifications are off or fail**

**Why it matters:** Credits, reversals, appeal decisions, cancelled events and reminders depend on notifications. A student who has turned them off, or whose message fails to send, could miss a reversal or an appeal deadline.

**How we handle it:** Every important message is also saved in an in-app Notifications list with an unread badge, and failed messages are retried 3 times over 24 hours. A prompt explains what is missed when notifications are off. Reminders are never sent late after the event has started, and the check-in alert fires when the check-in window opens.

# **R6: Roles and Access**

**Risk 9: A user bypasses their role with a modified app**

**Why it matters:** Hiding screens does not stop a student from sending requests that only a club or admin should make.

**How we handle it:** Roles are enforced on the backend with account roles and security rules. We write tests to prove a student is denied club actions and one club cannot touch another club's events. Admin actions exist only in the backend, through a defined admin tool with a reason for every action and an audit log that cannot be edited.

**Risk 10: Account or phone sharing**

**Why it matters:** A student could lend their login or phone so a friend checks in for them.

**How we handle it:** Each account is tied to one registered phone and one phone to one account, registered the first time the student logs in online. Changing phone needs a re-registration step (limited to once every 7 days), and a factory reset needs the phone to be registered again. The optional fingerprint or PIN check adds protection, but a student who hands over both phone and password cannot be stopped.

**Risk 11: Privacy and lost phones**

**Why it matters:** The app handles emails, location, login tokens and device keys, and the Malaysian Personal Data Protection Act 2010 applies.

**How we handle it:** We collect only what is needed, ask for consent at sign-up, capture location only at check-in and delete it 30 days after the event, and store no face or fingerprint data. A student can ask for their data to be deleted within 30 days, except records the university must keep. Tokens are stored securely, a lost or disabled account is signed out at the next refresh, and local data is removed on logout after warning about unsynced check-ins.

**Risk 12: Former students keep using the system**

**Why it matters:** Checking only that an email looks like a university email does not show the student is still enrolled. A graduated student whose email is deactivated could keep an active account and collect points.

**How we handle it:** Registration accepts only the university domain and requires email verification. The student gives year, degree and start year, and the system works out the expected end date. A daily check warns 30 days before, flags at the end date and deactivates after 30 more days unless the admin approves an extension. The account is also checked at every token refresh. Points and attendance records are kept after deactivation.

# Assumptions

## A. System Assumptions

- **A1:** Offline check-ins must sync within 72 hours of the event ending.
- **A2:** Check-in codes refresh regularly, with a short grace period after the event ends.
- **A3:** Students can check in using a QR code or a 6-digit manual code.
- **A4:** Failed uploads are retried automatically, with warnings for sync failures and low storage.
- **A5:** Suspicious check-ins, missing attendance claims and appeals are reviewed by the club or administrator.
- **A6:** Attendance points are credited after records are verified.
- **A7:** Accounts have password, verification, session and lockout protections.
- **A8:** Users can access only the features permitted by their assigned roles.
- **A9:** The system uses server time for deadlines and assumes organisers load event details before going offline.
- **A10:** The university provides the student and degree information required for account verification.

## B. Quality Targets

- **B1:** The app supports Android 10 and above.
- **B2:** The system handles peak check-in traffic with minimal delays.
- **B3:** The interface is accessible, supports TalkBack and uses appropriately sized touch targets.
- **B4:** Attendance records and audit logs are retained for the required periods.
- **B5:** Location data is deleted according to the defined retention policy.
- **B6:** Screens load within the specified performance targets under normal network conditions.
- **B7:** Authentication, access control and data protection measures safeguard user information.

## C. Operational Conditions

- **C1:** Students have a compatible Android phone or can enter the manual check-in code.
- **C2:** Organisers have sufficient battery power and event information loaded before going offline.
- **C3:** Organisers upload their session logs within the required timeframe.
- **C4:** Events have a defined venue when location verification is required.
- **C5:** University staff are available to review appeals and flagged records.
- **C6:** Students protect their login credentials and devices; the system cannot fully prevent misuse when both are shared.
