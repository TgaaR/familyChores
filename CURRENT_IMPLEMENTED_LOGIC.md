# familyChoreGame — Currently Implemented Logic

## 1. Scope and architecture

This document describes the application as it works now. Future requirements remain in `PROJECT_PLAN.md`.

The application uses Java 21, Spring Boot, Maven, Jdbi, and PostgreSQL. Spring Boot serves both the existing bilingual PWA and a versioned `/api/v1` JSON API.

- PostgreSQL is the shared source of truth. Local development uses the PostgreSQL 16 service in `compose.yaml`.
- Database changes are applied through ordered SQL migrations.
- Jdbi provides repository access and transaction boundaries.
- The service binds to `127.0.0.1` by default.
- `/actuator/health` provides a local health check.
- The PWA stores only language, displayed-week, and member-filter preferences in `localStorage` under `familyChoreGame.preferences.v2`.
- Old prototype data under `familyChoreGame.v1` is ignored except for a one-time preference fallback.

Member/PIN authentication and parent/child authorization are implemented. SSE push updates, local-data import, automated backups, right expiration/carry-over, approval workflows, refunds, and manually managed school-vacation overrides are not implemented.

## 2. Family onboarding and members

A fresh database contains no placeholder family. Before the board opens, a required bilingual setup window collects the family name, default language (`he` or `en`), and one or more initial members. Every member receives a parent or child role and a 4–6 digit PIN; at least one parent is required.

`POST /api/v1/families` creates the single family and its initial members in one transaction. A second family is rejected.

Member names are trimmed, have repeated whitespace collapsed, may contain Unicode letters and combining marks with single spaces between words, are limited to 24 characters, and must be unique after Unicode normalization and case folding.

Additional members can be added from the Family Members dialog. Removing a member archives the member instead of deleting historical assignments. Archived members no longer appear in selection controls but remain available for historical display.

PINs are stored only as BCrypt hashes. After setup, the user selects an active member and enters that member's PIN. A successful login creates an HTTP-only, SameSite Strict server session with a 12-hour timeout. Five failed attempts lock that member for 15 minutes; the shared PostgreSQL table makes the lock effective across application instances. Parent sessions can manage members and schedules and act for any member. Child sessions can complete only their own chores and redeem only their own rights. The PWA also hides or disables unavailable actions.

Spring Security CSRF protection applies to state-changing requests. `GET /api/v1/session` issues the `XSRF-TOKEN` cookie and the PWA returns it in the `X-XSRF-TOKEN` header. Login success, login failure, lockout, blocked login, and logout emit structured security events using stable member IDs without PINs or member names.

## 3. Language and layout

- Hebrew is the default unless onboarding chooses English.
- Hebrew uses `he-IL` and RTL layout; English uses `en-US` and LTR layout.
- Switching language updates UI labels, dates, chore/category/right names, and layout direction.
- UI translations live in the `TEXT` object in `app.js`.
- Catalog entries store separate Hebrew and English names.

## 4. Chore catalog

Categories and chores are loaded from `src/main/resources/catalog/choresAndScores.txt`.

```text
CATEGORY|id|Hebrew name|English name
CHORE|id|category id|Hebrew name|English name|points
```

At startup, the loader validates record types, fields, numeric values, unique IDs, and category references, then synchronizes the catalog into PostgreSQL. Categories and chores are returned alphabetically by English name, matching rights. Removed catalog entries become inactive rather than deleting referenced history. Chore points are copied into assignment `points_snapshot` values, so catalog changes do not rewrite historical scores.

## 5. Week rules

The server uses `Asia/Jerusalem`. A game week begins Sunday; chore days are Sunday through Friday. Saturday is displayed as a rest day and cannot receive assignments.

| Displayed week | Create/delete assignments | Complete/uncomplete |
|---|---:|---:|
| Current week | Yes | Yes |
| Coming week | Yes | No |
| Past week | No | No |
| More than one week ahead | No | No |

The UI and API enforce these rules. Other weeks remain available for read-only browsing.

The server classifies dates from the packaged 2026–2027 Jewish state-school calendar. Regular school days appear light green; Friday/Saturday weekends and official school vacations appear light blue. Each day header displays its bilingual school-day, weekend, or vacation label.

## 6. Assignment flow and constraints

To add a chore, the user chooses a day, member, and chore, then submits the dialog.

- Members already assigned that day are disabled.
- Chores already used that day are disabled.
- Refreshes do not reset the member selected in an open Add Chore dialog.
- Assignment creation uses a request ID for retry idempotency.
- PostgreSQL enforces one chore per member/date, one use of a chore/date, and one assignment per request ID.
- Wallet-changing operations lock the affected member row so concurrent redemption, uncompletion, and archival cannot overspend points or exceed a daily cap.

Completion is allowed only for current-week assignments. It uses an optimistic `version`; stale changes return HTTP `409 Conflict` and the PWA reloads current data.

## 7. Weekly scoring and member cards

For the displayed week, completed count is the number of completed assignments, weekly score is the sum of their point snapshots, and progress is completed assignment count divided by assignment count.

Each active member card combines weekly progress with the all-time point wallet:

- Large point value: current spendable balance.
- Total chore points: points earned from all completed chores.
- Rights redeemed: redemption count and combined point cost.
- Completion count and progress bar: the week currently displayed.

Redeeming rights does not change historical weekly scores; it reduces only the spendable balance.

## 8. Rights and redemptions

Rights are loaded from `src/main/resources/catalog/rights.txt`.

```text
id|Hebrew name|English name|point cost|availability|daily cap
```

- IDs must be unique and should remain stable after use.
- Point cost and daily cap must be positive integers.
- Availability is `ALL_WEEK` or `WEEKEND`.
- `WEEKEND` currently means Friday, Saturday, or a date in the packaged school-vacation calendar.
- The daily cap applies separately to each member and right.
- Rights are displayed alphabetically in the active language.

The Rights section lets the user select a member. It displays only rights available on the server's current Jerusalem date, below that member's daily cap, and affordable with that member's balance. Each right shows its cost, availability, and today's usage.

`POST /api/v1/right-redemptions` atomically validates the balance and daily cap and stores member/right IDs, bilingual name snapshots, cost snapshot, request ID, and UTC timestamp. Retries do not spend twice. A completed chore cannot be returned to incomplete when doing so would make the wallet negative.

The official 2026–2027 vacation calendar is implemented as a packaged resource and makes `WEEKEND` rights available on those dates. Manual parent-created date/range overrides and automatic annual imports are not implemented.

## 9. API and synchronization

Implemented endpoints:

- `GET /api/v1/session`
- `GET /api/v1/login-members`
- `POST /api/v1/login`
- `POST /api/v1/logout`
- `GET /api/v1/bootstrap`
- `POST /api/v1/families`
- `GET /api/v1/weeks/{weekStart}`
- `POST /api/v1/members`
- `DELETE /api/v1/members/{id}`
- `POST /api/v1/assignments`
- `DELETE /api/v1/assignments/{id}`
- `PATCH /api/v1/assignments/{id}/completion`
- `POST /api/v1/right-redemptions`

The PWA refreshes after mutations, when connectivity returns, when the document becomes visible, and every 15 seconds while visible. The sync banner shows loading, connected, and disconnected states. Writes are disabled when the API is unavailable. SSE is not implemented, so other devices normally converge on the next refresh.

## 10. PWA and offline behavior

- The service worker never caches `/api/` requests.
- Application assets use network-first behavior with cached fallback.
- Offline mode can show the cached application shell, but shared-data writes are disabled.
- The app remains installable through supported Chromium-based browsers.
- Responsive layouts support desktop and narrow mobile screens.

## 11. Logging and diagnostics

- Local execution uses human-readable console logs with structured key/value fields.
- The `aws` profile emits ECS JSON to standard output for CloudWatch-compatible ingestion.
- Every `/api/` request has an `X-Request-ID`; safe caller-provided IDs are preserved and other values are replaced with a UUID.
- The request ID is included in the response header, structured log context, and API error body.
- Request completion logs contain method, path, status, and duration.
- Mutations, migrations, and catalog synchronization emit structured business/lifecycle events using stable IDs and counts.
- Request bodies, family/member names, cookies, secrets, and SQL parameters are not logged.
- Unexpected errors are logged with stack traces but return only `internal_error`, a generic message, and the request ID.

## 12. Testing coverage

The automated suite currently contains 38 passing tests and covers:

- Family onboarding and duplicate-family rejection
- Fresh baseline migration, repeat startup, migration-failure rollback, resource catalog loading, and packaged resources
- Jerusalem week boundaries and mutation permissions
- Member validation, normalized uniqueness, and archival
- Assignment idempotency, uniqueness, concurrency, and point snapshots
- Completion/version conflicts and score changes
- Right deduction, insufficient funds, payload-safe retry idempotency, and daily caps
- Concurrent redemption protection for final points and final daily-cap slots
- Consistent redemption races with chore uncompletion and member archival
- Protection against reversing already-spent points
- The open Add Chore dialog refresh regression
- School-day/vacation CSS class rendering and light green/light blue color mappings
- PWA and service-worker contracts
- AWS/local logging configuration, request correlation, and safe unexpected-error responses
- PIN hashing, invalid and valid login, session persistence, logout, unauthenticated API rejection, and parent/child endpoint authorization
- CSRF token issuance/rejection and database-backed PIN retry lockout

## 13. Current limitations

1. PIN recovery/change is not yet implemented. Authentication events are structured logs rather than a durable audit-history table.
2. The packaged calendar covers Jewish state schools for 2026–2027 only; there is no annual import or manual override UI.
3. Weekend days are fixed to Friday and Saturday.
4. Points are an all-time earned-minus-spent balance; expiration and two-point carry-over are not implemented.
5. No redemption approval, fulfillment, cancellation, or refund lifecycle.
6. No append-only point ledger or audited corrections.
7. No SSE; polling is used.
8. No automated backup, restore, or export workflow.
9. No supported import of old `familyChoreGame.v1` family data.
10. No parent UI for editing chore or right catalogs.
11. Fonts are loaded from Google Fonts when network access is available.

## 14. Current sources of truth

- PostgreSQL: family, members, login-attempt lockouts, catalog copies, assignments, completion, rights, redemptions, and balances.
- `choresAndScores.txt`: packaged chore/category definitions.
- `rights.txt`: packaged right definitions.
- `jewish-school-vacations-2026-2027.txt`: packaged school-vacation ranges.
- Server `Asia/Jerusalem` clock: week permissions, right availability, and daily-cap boundaries.
- `app.js`: PWA rendering, localization, and API interaction.
- `PROJECT_PLAN.md`: future architecture, security, operations, and point-economy requirements.
