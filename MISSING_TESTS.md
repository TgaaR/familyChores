# familyChoreGame — Missing Test Coverage

## Purpose

This document tracks important behavior that is implemented or planned but is not yet adequately covered by automated tests.

The current suite contains 38 passing tests. It covers family onboarding, core member validation, week policies, assignment and right concurrency, completion versioning, weekly scoring, right spending and daily caps, the PostgreSQL baseline migration, representative school-calendar dates, API day classification, authentication/session basics, role enforcement, CSRF, login lockout, logging configuration, request correlation, safe malformed/unexpected-error responses, and several packaged-PWA contracts.

Items below should be checked off only after an automated test is added and passes through the normal project test commands.

## Priority 1 — Critical data and point safety

### Concurrent right redemptions

- [x] Two simultaneous redemptions cannot spend the same final points.
- [x] Two simultaneous redemptions cannot both consume the final daily-cap slot.
- [x] A right redemption racing with chore uncompletion cannot create a negative balance.
- [x] A member archive racing with right redemption produces a consistent result.

### Right request idempotency

- [x] Retrying the same request ID with the same payload returns the original redemption.
- [x] Reusing a request ID with a different member is rejected.
- [x] Reusing a request ID with a different right is rejected.
- [x] An idempotent retry does not fail merely because the daily cap was reached by the original request.

### Baseline migration

- [x] A fresh database receives the complete family, chore, assignment, right, and redemption schema.
- [x] Required baseline indexes and right availability/cap columns are created.
- [x] Restarting after the baseline is applied does not reapply it.
- [x] Baseline failure rolls back migration tracking and leaves pre-existing data readable.

## Priority 2 — Right availability and catalog behavior

### Availability rules

- [ ] A `WEEKEND` right is rejected on a regular Sunday–Thursday school day.
- [ ] A `WEEKEND` right is accepted on Friday.
- [ ] A `WEEKEND` right is accepted on Saturday.
- [ ] A `WEEKEND` right is accepted during an official school vacation.
- [ ] An `ALL_WEEK` right is accepted on a regular school day.
- [ ] The server, not the device clock, decides availability.
- [ ] Availability changes correctly at midnight in `Asia/Jerusalem`.
- [ ] Availability remains correct across daylight-saving transitions.

### Daily caps

- [ ] A daily cap applies separately to each member.
- [ ] A daily cap applies separately to each right.
- [ ] The cap resets on the next Jerusalem calendar date.
- [ ] Historical redemptions outside today's range do not count against today's cap.
- [ ] A cap greater than one permits exactly the configured number of redemptions.

### Right catalog loading

- [ ] Invalid field counts fail startup with the resource path and line number.
- [ ] Unknown availability values fail validation.
- [ ] Zero and negative costs fail validation.
- [ ] Zero and negative daily caps fail validation.
- [ ] Duplicate right IDs fail validation.
- [ ] Blank required fields fail validation.
- [ ] Updating a right synchronizes its names, cost, availability, and cap.
- [ ] Removing a right from the file marks it inactive.
- [ ] Catalog changes do not rewrite historical redemption names or costs.
- [ ] An empty right file is accepted and produces an empty UI state.

## Priority 3 — School calendar

### Calendar data

- [ ] Every configured vacation is tested at its first and last inclusive dates.
- [ ] Every return-to-school date is classified as a school day when it is not a weekend.
- [ ] Friday and Saturday are classified as non-school days outside vacation ranges.
- [ ] Summer vacation boundaries are tested.
- [ ] Dates outside the packaged school year have explicitly defined behavior.

### Calendar resource validation

- [ ] Invalid field counts fail with a useful error.
- [ ] Invalid ISO dates fail with the resource line number.
- [ ] A range whose end precedes its start fails validation.
- [ ] Duplicate vacation IDs fail validation.
- [ ] Overlapping vacation ranges are either rejected or explicitly supported and tested.
- [ ] A missing calendar resource fails startup clearly.

### Future calendar variants

- [ ] Elementary/kindergarten and secondary-school summer boundaries are independently supported if both are introduced.
- [ ] A future parent-created vacation override takes precedence correctly.
- [ ] Annual calendar replacement does not alter historical day labels unexpectedly.

## Priority 4 — API validation and error contracts

### Family onboarding

- [ ] Blank and oversized family names are rejected.
- [ ] Unsupported default languages are rejected.
- [ ] An empty initial-member list is rejected.
- [ ] Blank and oversized initial-member names are rejected.
- [ ] Invalid member characters are rejected during onboarding.
- [ ] A failure in any initial member rolls back the complete family transaction.

### Members, chores, and assignments

- [ ] Adding a member before family setup returns `family_setup_required`.
- [ ] Archiving a missing member returns `member_not_found`.
- [ ] Assigning an unknown member returns `member_not_found`.
- [ ] Assigning an archived member returns `member_not_found`.
- [ ] Assigning an unknown or inactive chore returns `chore_not_found`.
- [ ] Completing a missing assignment returns `assignment_not_found`.
- [x] Malformed path dates and malformed JSON return stable structured errors.

### Rights

- [ ] Redeeming for an unknown or archived member returns `member_not_found`.
- [ ] Redeeming an unknown or inactive right returns `right_not_found`.
- [ ] Insufficient points returns `insufficient_points` without creating a row.
- [ ] Unavailable-day rejection returns `right_not_available_today`.
- [ ] Daily-cap rejection returns `daily_cap_reached`.

### Error handling

- [ ] Validation failures consistently return `validation_failed`.
- [ ] Known uniqueness violations return the correct domain-specific conflict.
- [ ] Unexpected database failures are not mislabeled as assignment conflicts.
- [ ] Error responses never expose SQL, filesystem paths, or sensitive request data.

## Priority 5 — Browser and PWA flows

### Authentication and authorization

- [x] Unauthenticated access to family data is rejected.
- [x] PINs are BCrypt hashes and are not stored in plain text.
- [x] Invalid PIN login is rejected and a valid login persists through the session.
- [x] A child cannot call a parent-only member-management endpoint.
- [x] A parent can call the same protected endpoint.
- [x] Logout invalidates the session.
- [x] A child cannot complete another member's chore or redeem another member's right.
- [ ] Every parent-only endpoint has a crafted-request authorization test.
- [ ] Archived members cannot log in.
- [x] PIN retry throttling and temporary lockout are tested.
- [x] CSRF invalid-token rejection and the packaged PWA token-header contract are tested.
- [ ] Browser tests cover login, logout, role-specific controls, and session expiry.

The current frontend tests inspect packaged source text; they do not execute the UI in a browser. Add Playwright or an equivalent browser-test setup before completing this section.

### Onboarding and language

- [ ] A fresh browser displays the non-dismissible family setup dialog.
- [ ] Valid setup opens the board and uses the selected default language.
- [ ] Invalid and duplicate member names display bilingual errors.
- [ ] Adding and removing initial-member rows works.
- [ ] Switching language updates text and changes RTL/LTR direction.
- [ ] The selected language persists after reload.

### Week navigation and board

- [ ] Previous, next, and current-week navigation load the correct dates.
- [ ] Current and coming weeks expose the correct controls.
- [ ] Past and distant-future weeks remain read-only.
- [ ] Member filtering changes visible task cards without changing scores.
- [ ] School days render light green.
- [ ] Weekends and vacations render light blue.
- [ ] Bilingual day-type and vacation labels render correctly.
- [ ] The board remains usable on narrow mobile screens.

### Add Chore dialog

- [ ] Selecting a member reveals available chores.
- [ ] Busy members and already-used chores are disabled.
- [ ] Submitting creates the expected task card.
- [ ] Periodic refresh does not clear the selected member or chore.
- [ ] Save conflicts preserve a useful error and reload current data.
- [ ] Closing and reopening the dialog resets only the intended fields.

### Member management

- [ ] Adding a member updates selectors and score cards.
- [ ] Archiving requires confirmation.
- [ ] Archiving the currently filtered member resets the filter safely.
- [ ] Archived-member history remains visible on historical boards.

### Rights UI

- [ ] Changing the selected member refreshes balance and available rights.
- [ ] Unaffordable rights are hidden.
- [ ] Capped-out rights are hidden.
- [ ] Weekend rights appear on weekends and school vacations only.
- [ ] Cost, availability, and daily usage are rendered correctly.
- [ ] Confirmed redemption updates balance, count, value, and available choices.
- [ ] Cancelling the confirmation does not redeem or spend points.
- [ ] Server errors produce the correct bilingual message.

## Priority 6 — Service worker, installation, and offline behavior

- [ ] Initial installation caches every declared application-shell asset.
- [ ] API requests are never placed in the service-worker cache.
- [ ] Application assets use network-first behavior.
- [ ] Cached assets load when the server is unavailable.
- [ ] Missing cached assets fall back appropriately.
- [ ] Changing the cache version removes obsolete caches.
- [ ] A new release becomes visible after the expected reload/update flow.
- [ ] Mutations are disabled while offline.
- [ ] Reconnection refreshes current shared data.
- [ ] The custom install button follows `beforeinstallprompt` correctly.

## Priority 7 — Database configuration and repository behavior

- [x] Integration tests execute against a real PostgreSQL 16 instance.
- [ ] Production PostgreSQL connection-pool limits and timeouts are configured and tested.
- [ ] RDS SSL verification is configured and tested for the AWS profile.
- [ ] Right balance queries include every completed assignment and redemption exactly once.
- [ ] Archived members retain balances and historical redemption records.
- [ ] Catalog deactivation does not break referenced assignments or redemptions.
- [ ] Repository mapping covers nullable timestamps correctly.

## Priority 8 — Accessibility and visual quality

- [ ] All dialogs trap and restore focus correctly.
- [ ] Every interactive control is keyboard accessible.
- [ ] Buttons and controls have useful accessible names.
- [ ] Dynamic status and error messages are announced appropriately.
- [ ] Hebrew and English layouts do not clip at supported viewport sizes.
- [ ] Light green and light blue day colors meet contrast requirements with their text.
- [ ] Day type is understandable without relying on color alone.
- [ ] Reduced-motion preferences are respected if animations are added.

## Suggested implementation order

1. Concurrent redemption and baseline-migration tests.
2. Weekend/vacation right-availability boundary tests.
3. Right and calendar resource validation tests.
4. Playwright setup and core onboarding/chore/right browser flows.
5. Service-worker/offline tests.
6. Accessibility and responsive visual regression tests.

## Test commands

Current Java/backend suite:

```bash
./mvnw test
```

JavaScript syntax check:

```bash
node --check app.js
```

A browser-test command should be documented here once a browser framework is added.
