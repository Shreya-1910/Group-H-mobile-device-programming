# Wireframes 

## Ziad - Student discovery & booking 

**Owner:** Ziad 
**Figma:** [Admin Tool page](placeholder figma link)
**Frame size:** Pixel 2 (411 × 731 dp)

## Shuming - Student attendance & progress  

**Figma:** ([Student attendance & progress](https://www.figma.com/design/6z2DTyn9ipP83E4EA2nGPJ/MDP?node-id=77-502&t=LcMUeLHuAlxcqu2p-4))
**Frame size:** Pixel 2 (411 × 731 dp)

### 1. Check-in scanner

<img width="272" height="485" alt="image" src="https://github.com/user-attachments/assets/b4f6a06e-3244-45b7-898c-da47791ad568" />

This page allows students to check in to an event by scanning a QR code. They can also see the event name, date, time, location, and approval status. The system checks whether the QR code is valid, expired, or has already been used. If the student is offline, the check-in record will be saved locally and synced when the internet connection is back. The simple layout helps students complete check-in quickly and easily. 


### 2. Check-in result

<img width="272" height="483" alt="image" src="https://github.com/user-attachments/assets/257c8020-ec3c-4a03-96c0-4f649909874c" />

This page shows students whether their check-in was successful. It displays the event details, check-in time, attendance status, and points earned. Students can immediately confirm that their attendance has been recorded. If something goes wrong, the system should explain the reason and tell them what to do next. There are also buttons to view their progress or return to the check-in page, making the process simple and clear. 


### 3. Points/Progress

<img width="270" height="482" alt="image" src="https://github.com/user-attachments/assets/1ae83e7a-3e0a-40a4-8936-16405a221c0f" />

This page helps students track their participation and progress in club activities. It shows their total points, membership level, points needed to reach the next level, number of events attended, and attendance rate. Students can also view their recent activity and see how many points they earned from each event. Their points are only visible to themselves and authorised admins. The system also prevents points from being added twice or accidentally lost. 

### 4. Profile/Notification

<img width="272" height="487" alt="image" src="https://github.com/user-attachments/assets/7a0dd4a2-8a9b-443e-87e6-78dae4e0dac1" />

This page allows students to view their personal information, student ID, email address, and club membership. It also keeps all important notifications in one place, such as upcoming event reminders and points updates. Students can manage their notification preferences and access their account settings. To protect their privacy, notifications on the lock screen won't show event names or point amounts by default. Personal information is also protected so that only authorised users can access it. 



## Shreya - Club organiser   
### 1. Student Login


  <img src="images/login%20page.png" alt="Student Login" width="200">
  <img src="images/invalid%20login.png" alt="Invalid Login" width="200">
   <img src="images/locked%20page.png" alt="Locked Page" width="200">
 

**What it does**

This is the entry point for students, allowing them to sign in using their university email and password before accessing their respective screens. Inline validation highlights invalid inputs, while a generic “The email or password is incorrect” message prevents the system from revealing whether an account exists.After consecutive failed login attempts, the account is temporarily locked. The screen also supports offline error handling, displaying a message directing students to contact the office.

**User-friendly because**
- **Simplicity:** The screen focuses on one task, with clearly labelled fields and a primary sign-in action.
- **Proximity:** Labels and validation messages appear close to their respective fields, making errors easier to identify and correct.
- **Clear error feedback:** Specific input errors help students correct mistakes, while the generic authentication message protects account privacy.
- **Error recovery:** The offline message provides a clear next step when students cannot sign in due to connectivity issues.
- **Security feedback:** The temporary lockout communicates the consequence of repeated failed attempts, helping protect student accounts.


### 2. Home Page
<img src="images/homepage.png" alt="Student Homepage" width="250">

**What it does**

The home page provides quick access to key features through clearly labelled action buttons and a bottom navigation bar. Related functions are grouped together, applying the Gestalt principle of proximity to improve visual organisation and usability. The calendar and colour-coded attendance statuses allow students to identify upcoming events and track their progress at a glance.

**User-friendly because**
- **Clear navigation:** Clearly labelled action buttons and a consistent bottom navigation bar help students find features easily.
- **Proximity:** Grouping related functions together makes the interface easier to understand and reduces the effort required to locate information.
- **Visual hierarchy:** The calendar and attendance statuses make important information easy to identify at a glance.
- **Colour coding:** Different attendance statuses help students distinguish between event states quickly without having to read lengthy descriptions.
- **Consistency:** Reusing familiar navigation elements and visual patterns across screens makes the application easier to learn and use.

### 3. Events detail
<img src="images/event%20details.png" alt="Event Details" width="250">

**What it does**

The event details page clearly presents key information, including the date, time, location, available places, and points awarded. Grouped details, distinct headings, and consistent colours improve readability, while simple navigation makes the page easy to use.

**User-friendly because**
- **Visual hierarchy:** Distinct headings and organised information help students identify important event details quickly.
- **Consistency:** Consistent colours and layout make the interface predictable and easier to navigate.
- **Reduced cognitive load:** Presenting all essential information on one screen minimises the need to navigate between pages to make decisions.
- **Clear information:** Displaying available places and points upfront helps students decide whether to attend an event.

### 4. My Events
<img src="images/my%20events.png" alt="My Events" width="250">

**What it does**

The My Events page displays upcoming and past events with clear details, including dates, locations, points, and booking statuses. Distinct cards, colour-coded labels, and tabs help users quickly identify information and navigate the page easily.

**User-friendly because**
- **Visual organisation:** Separate event cards and colour-coded labels make information easier to scan and distinguish.
- **Easy navigation:** Tabs allow students to switch between upcoming and past events without searching through a single long list.
- **Consistency:** A consistent layout helps students locate event details and booking statuses quickly.
- **Reduced cognitive load:** Presenting key information at a glance minimises the effort needed to track event participation.


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
