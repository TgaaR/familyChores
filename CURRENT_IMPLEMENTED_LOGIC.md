# familyChoreGame — Currently Implemented Logic

## 1. Scope and architecture

This document describes the application as it works now. Future requirements remain in `PROJECT_PLAN.md`.

The application uses Java 21, Spring Boot, Maven, Jdbi, and SQLite. Spring Boot serves both the existing bilingual PWA and a versioned `/api/v1` JSON API.

- SQLite is the shared source of truth.
- Database changes are applied through ordered SQL migrations.
- Jdbi provides repository access and transaction boundaries.
- The service binds to `127.0.0.1` by default.
- `/actuator/health` provides a local health check.
- The PWA stores only language, displayed-week, and member-filter preferences in `localStorage` under `familyChoreGame.preferences.v2`.
- Old prototype data under `familyChoreGame.v1` is ignored except for a one-time preference fallback.

Authentication, role authorization, SSE push updates, local-data import, automated backups, benefit expiration/carry-over, approval workflows, refunds, and manually managed school-vacation overrides are not implemented.

## 2. Family onboarding and members

A fresh database contains no placeholder family. Before the board opens, a required bilingual setup window collects the family name, default language (`he` or `en`), and one or more initial member names.

`POST /api/v1/families` creates the single family and its initial members in one transaction. A second family is rejected.

Member names are trimmed, have repeated whitespace collapsed, may contain Unicode letters and combining marks with single spaces between words, are limited to 24 characters, and must be unique after Unicode normalization and case folding.

Additional members can be added from the Family Members dialog. Removing a member archives the member instead of deleting historical assignments. Archived members no longer appear in selection controls but remain available for historical display.

## 3. Language and layout

- Hebrew is the default unless onboarding chooses English.
- Hebrew uses `he-IL` and RTL layout; English uses `en-US` and LTR layout.
- Switching language updates UI labels, dates, chore/category/benefit names, and layout direction.
- UI translations live in the `TEXT` object in `app.js`.
- Catalog entries store separate Hebrew and English names.

## 4. Chore catalog

Categories and chores are loaded from `src/main/resources/catalog/choresAndScores.txt`.

```text
CATEGORY|id|Hebrew name|English name
CHORE|id|category id|Hebrew name|English name|points
```

At startup, the loader validates record types, fields, numeric values, unique IDs, and category references, then synchronizes the catalog into SQLite. Categories and chores are returned alphabetically by English name, matching benefits. Removed catalog entries become inactive rather than deleting referenced history. Chore points are copied into assignment `points_snapshot` values, so catalog changes do not rewrite historical scores.

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
- SQLite enforces one chore per member/date, one use of a chore/date, and one assignment per request ID.

Completion is allowed only for current-week assignments. It uses an optimistic `version`; stale changes return HTTP `409 Conflict` and the PWA reloads current data.

## 7. Weekly scoring and member cards

For the displayed week, completed count is the number of completed assignments, weekly score is the sum of their point snapshots, and progress is completed assignment count divided by assignment count.

Each active member card combines weekly progress with the all-time point wallet:

- Large point value: current spendable balance.
- Total chore points: points earned from all completed chores.
- Benefits redeemed: redemption count and combined point cost.
- Completion count and progress bar: the week currently displayed.

Redeeming benefits does not change historical weekly scores; it reduces only the spendable balance.

## 8. Benefits and redemptions

Benefits are loaded from `src/main/resources/catalog/benefits.txt`.

```text
BENEFIT|id|Hebrew name|English name|point cost|availability|daily cap
```

- IDs must be unique and should remain stable after use.
- Point cost and daily cap must be positive integers.
- Availability is `ALL_WEEK` or `WEEKEND`.
- `WEEKEND` currently means Friday, Saturday, or a date in the packaged school-vacation calendar.
- The daily cap applies separately to each member and benefit.
- Benefits are displayed alphabetically in the active language.

The Benefits section lets the user select a member. It displays only benefits available on the server's current Jerusalem date, below that member's daily cap, and affordable with that member's balance. Each benefit shows its cost, availability, and today's usage.

`POST /api/v1/benefit-redemptions` atomically validates the balance and daily cap and stores member/benefit IDs, bilingual name snapshots, cost snapshot, request ID, and UTC timestamp. Retries do not spend twice. A completed chore cannot be returned to incomplete when doing so would make the wallet negative.

The official 2026–2027 vacation calendar is implemented as a packaged resource and makes `WEEKEND` benefits available on those dates. Manual parent-created date/range overrides and automatic annual imports are not implemented.

## 9. API and synchronization

Implemented endpoints:

- `GET /api/v1/bootstrap`
- `POST /api/v1/families`
- `GET /api/v1/weeks/{weekStart}`
- `POST /api/v1/members`
- `DELETE /api/v1/members/{id}`
- `POST /api/v1/assignments`
- `DELETE /api/v1/assignments/{id}`
- `PATCH /api/v1/assignments/{id}/completion`
- `POST /api/v1/benefit-redemptions`

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

The automated suite currently contains 36 passing tests and covers:

- Family onboarding and duplicate-family rejection
- Fresh migrations, V1/V2 upgrade paths, repeat startup, migration-failure rollback, resource catalog loading, and packaged resources
- Jerusalem week boundaries and mutation permissions
- Member validation, normalized uniqueness, and archival
- Assignment idempotency, uniqueness, concurrency, and point snapshots
- Completion/version conflicts and score changes
- Benefit deduction, insufficient funds, payload-safe retry idempotency, and daily caps
- Concurrent redemption protection for final points and final daily-cap slots
- Consistent redemption races with chore uncompletion and member archival
- Protection against reversing already-spent points
- The open Add Chore dialog refresh regression
- School-day/vacation CSS class rendering and light green/light blue color mappings
- PWA and service-worker contracts
- AWS/local logging configuration, request correlation, and safe unexpected-error responses

## 13. Current limitations

1. No authentication, sessions, parent PIN, or role authorization.
2. Anyone who can reach the app can act as any member and manage members.
3. The packaged calendar covers Jewish state schools for 2026–2027 only; there is no annual import or manual override UI.
4. Weekend days are fixed to Friday and Saturday.
5. Points are an all-time earned-minus-spent balance; expiration and two-point carry-over are not implemented.
6. No redemption approval, fulfillment, cancellation, or refund lifecycle.
7. No append-only point ledger or audited corrections.
8. No SSE; polling is used.
9. No automated backup, restore, or export workflow.
10. No supported import of old `familyChoreGame.v1` family data.
11. No parent UI for editing chore or benefit catalogs.
12. Fonts are loaded from Google Fonts when network access is available.

## 14. Current sources of truth

- SQLite: family, members, catalog copies, assignments, completion, benefits, redemptions, and balances.
- `choresAndScores.txt`: packaged chore/category definitions.
- `benefits.txt`: packaged benefit definitions.
- `jewish-school-vacations-2026-2027.txt`: packaged school-vacation ranges.
- Server `Asia/Jerusalem` clock: week permissions, benefit availability, and daily-cap boundaries.
- `app.js`: PWA rendering, localization, and API interaction.
- `PROJECT_PLAN.md`: future architecture, security, operations, and point-economy requirements.
