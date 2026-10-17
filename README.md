# familyChoreGame

A Hebrew-first, bilingual weekly family chore game. The existing PWA is packaged with a Java 21 Spring Boot API backed by Jdbi and PostgreSQL.

## Run it

Start the local PostgreSQL 16 database, then start the application:

```bash
docker compose up -d postgres
```

```bash
./mvnw spring-boot:run
```

Open `http://127.0.0.1:8080`. The server deliberately binds to localhost by default. The checked-in Compose service creates a persistent local database with development-only credentials.

Override the connection in other environments with `FAMILY_CHORE_DB_URL`, `FAMILY_CHORE_DB_USERNAME`, and `FAMILY_CHORE_DB_PASSWORD`. The defaults are `jdbc:postgresql://localhost:5432/family_chore_game` and the local Compose username/password `family_chore_game`.

The versioned API starts at `/api/v1`; `/actuator/health` provides a basic health check. Family members, assignments, completion state, scores, rights, and redemptions are stored in PostgreSQL and shared by every browser using this server. Browser storage now holds only language, week, and filter preferences; the old prototype data is left untouched under `familyChoreGame.v1` but is no longer used.

The old `data/family-chore-game.db` SQLite file is not imported automatically. Keep it until its data is either confirmed disposable or migrated separately.

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
- Spend earned points on rights configured in `src/main/resources/catalog/rights.txt`
- Responsive, installable, offline-capable interface

## Current backend slice

- PostgreSQL schema migration plus catalog loading from `src/main/resources/catalog/choresAndScores.txt`
- Persistent members with archive behavior and normalized unique names
- Coming-week assignment creation/deletion with database uniqueness constraints
- Current-week assignment creation/deletion alongside completion controls
- Current-week completion updates with optimistic concurrency
- Server-authoritative `Asia/Jerusalem` week boundaries and score calculation from point snapshots
- Bootstrap and weekly-board JSON endpoints
- PWA API client with shared reads/writes, periodic refresh, connectivity status, and stale-update recovery

Rights use the format `id|Hebrew name|English name|point cost|availability|daily cap` and are displayed alphabetically in the active language. Availability is `ALL_WEEK` or `WEEKEND`; the Israeli weekend is Friday and Saturday. A redemption atomically checks its schedule, daily per-member cap, and point balance before deducting the cost; weekly scores remain unchanged.

The 2026–2027 Jewish state-school vacation dates are stored in `src/main/resources/calendar/jewish-school-vacations-2026-2027.txt`. `WEEKEND` rights are also available on those vacation dates. The included summer range follows the elementary-school/kindergarten July 1 end-of-year boundary; secondary schools finish earlier.

Member/PIN login, parent/child authorization, database-backed login lockout, CSRF protection, and safe authentication logs are implemented. PIN recovery/change, SSE synchronization, backups, and the planned expiration/scheduling rules remain later project phases.

## Tests

Run the complete local suite with:

```bash
./mvnw test
```

Docker must be running because integration and migration tests use an isolated PostgreSQL 16 Testcontainers instance. The tests do not use or modify the local Compose database.

The suite covers fresh-install family onboarding, migrations and resource-file catalog loading, Jerusalem week boundaries, API validation, correlation IDs, safe error contracts,
member normalization and archiving, assignment idempotency and uniqueness, concurrent assignment attempts,
week-mode permissions, optimistic completion conflicts, scoring, point snapshots, and packaged PWA/service-worker contracts.

## Full application data reset

`DELETE /api/v1/data` permanently empties `assignments`, `right_redemptions`,
`member_login_attempts`, `chores`, `chore_categories`, `rights`, `family_members`,
and `families` in one PostgreSQL transaction. It preserves the tables themselves
and all `schema_migrations` records. The endpoint requires a logged-in parent and
a valid CSRF token, returns `204 No Content`, and logs the caller out only after
success. Other sessions belonging to deleted members are invalidated on their
next request. After a successful reset, reload the app to create a new family;
the chore and rights catalogs are loaded again during family setup.

To call it from the browser developer console while logged in as a parent:

```javascript
await fetch('/api/v1/session'); // Ensure the CSRF cookie is available.
const csrfCookie = document.cookie.split('; ').find(value => value.startsWith('XSRF-TOKEN='));
if (!csrfCookie) throw new Error('CSRF cookie unavailable');
const response = await fetch('/api/v1/data', {
  method: 'DELETE',
  headers: { 'X-XSRF-TOKEN': decodeURIComponent(csrfCookie.slice('XSRF-TOKEN='.length)) }
});
if (response.status !== 204) throw new Error(`Reset failed: HTTP ${response.status}`);
location.reload();
```

## Swagger API explorer

Open http://localhost:8080/swagger-ui.html while the application is running.
Expand an endpoint, select **Try it out**, fill in the request, and select **Execute**.
Request and response schemas are included. The OpenAPI JSON is at `/v3/api-docs`.

Log in through the main application in another tab using the same hostname, or
call `GET /api/v1/login-members` and then `POST /api/v1/login` in Swagger.
After login or logout, execute `GET /api/v1/session` to refresh the CSRF cookie.
Swagger automatically sends the session cookie and CSRF header; do not enter a
cookie manually in **Authorize**. On an empty database, complete family setup first.
Parent-only endpoints require a parent session. The Administration section includes
the permanent full-data reset endpoint, with its effects described explicitly.
