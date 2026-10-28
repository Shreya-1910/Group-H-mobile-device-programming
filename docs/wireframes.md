# Wireframes 

## Ziad - Student discovery & booking 

**Owner:** Ziad 
**Figma:** [Admin Tool page](placeholder figma link)
**Frame size:** Pixel 2 (411 × 731 dp)

## Mark - Student attendance & progress  

**Figma:** [Admin Tool page](placeholder figma link)
**Frame size:** Pixel 2 (411 × 731 dp)

## Shreya - Club organiser   

**Figma:** [Admin Tool page](placeholder figma link)
**Frame size:** Pixel 2 (411 × 731 dp)

## Sanjanaah — Admin Tool

**Figma:** [Admin Tool page](https://www.figma.com/design/6z2DTyn9ipP83E4EA2nGPJ/MDP?node-id=113-359&t=fKFFFji1polv8yy0-4)
**Frame size:** Pixel 2 (411 × 731 dp)

The Admin Tool lets staff sign in securely, review club event submissions, resolve student appeals, and inspect a read-only record of every admin and check-in action.

### Admin journey

`Admin Login → Approval Queue → Accounts & Appeals → Audit Log`

---

### 1. Admin Login
**Frame:** `R6-S1 · Admin login · Error`
<img src="images/admin-login.png" alt="Admin Login" width="200">

**What it does**
This is the entry point for staff, and it separates admins from student users. Admins sign in with their staff email and password, or with University SSO as a one-tap alternative. If a login fails, the password field turns red and an inline message explains the problem and how many attempts are left (“Incorrect email or password. 2 attempts left.”), so the admin knows what went wrong before being locked out. A “Forgot password?” link offers recovery. The footer reminds admins that every action is recorded in the audit log, which makes the screen’s purpose and level of accountability clear from the start. 

**User-friendly because**
- **Simplicity:** one task, two fields, one primary action.
- **Proximity:** each label sits directly above its field.
- **Clear error instructions:** the error says what went wrong and how many attempts are left.

---

### 2. Approval Queue
**Frame:** `R1-S1 · Approval queue · Default`
<img src="images/approval-queue.png" alt="Approval Queue" width="200">

**What it does**
This is the admin’s home screen, where clubs’ submitted events are reviewed before students can book them. Three summary cards at the top show the workload at a glance: 8 pending, 3 needing review, and 21 approved today. Each count is colour-coded to match its status. Filter chips narrow the list to Events, Club requests or Points claims. Each request card shows the event name, the submitting club, the date, time, venue and points, plus a status chip. Approve and Reject buttons sit directly under the card they act on. Flagged items explain why they need attention, such as “Venue clash with another event.” Rejecting opens a confirmation sheet that asks for a reason, which prevents accidental rejections and gives the club feedback. 

**User-friendly because**
- **Similarity:** every request uses the same card and colour-coded status chip.
- **Show status:** the counts at the top give an instant overview.
- **Proximity:** the actions sit directly under the item they affect.
- **Error prevention:** a reject confirmation with a required reason.

---

### 3. Accounts & Appeals
**Frame:** `R6-S2 · Accounts & appeals · Default`
<img src="images/accounts-appeals.png" alt="Accounts & Appeals" width="200">

**What it does**
This screen handles student accounts and the appeals they raise. Tabs switch between all accounts and pending appeals, and the Appeals tab shows its count, “(2)”. Each appeal card groups the student’s initials, name, ID and club with their reason, a status chip and when it was submitted. The actions are specific to each case: “Grant +3 pts” restores missed attendance points, and “Restore account” lifts a suspension. Every card also has a “Deny” option. Granting opens a confirmation explaining exactly what will change. A “Recently resolved” section confirms completed actions, so admins can see their decisions took effect. 

**User-friendly because**
- **Figure-ground:** white cards on a grey background separate each appeal.
- **Proximity:** the reason, status and decision are grouped in one card.
- **Consistency:** the same buttons and chips are used across the app.
- **Clear feedback:** a confirmation states exactly what will change.

---

### 4. Audit Log
**Frame:** `R3-S1 · Audit log · Default`
<img src="images/audit-log.png" alt="Audit Log" width="200">

**What it does**
This is a read-only, time-ordered record of every admin and check-in action, supporting the app’s secure attendance requirement. Entries are grouped by day and linked by a vertical timeline. Each entry states who did what, when, and the outcome as a colour-coded chip. Suspicious activity, such as a QR code scanned outside the event location, is flagged for review. Admins can search, filter by action type, and export the log as CSV for reporting. Because nothing here can be edited, admins and markers can trust the records, which keeps approvals, appeals and attendance transparent. 

**User-friendly because**
- **Continuity:** the timeline leads the eye through events in order.
- **Proximity:** entries are grouped by day.
- **Similarity:** the same status chips are used as elsewhere in the app.
- **Show status:** who, what and when are visible for every action, which builds trust in attendance records.

--- 
### Prototype flow
- **Sign in** → Approval Queue
- **Reject** → reason bottom sheet → back to the queue
- **Bottom nav** → Queue / Accounts / Audit log
- **Grant appeal** → confirmation sheet → Audit Log
- **Filter icon** → filter sheet
