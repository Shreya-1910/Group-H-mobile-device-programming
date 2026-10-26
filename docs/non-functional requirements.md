
## R4 Non-functional requirements

- **R4-NFR1 (Performance):** A scan is saved locally within 1 second on 95% of scans on the minimum supported phone. 
- **R4-NFR2 (Performance):** Uploading 50 waiting check-ins completes within 30 seconds on a 4G connection. 
- **R4-NFR3 (Reliability):** No saved check-in is lost across app closure, restart or phone power loss; target 100% in test runs, and at least 99.5% of check-ins sent from the app eventually reach the server within 72 hours.
- **R4-NFR4 (Compatibility):** The app works on Android 10 (API 29) and above *(assumed)*, in portrait and landscape. 
- **R4-NFR5 (Accessibility):** Scan and manual entry screens support TalkBack, touch targets are at least 48 x 48 dp, text contrast meets WCAG 2.1 AA, and every status is shown by text as well as colour. 
- **R4-NFR6 (Security):** Event codes are signed by the server, so a code cannot be created or changed by a phone; saved check-ins are stored encrypted on the device; all uploads use TLS 1.2 or higher. 
- **R4-NFR7 (Audit):** Every code display, check-in decision (approve, reject, claim), flag and clearance is written to an audit log with user, action, time and reason; audit logs are kept for 2 years *(assumed)* and cannot be edited. 
- **R4-NFR8 (Data retention and privacy):** Attendance records are kept for 6 years after the event. Location is used only to check venue distance and is not stored as a movement history. 


## R5 Non-functional requirements

- **R5-NFR1 (Performance):** The points total and the first page of history load within 3 seconds on 4G on 95% of loads; saved data shows within 1 second offline. 
- **R5-NFR2 (Reliability):** Points are never lost or doubled: reconciliation between check-in records and credit entries shows 0 differences in each test run. 
- **R5-NFR3 (Reliability):** Points processing runs at least every hour, and a failed run is retried and reported to the admin if not completed within 24 hours. 
- **R5-NFR4 (Accessibility):** Points screens meet WCAG 2.1 AA; status (Pending, Credited, Reversed) is shown with text and icon as well as colour; TalkBack reads each history row in full. 
- **R5-NFR5 (Privacy):** A student's points are visible only to that student and authorised admins; notification text on the lock screen does not show the event name or number of points (setting on by default). 
- **R5-NFR6 (Retention):** Credit and reversal entries are kept for the student's study period plus 6 years and are not deleted when an account is deactivated.
- **R5-NFR7 (Audit):** All credits, reversals, appeals, flags and denied change attempts are kept in the audit log for at least 2 years.

## R6 Non-functional requirements

- **R6-NFR1 (Performance):** Login, registration steps and password reset screens respond within 3 seconds on 4G on 95% of requests; the verification email is sent within 1 minute.
- **R6-NFR2 (Security):** Passwords are stored only as salted hashes; all traffic uses TLS 1.2 or higher; a session expires after 1 hour unused online (refresh up to 30 days); every backend request is checked for role and ownership, not only in the app. 
- **R6-NFR3 (Privacy):** Only the data needed is collected (email, name, student ID, study details, device); it is handled under the Personal Data Protection Act 2010 (Malaysia); users can ask what data is held about them.
- **R6-NFR4 (Accessibility and compatibility):** Login, registration and lock screens support TalkBack, are readable at 200% font size, meet WCAG 2.1 AA and run on Android 10 (API 29) and above.
- **R6-NFR5 (Reliability):** The daily study-period check runs every day with at least 99% success over a month and alerts an admin when it fails; account disabling takes effect for online users within 5 minutes. 
- **R6-NFR6 (Retention):** Deactivated accounts and their points records are kept as set in R5-NFR6; unverified accounts are deleted after 7 days; audit logs are kept for 2 years.
