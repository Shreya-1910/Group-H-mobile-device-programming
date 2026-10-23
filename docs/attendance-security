# Attendance Security Analysis (R3, R4)

[← Back to README](../README.md) ]

Our design blocks remote, late, forged and replayed check-ins. Its honest limit is that a QR code proves that *a registered phone* saw the code in the room, not who is holding it. So cheating by people who are physically present is made difficult and auditable rather than impossible.

| # | Attack | Prevention | Remaining weakness | Stories |
|---|---|---|---|---|
| 1 | Screenshot of the code sent to an absent friend | Code rotates every 15 s and is valid for about 30 s | A live relay (e.g. a video call) within 30 s | [R3-S7](research.md#r3-secure-attendance), [R3-S12](research.md#r3-secure-attendance) |
| 2 | Checking in a friend on one phone | One registered device per account; one check-in per device per event; optional biometric confirmation | A student carrying a friend's own unlocked phone | [R3-S10](research.md#r3-secure-attendance), [R6-S8](research.md#r6-roles-and-access-control), [R4-S12](research.md#r4-offline-functionality) |
| 3 | Checking in before or after the event | Server checks the signed window counter, not the phone clock | Club device clock wrong and never synced (the club app syncs time when online) | [R3-S8](research.md#r3-secure-attendance), [R4-S7](research.md#r4-offline-functionality) |
| 4 | Changing the phone's date or time | Device time is recorded only as an anomaly signal | None significant | [R4-S7](research.md#r4-offline-functionality) |
| 5 | Forging a code | HMAC with a per-event secret readable only by the owning club | A club member leaking the secret (valid for one event only) | [R3-S9](research.md#r3-secure-attendance), [R6-S3](research.md#r6-roles-and-access-control) |
| 6 | Editing an offline record | Record signed with a non-exportable Keystore key and verified on the server | A rooted device misusing the key (needs specialist effort) | [R4-S6](research.md#r4-offline-functionality), [R4-S11](research.md#r4-offline-functionality) |
| 7 | Replaying or duplicating a record | Idempotent upload; one record per student and event | None significant | [R4-S5](research.md#r4-offline-functionality) |
| 8 | Modified app or direct API calls | Firestore security rules and Cloud Functions; the client is never trusted; rules have automated tests | A rule misconfiguration, caught by the rule tests | [R1-S7](research.md#r1-event-approval), [R2-S10](research.md#r2-marketplace-and-booking), [R6-S3](research.md#r6-roles-and-access-control) |
| 9 | Walk-in without a booking | Held as Needs review; no points until approved | Depends on the organiser reviewing honestly | [R3-S11](research.md#r3-secure-attendance), [R4-S10](research.md#r4-offline-functionality) |
| 10 | Club marking absent friends present | Manual marks need a reason and are audited; events over 20% manual are flagged | Collusion is made visible, not prevented | [R3-S6](research.md#r3-secure-attendance), [R3-S13](research.md#r3-secure-attendance) |
| 11 | Spoofed location | Mock location or distance only flags the record, never blocks it | Location is optional and spoofable, so it is a supporting signal only | [R3-S12](research.md#r3-secure-attendance) |
