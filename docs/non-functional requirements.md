### R1 Non-functional requirements

* R1 NFR1 (Usability): A club organiser creates and submits a complete event within 3 minutes without help, with at least 9 of 10 test participants succeeding. Every invalid field shows its error next to the field (R1-S1).
* R1 NFR2 (Security): Automated security-rule tests show 0 successful club writes to status, points, time or capacity after submission (R1-S7). 100% of approvals, rejections and expiries create an audit entry with actor, old and new status, reason and time, kept for 2 years (R6-S20).
* R1 NFR3 (Performance): When online, a decision made in the admin tool appears in the club's status list within 5 seconds on 95% of decisions. An event still Pending at its start time is marked Expired within 5 minutes (R1-S8).

### R2 Non-functional requirements

* R2 NFR1 (Usability): A first-time student finds and books a named event within 60 seconds without help, with at least 9 of 10 test participants succeeding.
* R2 NFR2 (Performance): The first page of the Marketplace (20 of 200 approved events) loads within 2 seconds on 95% of loads, and search or filter results appear within 1 second. Both are measured on an Android 10 emulator with a Pixel 2 profile and the LTE network profile. Offline, the saved list appears within 1 second (R2-S9).
* R2 NFR3 (Reliability): 100 simultaneous booking requests for 10 places, run in the Firebase Emulator Suite, give exactly 10 bookings and 90 waitlist offers, with 0 overbookings across 20 test runs (R2-S6).

### R3 Non-functional requirements

* R3 NFR1 (Performance): The QR code redraws within 0.5 seconds of each 15-second change. A student's phone decodes it within 1 second from 1 m in normal room light, on 95% of scans, measured on one real mid-range phone. Online result time and peak load follow R4 NFR1.
* R3 NFR2 (Reliability): The club check-in screen runs for 3 hours with the screen on, without crashing or skipping a code counter. After the app is closed and reopened, the code display resumes within 3 seconds and continues the same counter sequence (R4-S14).
* R3 NFR3 (Security): Automated tests show forged, expired, out-of-window and duplicate codes are rejected in 100% of test cases (R3-S7 to R3-S9). 100% of manual marks and review decisions are logged with actor, reason and time, and kept for 2 years (R6-S20).
* R3 NFR4 (Accessibility): On all R1–R3 screens, every control has a TalkBack label and touch targets are at least 48 dp. The 6-digit code is shown at 32 sp or larger with a contrast ratio of at least 4.5:1 (WCAG 2.1 AA). TalkBack reads out the check-in result.


## R4 Non-functional requirements
- **R4 NFR1 (Performance):** A scan is saved locally within 1 second on 95% of scans, measured offline on an Android 10 emulator with a Pixel 2 profile and on one real mid-range phone. The server returns a check-in result within 2 seconds (R3-S2) and accepts a peak of 300 check-ins within 2 minutes with 95% processed within 2 seconds.
- **R4 NFR2 (Reliability):** No saved check-in is lost across app closure, restart or power loss (100% in test runs). Uploading 50 waiting check-ins finishes within 30 seconds on 4G. Backoff is 30 seconds doubling for 5 attempts (30 s, 1, 2, 4, 8 minutes), then once every 6 hours until 72 hours after the event, then the record is Rejected as "Synced too late".
- **R4 NFR3 (Security):** Check-ins are signed with a key in the Android Keystore (hardware-backed where the phone supports it); codes are signed by the server; saved records and tokens are stored encrypted on the phone; all uploads use TLS 1.2 or higher.
- **R4 NFR4 (Compatibility):** The app works on Android 10 (API 29) and above (assumed), in portrait and landscape.
- **R4 NFR5 (Accessibility):** Scan and manual entry screens work with TalkBack, every control has a label, touch targets are at least 48 x 48 dp, text contrast meets WCAG 2.1 AA, and every status is shown by text as well as colour.
- **R4 NFR6 (Audit):** Every code display, check-in decision, claim, flag and clearance is written to an audit log with user, action, time and reason. Audit logs are kept for 2 years (assumed) and cannot be edited.
- **R4 NFR7 (Retention and privacy (PDPA 2010)):** Consent for collecting attendance and location is asked at sign-up. Location is used only to check distance to the venue and is deleted 30 days after the event (assumed). Attendance records are kept for 6 years after the event (assumed, confirm with university policy). A student can ask for their personal data to be deleted, and it is deleted within 30 days except records the university must keep.


## R5 Non-functional requirements
- **R5 NFR1 (Performance):** The points total and the first page of history load within 3 seconds on 4G on 95% of loads; saved data shows within 1 second offline.
- **R5 NFR2 (Reliability):** Points are never lost or doubled: a reconciliation between check-in records and credit entries shows 0 differences in each test run. Points processing runs at least every hour, and a failed run is retried and reported to the admin if not completed within 24 hours.
- **R5 NFR3 (Accessibility):** Points screens meet WCAG 2.1 AA; status (Pending, Credited, Reversed) is shown with text and icon as well as colour; TalkBack reads each history row in full.
- **R5 NFR4 (Privacy (PDPA 2010)):** A student's points are visible only to that student and authorised admins; notification text on the lock screen does not show the event name or number of points (on by default).
- **R5 NFR5 (Retention):** Credit and reversal entries are kept for the student's study period plus 6 years (assumed, confirm with university policy) and are not deleted when an account is deactivated (R6 S18).
- **R5 NFR6 (Audit):** All credits, reversals, appeals, flags and denied change attempts are kept in the audit log for at least 2 years.
- 
## R6 Non-functional requirements
- **R6 NFR1 (Performance):** Login, registration steps and password reset screens respond within 3 seconds on 4G on 95% of requests; the verification email is sent within 1 minute.
- **R6 NFR2 (Security):** Passwords are stored only as salted hashes; all traffic uses TLS 1.2 or higher; a session expires after 1 hour unused online with refresh up to 30 days (assumed); every backend request is checked for role and ownership and not only in the app; lock-out is 5 failed attempts for 15 minutes.
- **R6 NFR3 (Privacy (PDPA 2010)):** Only needed data is collected (email, name, student ID, study details, phone key); consent is asked at sign-up; location is kept for 30 days after the event; a student can ask for their data to be deleted and it is deleted within 30 days except records the university must keep.
- **R6 NFR4 (Accessibility and compatibility):** Login, registration and lock screens work with TalkBack, are readable at 200% font size, meet WCAG 2.1 AA and run on Android 10 (API 29) and above (assumed).
- **R6 NFR5 (Reliability):** The daily study-period check runs every day with at least 99% success over a month and alerts an admin when it fails; account disabling takes effect for online users within 5 minutes.
- **R6 NFR6 (Retention):** Deactivated accounts and their points records are kept as set in R5 NFR5; unverified accounts are deleted after 7 days (assumed); audit logs are kept for 2 years (assumed).

