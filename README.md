# familyChoreGame

A Hebrew-first, bilingual weekly family chore game. The existing PWA is packaged with a Java 21 Spring Boot API backed by Jdbi and SQLite.

## Run it

Build and start the application:

```bash
./mvnw spring-boot:run
```

Open `http://127.0.0.1:8080`. The server deliberately binds to localhost by default. Set `FAMILY_CHORE_DB` to choose the SQLite file; otherwise it uses `./data/family-chore-game.db`.

The versioned API starts at `/api/v1`; `/actuator/health` provides a basic health check. Family members, assignments, completion state, scores, benefits, and redemptions are stored in SQLite and shared by every browser using this server. Browser storage now holds only language, week, and filter preferences; the old prototype data is left untouched under `familyChoreGame.v1` but is no longer used.

## Logging

Local runs use readable console logs. API requests receive an `X-Request-ID` response header; a safe incoming value is preserved, otherwise the server generates a UUID. The same ID is attached to request, error, and business-event logs so one operation can be followed across the log stream.

The `aws` Spring profile writes ECS-formatted JSON to standard output for ingestion by CloudWatch:

```bash
SPRING_PROFILES_ACTIVE=aws ./mvnw spring-boot:run
```

Production logs contain structured IDs, event names, HTTP status/duration, dates, and point values. They intentionally exclude request bodies, family/member names, cookies, secrets, and SQL parameters. Expected API rejections are logged at `WARN`; unexpected failures are logged at `ERROR` with a stack trace while the API returns only a generic message and request ID.

On a fresh database, the application opens a required family setup window before the chore board. It creates the family name, default language, and initial members together, then loads the catalog. Existing databases with a family continue directly to the board.

## Included

- Hebrew by default, with instant English switching and correct RTL/LTR layout
- Sunday–Friday board and a Saturday rest day
- Jewish state-school calendar coloring: light green school days and light blue weekends/vacations
- Family-member management and color-coded scores
- Assign one or several predefined chores to any day
- Mark chores complete and calculate weekly points
- Browse previous and future weeks
- Spend earned points on benefits configured in `src/main/resources/catalog/benefits.txt`
- Responsive, installable, offline-capable interface

## Current backend slice

- SQLite schema migration plus catalog loading from `src/main/resources/catalog/choresAndScores.txt`
- Persistent members with archive behavior and normalized unique names
- Coming-week assignment creation/deletion with database uniqueness constraints
- Current-week assignment creation/deletion alongside completion controls
- Current-week completion updates with optimistic concurrency
- Server-authoritative `Asia/Jerusalem` week boundaries and score calculation from point snapshots
- Bootstrap and weekly-board JSON endpoints
- PWA API client with shared reads/writes, periodic refresh, connectivity status, and stale-update recovery

Benefits use the format `BENEFIT|id|Hebrew name|English name|point cost|availability|daily cap` and are displayed alphabetically in the active language. Availability is `ALL_WEEK` or `WEEKEND`; the Israeli weekend is Friday and Saturday. A redemption atomically checks its schedule, daily per-member cap, and point balance before deducting the cost; weekly scores remain unchanged.

The 2026–2027 Jewish state-school vacation dates are stored in `src/main/resources/calendar/jewish-school-vacations-2026-2027.txt`. `WEEKEND` benefits are also available on those vacation dates. The included summer range follows the elementary-school/kindergarten July 1 end-of-year boundary; secondary schools finish earlier.

Authentication, authorization, SSE synchronization, backups, and the planned expiration/scheduling rules remain later project phases.

## Tests

Run the complete local suite with:

```bash
./mvnw test
```

The suite covers fresh-install family onboarding, migrations and resource-file catalog loading, Jerusalem week boundaries, API validation, correlation IDs, safe error contracts,
member normalization and archiving, assignment idempotency and uniqueness, concurrent assignment attempts,
week-mode permissions, optimistic completion conflicts, scoring, point snapshots, and packaged PWA/service-worker contracts.
