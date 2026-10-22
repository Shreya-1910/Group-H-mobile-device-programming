# mobile-device-programming
# R4: Online and Offline Functionality

## User R4-S1

**Priority:** Must have

**User Story:** As a student attending an event without internet access, I want to scan the event QR code and save my attendance locally, so that I can check in even when the network is unavailable.

**Acceptance Criteria:**
- Given that the device is offline, when the student scans a valid QR code within the event's permitted check-in window, the app saves the attendance record locally within 1 second.
- The app displays “Saved on this phone, waiting to sync” and the capture time.
- No network request is required to save the record.

## User R4-S2

**Priority:** Must have

**User Story:** As a student, I want my pending attendance records to sync automatically when internet access returns, so that I do not need to upload them manually.

**Acceptance Criteria:**
- Given that unsent attendance records exist, when connectivity returns, the app begins synchronisation within 60 seconds.
- Failed uploads are retried using exponential backoff.
- Pending records remain stored across app termination and device reboot.
- Repeated upload attempts must not create duplicate server records.

## User R4-S3

**Priority:** Must have

**User Story:** As a student, I want to see the current status of each attendance record, so that I know whether my attendance has been accepted.

**Acceptance Criteria:**
- Each record displays one status: Pending, Syncing, Verified, Rejected (with a reason), or Needs review.
- When unsent records exist, a banner shows the number of records waiting to sync.
- The screen includes a “Sync now” button and a “Last synced” timestamp.
- Record statuses persist after the app is restarted and update when the server returns a result.

## User R4-S4

**Priority:** Must have

**User Story:** As a club organiser, I want to operate the check-in screen without internet access, so that poor venue connectivity does not interrupt attendance collection.

**Acceptance Criteria:**
- Given that the club device has downloaded the event's required secrets and attendee roster while online, the rotating QR code and manual check-in code continue to work offline.
- The app displays an offline warning.
- If the required data has not been downloaded, the app prevents check-in from starting and explains that an online setup is required.

## User R4-S5

**Priority:** Must have

**User Story:** As the system, I want to prevent duplicate and replayed attendance submissions, so that the same check-in cannot be counted more than once.

**Acceptance Criteria:**
- Given that an attendance submission has already been accepted, when the same submission is uploaded again, the server returns the existing result without creating another record.
- A second attendance record for the same student and event is rejected as a duplicate.

## User R4-S6

**Priority:** Must have

**User Story:** As the system, I want to detect altered or forged offline attendance records, so that modifying local data cannot be used to claim attendance fraudulently.

**Acceptance Criteria:**
- When a record is uploaded, the server verifies its digital signature against the device key registered to the student's account.
- If the record has been altered or the signature is invalid, the record is rejected and the account is flagged for review.

## User R4-S7

**Priority:** Must have

**User Story:** As the system, I want to validate offline attendance using the event's QR-code time window rather than trusting the phone's clock, so that changing the device date or time cannot bypass attendance restrictions.

**Acceptance Criteria:**
- The server validates the QR-code window counter against the event's permitted check-in window.
- A counter outside the permitted window is rejected, even if the device clock has been changed.
- The device-reported time and available monotonic-clock information are recorded as anomaly signals for review, rather than being treated as authoritative proof of attendance time.

## User R4-S8

**Priority:** Must have

**User Story:** As a student, I want to understand why an offline attendance record was rejected when I synchronised too late, so that I can take appropriate action.

**Acceptance Criteria:**
- The synchronisation deadline is 72 hours after the event ends.
- A record uploaded after this deadline is marked “Rejected — Synced too late”, with an option to appeal.
- A record uploaded before the deadline is assessed using its validated capture evidence and event-window information, rather than being rejected simply because it was uploaded after the event ended.

## User R4-S9

**Priority:** Must have

**User Story:** As a student, I want to access my booked events and ticket details offline, so that I can find event information even without internet access.

**Acceptance Criteria:**
- The app caches the student's booked events, venue details and ticket information in the local database.
- When offline, the app displays the saved information and a notice stating “Offline: showing saved data”.

## User R4-S10

**Priority:** Must have

**User Story:** As a club organiser, I want to review attendance records that cannot be verified automatically, so that legitimate attendance is not unfairly rejected.

**Acceptance Criteria:**
- Records requiring manual verification, such as attendance without a matching booking or a location mismatch, appear in a review list accessible to authorised club organisers.
- Each record can be approved or rejected, with a reason recorded for the decision.
- Every decision is logged with the acting user's identity and timestamp.

## User R4-S11

**Priority:** Must have

**User Story:** As a student, I want my locally stored account and attendance data to be protected, so that sensitive information is not exposed if my phone is lost.

**Acceptance Criteria:**
- Authentication tokens are stored using EncryptedSharedPreferences or an appropriate supported secure-storage alternative.
- Attendance records are signed using a non-exportable Android Keystore private key.
- On logout, authentication credentials and user-specific cached data are cleared from the device.

## User R4-S12

**Priority:** Nice to have

**User Story:** As a student, I want to confirm my identity using my fingerprint or device PIN before checking in, so that someone borrowing my unlocked phone cannot easily record attendance on my behalf.

**Acceptance Criteria:**
- When biometric or device-credential confirmation is enabled, scanning a valid QR code prompts the student to authenticate before the attendance record is signed.
- Cancelling or failing authentication creates no attendance record.
- Authentication works offline using the device's supported authentication mechanism.
- The app never stores fingerprint or facial biometric data.

---

# R5: Points and Progress

## User R5-S1

**Priority:** Must have

**User Story:** As a student, I want to see my total s-CPD points on the home screen, so that I can track my accumulated points at a glance.

**Acceptance Criteria:**
- The displayed total equals the sum of all CREDIT and REVERSAL ledger entries, with reversals reducing the total.
- When online, the total is updated within 5 seconds of a confirmed ledger change.

## User R5-S2

**Priority:** Must have

**User Story:** As a student, I want to view my points history, so that I can understand when and why points were awarded or reversed.

**Acceptance Criteria:**
- The history lists the associated event, date, point value and status for each entry.
- Entries are displayed newest first and show Pending, Credited or Reversed, as applicable.
- An empty state explains how students can earn s-CPD points.

## User R5-S3

**Priority:** Must have

**User Story:** As a student, I want points to be awarded only for verified attendance at completed events, so that the points system remains fair and accurate.

**Acceptance Criteria:**
- When an attendance record is Verified and the event has ended without being cancelled, the system creates exactly one CREDIT ledger entry for the eligible points.
- Rejected or missing attendance records receive no credit.
- Repeated processing must not create additional credits for the same attendance record.

## User R5-S4

**Priority:** Must have

**User Story:** As a student, I want pending points to be shown separately from my confirmed total, so that I understand why some points are not yet counted.

**Acceptance Criteria:**
- Points awaiting attendance verification or event completion are displayed as Pending, together with the reason.
- Pending points are excluded from the confirmed points total until the eligibility conditions are satisfied.

## User R5-S5

**Priority:** Must have

**User Story:** As a student, I want to be notified if previously credited points are later invalidated, so that I understand why my total has changed.

**Acceptance Criteria:**
- When credited attendance is subsequently invalidated, the system creates a REVERSAL ledger entry containing the number of points reversed and the reason.
- The total is recalculated accordingly, and a push notification is sent.
- The original CREDIT entry remains visible in the history; neither entry is deleted.

## User R5-S6

**Priority:** Must have

**User Story:** As a student, I want to appeal a rejected attendance record or points reversal, so that an incorrect decision can be reviewed.

**Acceptance Criteria:**
- The student can submit an appeal with an optional explanatory note.
- The appeal appears in the review queue, and its status is visible to the student.
- If the appeal is upheld, the system creates a new CREDIT ledger entry where the student is eligible for the points.
- Existing ledger entries remain unchanged.

## User R5-S7

**Priority:** Nice to have

**User Story:** As a student, I want to view my points total and history offline, so that I can check my progress without internet access.

**Acceptance Criteria:**
- When offline, the app displays the most recently cached points total and history, together with a “Last updated” timestamp and an offline notice.
- The app identifies the information as saved data rather than implying it is current.

## User R5-S8

**Priority:** Must have

**User Story:** As the system, I want to prevent students and club organisers from directly modifying the points ledger, so that points cannot be forged or manipulated.

**Acceptance Criteria:**
- Client applications cannot create, modify or delete points ledger entries.
- Backend security rules deny direct client writes.
- Only authorised server-side functions using the Admin SDK can modify the ledger.
- Automated security-rule tests verify that student and club accounts cannot write ledger entries.

## User R5-S9

**Priority:** Nice to have

**User Story:** As a student, I want to compare my points against a target and view a category breakdown, so that I can monitor my progress across different activities.

**Acceptance Criteria:**
- The app displays a progress indicator showing the student's confirmed points against the configured target.
- It also displays a breakdown by configured activity categories, such as sport and volunteering.
- Pending points are distinguished from confirmed points.

## User R5-S10

**Priority:** Must have

**User Story:** As a club organiser, I want to view attendance and points-credit status for each event, so that I can confirm which attendees have received their points.

**Acceptance Criteria:**
- The event details screen lists attendees and their attendance status: Verified, Pending or Rejected.
- It also shows relevant attendee and status counts, together with the associated credit status.
- An authorised club organiser can flag a record for review but cannot directly create, edit or reverse points ledger entries.

## User R5-S11

**Priority:** Must have

**User Story:** As a student, I want to receive a notification when points are credited, so that I know when my attendance has earned points.

**Acceptance Criteria:**
- When a CREDIT entry is created, the system sends a push notification to the student.
- Tapping the notification opens the relevant points history or credit entry.

## User R5-S12

**Priority:** Must have

**User Story:** As a student, I want cancelled events to award no points, so that my points total reflects only eligible activities.

**Acceptance Criteria:**
- A cancelled event cannot generate new CREDIT entries.
- Any existing pending point claims associated with the event are closed with the reason “Event cancelled”.
- If points were already credited before cancellation, the system follows the defined reversal and review process rather than silently deleting the original credit.

---

# R6: Roles and Access Control

## User R6-S1

**Priority:** Must have

**User Story:** As a user, I want to sign in using my university email and password, so that I can securely access my own account and data.

**Acceptance Criteria:**
- The login form validates that the email field is not empty and contains a valid email format before submission.
- Successful authentication opens the home screen for the user's assigned role.
- Invalid credentials produce a clear error message.
- Repeated failed attempts trigger a temporary lockout or equivalent rate-limiting protection.

## User R6-S2

**Priority:** Must have

**User Story:** As a user, I want the app to display functions relevant to my role, so that I can navigate the system without unnecessary options.

**Acceptance Criteria:**
- Students can access Marketplace, My Events, Points and Profile.
- Club organisers can access My Events, Create Event, Check-in and Attendance.
- The navigation graph is selected using the authenticated account's role.
- Hiding a screen does not replace backend authorisation checks.

## User R6-S3

**Priority:** Must have

**User Story:** As the system, I want to prevent students from accessing club-only functions, even if they modify the app or call the API directly, so that role restrictions cannot be bypassed.

**Acceptance Criteria:**
- Requests from a student account to create events or read event attendance secrets are denied by backend authorisation rules.
- Automated security tests verify that student credentials cannot perform these restricted operations.

## User R6-S4

**Priority:** Must have

**User Story:** As a club organiser, I want to manage only events belonging to my own club, so that I cannot access or modify another club's information.

**Acceptance Criteria:**
- A club organiser can read or modify only events and attendee information belonging to the club associated with their authorised account.
- The backend verifies that the account's club ID matches the event's club ID.
- Access to another club's records is denied, including through direct API requests.

## User R6-S5

**Priority:** Must have

**User Story:** As an s-CPD administrator, I want account approvals and role assignments to be controlled through trusted backend operations, so that users cannot grant themselves privileged access.

**Acceptance Criteria:**
- The mobile app contains no administrator-only screens for granting roles or approving accounts.
- Role assignments and approval status can be changed only through authorised backend scripts or administrative tools.
- Client requests to modify these fields are denied by security rules.

## User R6-S6

**Priority:** Must have

**User Story:** As a user, I want logout and session expiry to clear my private data from the device, so that another person using the same phone cannot access my account.

**Acceptance Criteria:**
- On logout, the app clears authentication credentials and user-specific cached data.
- If the session expires or is revoked, the app returns the user to the login screen and displays an appropriate message.
- Protected operations are denied once the session is no longer valid.

## User R6-S7

**Priority:** Must have

**User Story:** As a user, I want to reset my forgotten password, so that I can regain access to my account securely.

**Acceptance Criteria:**
- The user can request a password-reset email by entering their university email address.
- The on-screen response is the same whether or not the address is registered, preventing account enumeration.
- The reset process follows the authentication provider's secure verification requirements.

## User R6-S8

**Priority:** Must have

**User Story:** As the system, I want each student account to be associated with one registered attendance device, so that sharing accounts across devices to record attendance is more difficult.

**Acceptance Criteria:**
- A device's registered attendance key cannot be associated with a different student account while its existing registration remains active.
- Changing phones requires an authorised re-registration process that invalidates the previous key.
- Attendance submissions signed by an invalidated device key are rejected.

## User R6-S9

**Priority:** Nice to have

**User Story:** As a user, I want optional biometric or device-PIN protection when reopening the app, so that someone picking up my phone cannot immediately access my account.

**Acceptance Criteria:**
- When app-lock protection is enabled, resuming the app after 5 minutes of inactivity prompts for fingerprint or device-PIN authentication.
- Failed or cancelled authentication keeps protected screens inaccessible until successful authentication or an authorised re-login.

## User R6-S10

**Priority:** Must have

**User Story:** As an s-CPD administrator, I want to disable accounts or revoke a club's access, so that misuse can be stopped promptly.

**Acceptance Criteria:**
- An authorised administrator can disable an account or revoke a club's access through the backend.
- Disabled accounts cannot establish new sessions, and existing sessions are invalidated or denied access when the revocation is enforced.
- The restriction is applied on subsequent protected requests and token refreshes, without relying solely on the mobile app to detect the change.
