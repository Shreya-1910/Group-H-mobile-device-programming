
# Additional Features

## 1. Gamification & Milestone Badging

**Target Users:** Students

### 1.1 Function Description

The Gamification & Milestone Badging feature encourages students to participate actively in academic activities, community services, and other university events by rewarding their engagement and achievements.

### 1.2 Level and Title System

- Implement a tier-based progression system in which students earn points by participating in eligible activities.
- Automatically promote students to a higher level when their accumulated points reach the predefined threshold.
- Display each student's current level, title, total points, and points required to reach the next level on their personal centre page.

### 1.3 Achievement Badges

- Award exclusive badges when students complete specific activities or achieve predefined milestones.
- Support different badge categories, including academic participation, community service, leadership, and other eligible activities.
- Automatically unlock badges when the corresponding conditions are satisfied.
- Display earned badges on the student's personal centre page.

**Example Badges:**

- **Academic Pioneer:** Awarded for participating in three consecutive academic lectures.
- **Community Star:** Awarded for participating in eligible volunteer service activities.

### 1.4 Progress Visualisation

- Display visual statistics on the student's personal centre page.
- Show the total points earned and the remaining points required to reach the next level.
- Use radar charts to visualise abilities across different dimensions, such as leadership, social skills, and innovation.
- Use a chart library, such as MPAndroidChart, to implement progress charts and radar charts.

### 1.5 Expected Benefits

- Encourage active participation in university activities.
- Recognise student achievements through levels, titles, and badges.
- Help students track their progress towards personal milestones.
- Visualise student engagement and competency development.

---

## 2. Smart Reminders and Check-in Alerts

**Target Users:** Students

### 2.1 Function Description

The Smart Reminders and Check-in Alerts feature helps students remember their booked events, manage reservations, and complete check-in on time. It also reduces unused event places by allowing students to cancel bookings when they can no longer attend.

### 2.2 Event Reminders

- Schedule local notifications on the student's device for each confirmed event booking.
- Send a reminder 24 hours before the event starts.
- Send another reminder 3 hours before the event starts.
- Allow scheduled reminders to be triggered without an active internet connection, subject to operating system restrictions and notification permissions.
- Exclude email and SMS reminders from the scope of this feature.

### 2.3 Cancellation Through Reminders

- Include a **"Can't make it"** button in the 3-hour reminder notification.
- Allow students to initiate booking cancellation directly from the reminder.
- Once the cancellation is successfully processed, release the booking place to the next eligible student on the waiting list.
- Update the booking and waiting-list status accordingly.
- If the device is offline, queue the cancellation request or inform the student that the cancellation has not yet been confirmed.

### 2.4 Check-in Alerts

- Send a local notification when check-in opens for a booked event.
- Allow students to tap the notification to navigate directly to the relevant event check-in screen.
- Display relevant event information and the available check-in action.
- Ensure that check-in follows the event's configured check-in rules and availability period.

### 2.5 Expected Benefits

- Reduce the number of students who miss booked events.
- Improve student attendance and punctuality.
- Reduce wasted event places by making cancellations easier.
- Provide quick access to the event check-in screen.
- Improve the overall event booking and attendance management experience.

### 2.6 Scope and Limitations

- Reminders are scheduled locally on the student's device.
- Internet access is not required at the time a scheduled reminder is delivered, subject to device and operating system restrictions.
- Email and SMS notifications are not included.
- Booking cancellations and waiting-list updates may require an internet connection.
- Check-in alerts are triggered according to the event's configured check-in opening time.
