# Design System

**Owner:** Ziad

**Figma:** [Design System](https://www.figma.com/design/6z2DTyn9ipP83E4EA2nGPJ/MDP?node-id=194-1302&t=WtO7yMKTm3fSBXUz-4)

**Frame size:** Pixel 2 (411 × 731 dp)

**Font:** Roboto (Android default)

Every screen in the app is built from one shared set of colours, text styles and components. This keeps the student, club and admin screens consistent, so they look and behave like one app. Teammates copy components from the Design System page instead of drawing their own, and changing a main component updates every screen that uses it.

---

## 1. Colours

<img src="/docs/images/Colours.png" width="500">

The palette is deliberately small: one brand colour, neutral colours for text and backgrounds, and four fixed status colours. Each status colour always means the same thing on every screen, so users only need to learn it once.

| Style | Hex | Used for |
|---|---|---|
| Primary | `#4E32B0` | Main buttons, active navigation, links, points badge |
| Text | `#1F1F1F` | Main text |
| Text Secondary | `#6B6B6B` | Dates, labels, placeholders |
| Border | `#E0E0E0` | Card and field outlines, disabled buttons |
| Surface | `#F5F5F5` | Screen background, secondary badges |
| Status / Verified | `#2E7D32` | Approved, Verified, Checked in, Credited |
| Status / Pending | `#B45309` | Pending, Syncing |
| Status / Rejected | `#C62828` | Rejected, Reversed, errors, destructive actions |
| Status / Needs review | `#1565C0` | Needs review |

**Accessibility:** status is never shown by colour alone. Every status chip combines colour, icon and word (e.g. ✓ Verified), so it remains clear for colour-blind users.

---

## 2. Typography

<img src="/docs/images/Typography.png" width="500">

Five text styles in Roboto, Android's default font, create a clear visual hierarchy. The size steps guide the eye from the most important information (points, screen titles) down to smaller details (dates, chip labels).

| Style | Size | Weight | Used for |
|---|---|---|---|
| Big number | 32 | Bold | Points total, attendance count |
| Headline | 24 | Bold | Screen title (once per screen) |
| Title | 18 | SemiBold | Event names, section headers |
| Body | 14 | Regular | Descriptions, field values |
| Label | 12 | Medium | Dates, chips, small details |

**Consistency:** text colours and fonts are always picked from the saved styles, never typed in by hand, so a single change updates the whole app.

---

## 3. Components

<img src="/docs/images/Components.png" width="700">

Reusable building blocks used across every screen. Components with several versions are built as **variants**, so a teammate can drop one in and switch its type from a dropdown (e.g. Button → Primary / Secondary).

**Actions**

- **Button:** Primary · Secondary · Destructive · Disabled
- **Floating action button:** "+ Create Event" on club screens
- **Text link:** "Forgot password?", "View list ›"

**Status and feedback**

- **Status chip:** Verified · Pending · Rejected · Needs review · Syncing · Draft · Cancelled · Expired · Checked in · Manual · Not yet · Present · Credited · Reversed
- **Info banner and offline banner:** hints and offline notices
- **Empty and error state:** Empty · Error (with Retry)
- **Dialog and bottom sheet:** confirmations and filters

**Inputs and navigation**

- **Text field:** Default · Error
- **Search bar:** searching events, attendees and accounts
- **Filter chip:** Selected · Unselected
- **Tabs:** Selected · Unselected
- **Top app bar:** Default · Back
- **Bottom navigation:** Club (5 variants) · Admin (4 variants)

**Content**

- **Event card:** Approved · Pending · Rejected · Needs review
- **Cards:** Stat tile · Points progress card · Details card
- **List rows:** Attendee · Settings · Activity · Audit entry
- **Small elements:** Avatar · Progress bar · Notification item · Android status bar
