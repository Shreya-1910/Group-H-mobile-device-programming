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



**What it does**
This is the entry point for staff, and it separates admins from student users. Admins sign in with their staff email and password, or with University SSO as a one-tap alternative. If a login fails, the password field turns red and an inline message explains the problem and how many attempts are left. A "Forgot password?" link offers recovery. The footer reminds admins that all actions are recorded in the audit log.

**User-friendly because**
- **Simplicity:** one task, two fields, one primary action.
- **Proximity:** each label sits directly above its field.
- **Clear error instructions:** the error says what went wrong and how many attempts are left.

---

### 2. Approval Queue
**Frame:** `R1-S1 · Approval queue · Default`


**What it does**
This is the admin's home screen, where clubs' submitted events are reviewed before students can book them. Summary cards show the workload at a glance (Pending, Needs review, Approved today). Filter chips narrow the list by request type. Each card shows the event details, the submitting club, the points and a status chip. Approve and Reject buttons sit directly under each card. Rejecting asks for a reason before confirming.

**User-friendly because**
- **Similarity:** every request uses the same card and colour-coded status chip.
- **Show status:** the counts at the top give an instant overview.
- **Proximity:** the actions sit directly under the item they affect.
- **Error prevention:** a reject confirmation with a required reason.

---

### 3. Accounts & Appeals
**Frame:** `R6-S2 · Account & appeals · Default`


**What it does**
This screen manages student accounts and the appeals they raise. Tabs switch between Accounts and Appeals, and the Appeals tab shows how many are pending. Each appeal card groups the student's details, their reason, a status chip and when it was submitted. The actions are specific to each case, such as "Grant +3 pts" or "Restore account". A "Recently resolved" section confirms completed decisions.

**User-friendly because**
- **Figure-ground:** white cards on a grey background separate each appeal.
- **Proximity:** the reason, status and decision are grouped in one card.
- **Consistency:** the same buttons and chips are used across the app.
- **Clear feedback:** a confirmation states exactly what will change.

---

### 4. Audit Log
**Frame:** `R3-S1 · Audit log · Default`


**What it does**
This is a read-only, time-ordered record of every admin and check-in action. Entries are grouped by day along a timeline, and each one shows who did what, when, and the outcome. Suspicious check-ins, such as a QR code scanned outside the event location, are flagged for review. Admins can search, filter by action type, and export the log as CSV.

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
