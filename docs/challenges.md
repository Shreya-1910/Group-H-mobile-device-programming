# **R4: Online and offline**

**Risk 1: Offline check-ins are lost, stuck or uploaded twice**

**Why it matters:** A check-in saved without a connection could disappear after the app is closed, stay on syncing forever, or be counted twice after a retry.

**How we handle it:** Check-ins go into a local database before anything else and are uploaded in the background with retries. The server accepts each record only once. Every check-in shows a clear status, and a failed upload shows a retry button. We test offline, restart, retry and duplicate cases automatically.

**Risk 2: Offline records are faked or replayed**

**Why it matters:** Someone could edit a saved record, resend an old one, or change the phone clock to look like they were on time.

**How we handle it:** Each record is signed by the phone and checked by the server, which also rejects repeats. Validity depends on the club's changing code and not the phone clock. A friend inside the room can still relay a code within 15 seconds, and we admit this in the security analysis.

**Risk 3: The emulator cannot scan or locate**

**Why it matters:** The Pixel 2 emulator has no real camera view or GPS, so offline attendance is hard to demonstrate.

**How we handle it:** We use a physical Android phone for camera and GPS testing, while retaining the emulator for repeatable tests. Offline attendance is tested by disabling network connectivity, verifying that check-ins are stored locally, and confirming that they synchronise when connectivity is restored. A six-digit manual code is provided as an alternative check-in method.

# **R5: Points and Progress**
**Risk 4: Points are given twice or to the wrong person**

**Why it matters:** Running the crediting process more than once, or a late record arriving after processing, could create extra points.

**How we handle it:** Points are an append-only list with one credit per student per event. Running the process again changes nothing. Only server code can write to it.

**Risk 5: A credited record is found invalid later**

**Why it matters:** Removing points can confuse students and leave the history unclear.

**How we handle it:** We never delete an entry. A reversal entry with a reason is added, the student is notified, and they can appeal.

**Risk 6: Server code may need a paid backend**

**Why it matters:** Crediting and reversing points need server functions, which can need a paid Firebase plan.

**How we handle it:** We use the free allowance or the Firebase Emulator Suite for development and the demo, and document any difference in the report.

# **R6: Roles and Access**

**Risk 7: A user bypasses their role with a modified app**

**Why it matters:** Hiding screens does not stop a student from sending requests that only a club or admin should make.

**How we handle it:** Roles are enforced on the backend with account roles and security rules. We write tests to prove a student is denied club actions and one club cannot touch another club's events. Admin actions exist only in the backend.

**Risk 8: Account or phone sharing**

**Why it matters:** A student could lend their login or phone so a friend checks in for them.

**How we handle it:** Each account is tied to one registered phone and one phone to one account. Changing phone needs a re-registration step. The optional fingerprint or PIN check adds protection, but a student who hands over both phone and password cannot be stopped.

**Risk 9: Privacy and lost phones**

**Why it matters:** The app handles emails, location, login tokens and device keys, and Malaysian data protection rules apply.

**How we handle it:** We collect only what is needed, capture location only at check-in and store no face or fingerprint data. Tokens are stored securely, a lost or disabled account is signed out at the next refresh, and local data is removed on logout after warning about unsynced check-ins.