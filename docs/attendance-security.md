# Attendance Security Analysis (R3, R4)

[← Back to README](../README.md)

Our design blocks remote, late, forged and replayed check-ins. It has two limits:
- **Who is holding the phone:** a QR code proves that *a registered phone* saw a genuine code, not who is holding it.
- **When an offline scan happened:** the server can't confirm the time of an offline scan.

Cheating by people who are present, or by a code relayed live, is therefore made difficult and auditable rather than impossible. Attendance statuses follow the single state diagram in [R3](research.md#r3-secure-attendance).

| # | Attack | Prevention | Remaining weakness | Stories |
|---|---|---|---|---|
| 1 | Screenshot of the code sent to an absent friend who is **online** | The code changes every 15 s; only the current and previous codes are accepted at server time (valid ≤ 30 s) | A code relayed live (e.g. by video call) within 30 s | [R3-S7](research.md#r3-secure-attendance), [R4-S15](research.md#r4-online-and-offline) |
| 2 | Screenshot forwarded live and scanned **offline** by an absent student | The counter must be inside the check-in window and in the club device's session log; a record not in the log becomes Needs review | **Not caught by the code checks**, because the counter is genuine and the scan time can't be trusted offline. It is caught only by the location flag, or when scans for a display exceed the headcount | [R3-S7](research.md#r3-secure-attendance), [R4-S15](research.md#r4-online-and-offline), [R4-S23](research.md#r4-online-and-offline) |
| 3 | Checking in a friend on one phone | One registered device per account (Keystore key); one check-in per device per event; optional biometric confirmation | A student carrying a friend's own unlocked phone | [R3-S10](research.md#r3-secure-attendance), [R6-S13](research.md#r6-roles-and-access), [R6-S22](research.md#r6-roles-and-access), [R4-S12](research.md#r4-online-and-offline) |
| 4 | Checking in before or after the event | The server checks the signed window counter against the window (event start to 15 min after the end), not the phone clock | Club phone clock wrong while offline; tolerated up to 5 min, otherwise the record becomes Needs review | [R3-S8](research.md#r3-secure-attendance), [R4-S16](research.md#r4-online-and-offline), [R4-S19](research.md#r4-online-and-offline) |
| 5 | Changing the phone's date or time | Phone time is recorded only as an anomaly signal; a gap of more than 24 h marks the record Needs review | None significant | [R4-S16](research.md#r4-online-and-offline) |
| 6 | Forging a code | HMAC with a per-event secret readable only by the owning club; the secret expires after the window | A club member leaking the secret (valid for one event only) | [R3-S9](research.md#r3-secure-attendance), [R6-S7](research.md#r6-roles-and-access) |
| 7 | Guessing the 6-digit code | 1 in 1,000,000 chance per try; entry locks for 60 s after 5 wrong codes; codes change every 15 s | None significant | [R3-S3](research.md#r3-secure-attendance), [R4-S6](research.md#r4-online-and-offline) |
| 8 | Editing an offline record | Record signed with a non-exportable Keystore key and verified on the server; tampering flags the account | A rooted device misusing the key (needs specialist effort) | [R4-S17](research.md#r4-online-and-offline), [R4-S20](research.md#r4-online-and-offline) |
| 9 | Replaying or duplicating a record | Idempotent upload; one record per student and event | None significant | [R4-S17](research.md#r4-online-and-offline) |
| 10 | Modified app or direct API calls | Firestore security rules and Cloud Functions; the client is never trusted; rules have automated tests | A rule misconfiguration, caught by the rule tests | [R1-S7](research.md#r1-event-approval), [R2-S10](research.md#r2-marketplace-and-booking), [R5-S9](research.md#r5-points-and-progress), [R6-S7](research.md#r6-roles-and-access) |
| 11 | Scanning at the start, then leaving | The organiser shows the code at chosen times, and each display is logged; check-out is optional | A student who leaves after the last display | [R3-S14](research.md#r3-secure-attendance), [R4-S23](research.md#r4-online-and-offline) |
| 12 | Walk-in without a booking | Held as Needs review; no points until the organiser approves | Depends on the organiser reviewing honestly; appeals go to the s-CPD admin | [R3-S11](research.md#r3-secure-attendance), [R4-S7](research.md#r4-online-and-offline), [R4-S18](research.md#r4-online-and-offline) |
| 13 | Club marking absent friends present | Manual marks need a reason and are audit-logged; events with more than 20% manual marks are flagged to the s-CPD admin | Collusion is made visible, not prevented | [R3-S6](research.md#r3-secure-attendance), [R3-S13](research.md#r3-secure-attendance), [R6-S20](research.md#r6-roles-and-access) |
| 14 | Spoofed location | Mock location or distance only flags the record, never blocks it | Location is optional and spoofable, so it is a supporting signal only | [R3-S12](research.md#r3-secure-attendance), [R4-S15](research.md#r4-online-and-offline) |
