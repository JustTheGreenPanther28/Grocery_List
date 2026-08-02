# Grocery List (Spring Boot + Spring Security + HTML CSS JS)

REST API backing the Grocery List app: two hardcoded accounts, JWT-based auth,
per-date grocery items, and a WhatsApp send endpoint.

## Stack
- Java 17, Spring Boot 3.2.5
- Spring Security (JWT, stateless)
- Spring Data JPA + H2 in-memory database (data resets on restart — swap for
  MySQL/Postgres by changing `application.properties` if you want it to persist)
- `jjwt` for token generation/validation
-  HTML CSS JS

## Run it
```bash
cd grocery-backend
mvn spring-boot:run
```
Server starts on `http://localhost:8080`.

## The two hardcoded accounts
Defined in `SecurityConfig.java`:

| username | password    |
|----------|-------------|
| XYZ   | 123  |
| PQR   | 123  |

Change them there (passwords are BCrypt-hashed automatically at startup).

## Authentication flow
1. `POST /api/auth/login` with `{ "username": "...", "password": "..." }`
   returns a JWT.
2. Send that token on every other request:
   `Authorization: Bearer <token>`
3. Token expires after 24h by default (`jwt.expiration-ms` in
   `application.properties`).

**Before deploying anywhere real**, replace `jwt.secret` in
`application.properties` with your own long random string — the one in the
repo is a placeholder.

## API Reference

### `POST /api/auth/login`
```json
// request
{ "username": "XYZ", "password": "123" }
// response
{ "token": "xyz...", "username": "XYZ", "expiresInMs": 86400000 }
```

### `GET /api/groceries?date=2026-08-01`
Returns the caller's items for that date.
```json
[
  { "id": 1, "date": "2026-08-01", "name": "Milk", "qty": "1L", "checked": false }
]
```

### `POST /api/groceries`
```json
// request
{ "date": "2026-08-01", "name": "Bread", "qty": "2" }
```

### `PUT /api/groceries/{id}`
Send only the fields you want to change.
```json
{ "checked": true }
```

### `DELETE /api/groceries/{id}`
No body. Returns `204 No Content`.

### `POST /api/whatsapp/send`
Builds the message from that date's saved items for the logged-in user and
either sends it directly (if the WhatsApp Cloud API is configured) or returns
a `wa.me` link for the frontend to open.
```json
// request
{ "date": "2026-08-01", "toNumber": "919876543XXX" }

// response (Cloud API not configured — most common case out of the box)
{
  "sentDirectly": false,
  "message": "WhatsApp Cloud API not configured on the server - open this link to send manually.",
  "waLink": "https://wa.me/919876543XXX?text=..."
}
```

To make it send directly with no manual tap, fill in `application.properties`:
```properties
whatsapp.phone-number-id=YOUR_PHONE_NUMBER_ID
whatsapp.access-token=YOUR_PERMANENT_ACCESS_TOKEN
```
(from Meta for Developers — see the earlier setup steps discussed in chat).
**Never commit real values** — use environment variables in production:
```bash
export WHATSAPP_PHONE_NUMBER_ID=...
export WHATSAPP_ACCESS_TOKEN=...
```
and reference them as `${WHATSAPP_PHONE_NUMBER_ID}` / `${WHATSAPP_ACCESS_TOKEN}`
in `application.properties` instead of hardcoding.

### Usual items (templates)
Reusable items (milk, bread, eggs...) you tap to add instead of retyping.

`GET /api/templates` — list your usual items.
`POST /api/templates` — `{ "name": "Milk", "qty": "1L" }`
`DELETE /api/templates/{id}`
`POST /api/templates/{id}/add?date=2026-08-01` — copies that one item onto the date's list.
`POST /api/templates/add-all?date=2026-08-01` — copies every usual item onto the date's list at once.

### Sending by Gmail
`POST /api/email/send`
```json
{ "date": "2026-08-01", "toEmail": "family@gmail.com" }
```
Requires `GMAIL_USERNAME` (the Gmail address to send *from*) and `GMAIL_APP_PASSWORD` as env vars.
Gmail blocks SMTP login with your normal password — generate an **app password** instead:
Google Account → Security → 2-Step Verification (must be on) → App passwords → generate one for
"Mail", and use that 16-character value as `GMAIL_APP_PASSWORD`.

### Send history
`GET /api/whatsapp/history` — every send across **both** channels (WhatsApp and Gmail, manual or
scheduled) for the logged-in user, most recent first, each entry tagged with `channel`. Add
`?date=2026-08-01` to filter to one list's sends.

### Auto-send schedule
One schedule per user: "send today's list every &lt;day&gt; at &lt;time&gt; via &lt;WhatsApp or
Gmail&gt; to &lt;recipient&gt;".

`GET /api/whatsapp/schedule` — current schedule, or `204 No Content` if none is set.
`PUT /api/whatsapp/schedule`
```json
{ "enabled": true, "dayOfWeek": "SATURDAY", "hour": 9, "minute": 0, "channel": "EMAIL", "toNumber": "family@gmail.com" }
```
`channel` is `"WHATSAPP"` or `"EMAIL"` (defaults to `WHATSAPP` if omitted). `toNumber` holds a phone
number or an email address depending on the channel.

A background job (`ScheduledSendService`, `@Scheduled(cron = "0 * * * * *")`) checks every minute and
fires the send if the day/hour/minute matches and it hasn't already sent for today.

**Important caveat**: this only fires while the server process is actually running at that minute.
Render's free tier spins the server down after ~15 minutes of no traffic and only wakes it up on
an incoming request — so a scheduled send at, say, 9:00 AM will simply be missed if the app was
asleep. Options if you want this reliable:
- Upgrade to a Render plan that doesn't sleep.
- Use a free external cron pinger (e.g. cron-job.org, UptimeRobot) to hit any endpoint (e.g.
  `GET /api/groceries?date=...` with a valid token, or add a lightweight public health-check route)
  once a minute so the app never fully sleeps.
- Move the scheduled trigger itself external: have the cron pinger call a dedicated
  `/api/whatsapp/trigger-scheduled` endpoint instead of relying on Spring's in-process `@Scheduled` —
  more reliable than hoping the app happens to be awake at the exact minute.

## CORS
Wide open (`*`) by default in `application.properties` so the static HTML
frontend can call it from anywhere while you're developing. Lock this down to
your actual frontend origin before going to production:
```properties
app.cors.allowed-origins=https://your-frontend-domain.com
```

## Connecting the HTML/CSS/JS frontend
The frontend built earlier used `window.storage` (Claude-artifact-only) and a
direct `wa.me` link. To wire it up to this backend instead:
1. On login, `POST /api/auth/login`, store the returned token (e.g. in a JS
   variable — avoid `localStorage` for tokens on shared machines).
2. Replace the `window.storage` calls with `fetch()` calls to
   `/api/groceries`, sending the JWT in the `Authorization` header.
3. Replace the direct `wa.me` window.open with a `fetch()` to
   `/api/whatsapp/send`, then either report success or open the returned
   `waLink`.

Happy to wire that integration up directly if you want the working
end-to-end version next.

## Challenges Faced & How They Were Fixed

Real debugging notes from building this, kept here because they're more
useful long-term (portfolio, interviews, my own memory in six months) than a
sanitized feature list.

### 1. Missing PostgreSQL driver on the classpath
`application.properties` was configured for a Supabase Postgres connection
(`spring.datasource.driver-class-name=org.postgresql.Driver`), but `pom.xml`
only had the `h2` dependency — no `org.postgresql:postgresql`. On startup
this threw `ClassNotFoundException: org.postgresql.Driver`, which cascaded
through `DataSource` → `EntityManagerFactory` → every JPA repository bean
failing to construct, so the actual root cause was buried under several
layers of `UnsatisfiedDependencyException` in the stack trace. **Fix:** add
the driver as a runtime dependency:
```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

### 2. Fragile CSS `:first-child`/`:last-child` sizing
The "add item" form sized its two inputs with
`.add-form input:first-child { flex: 2; }` and `:last-child { flex: 1; }`.
In practice this collapsed the item-name field to near-zero width, making it
impossible to see the text while typing — the qty field rendered fine, which
made the bug look input-specific rather than a sizing issue at first. Fixed
by targeting the input IDs directly instead of relying on sibling-order
selectors, with explicit `min-width: 0` so flex-basis actually applies:
```css
.add-form input#new-item-name { flex: 2 1 0; min-width: 0; }
.add-form input#new-item-qty { flex: 1 1 0; min-width: 0; }
```

### 3. Spring Security blocking the frontend itself (403 before login)
The first `SecurityConfig` looked like this:
```java
.requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
.anyRequest().authenticated()
```
That single `anyRequest().authenticated()` line covers *every* request Spring
sees — not just `/api/**`, but also `/`, `/index.html`, `/css/style.css`,
`/js/app.js`. On a fresh browser with no JWT yet, simply navigating to the
site's root URL got rejected with `403 Access Denied` before the login page
could even render. The failure mode was confusing at first because it looked
like a login bug, when actually login was never reachable — the static asset
request that serves the login *form* itself was being blocked.

Separately, once static files were unblocked, `fetch()` calls from the
frontend to authenticated endpoints (`/api/groceries`, `/api/whatsapp/send`)
still silently failed in the browser with CORS errors. The cause: modern
browsers send an `OPTIONS` "preflight" request before certain cross-origin
`POST`/`PUT`/`DELETE` calls, asking the server if the real request is
allowed. Spring Security was rejecting that `OPTIONS` preflight too (it
carries no `Authorization` header), so the browser cancelled the real request
before it was ever sent — even though the actual `POST`/`PUT` endpoint would
have accepted a valid JWT just fine.

**Fix**, three explicit `permitAll()` rules added ahead of the catch-all:
```java
.authorizeHttpRequests(auth -> auth
        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
        .requestMatchers("/", "/index.html", "/css/**", "/js/**", "/favicon.ico").permitAll()
        .anyRequest().authenticated()
)
```
Important nuance: opening up `OPTIONS` doesn't weaken security. None of the
`@RestController` classes in this project have a handler method for
`OPTIONS` on any path — there's no business logic behind it to execute, it
just returns CORS headers and an empty `200`. The real `GET`/`POST`/`PUT`/
`DELETE` handlers on `/api/**` still require a valid JWT exactly as before;
only the browser's own internal pre-check and the static frontend files were
unblocked.

### 4. The WhatsApp integration — the actual saga
This took far longer than the rest of the backend combined, almost entirely
because of platform gatekeeping rather than application code. Roughly in
order:

**Attempt 1 — Gupshup (a WhatsApp Business Solution Provider / BSP).**
Chosen specifically to *avoid* needing a Facebook account, since Gupshup
handles Meta's business verification on your behalf and lets you sign up
with just an email. Created an app ("GrocerySend"), generated an API key,
and used their built-in Sandbox test-send tool. First few attempts silently
did nothing — turned out the *recipient* number has to opt in first by
sending any WhatsApp message to Gupshup's shared sandbox number
(`+91 XXXXXXXXX`); WhatsApp requires this on the recipient's side regardless
of provider, it's not a Gupshup-specific step. After opting in, sends
returned `{"status":"submitted"}` — success from the API's point of view —
but nothing ever arrived on WhatsApp itself, and checking the wallet
afterward showed real money had been deducted per test send, despite the
dashboard describing sandbox testing as free-to-test. Concluded Gupshup
wasn't viable for a zero-budget student project and abandoned it, but not
before losing a small amount of wallet balance figuring that out.

**Attempt 2 — Meta's Cloud API, direct.** Turns out this is actually the
cheapest and most reliable path: the API itself costs nothing to use, Meta
gives 1,000 free "service conversation" replies per WhatsApp Business Account
per month, and even outbound business-initiated messages are billed far
below what a BSP markup adds on top. The catch is it requires a real Facebook
account and a Meta Developer / Business Portfolio setup, which is exactly
what the Gupshup route was chosen to avoid. Working through it surfaced a
chain of friction specific to using an old, rarely-used personal account:
- The phone number needed for account verification was already registered as
  the *2FA* number on an older personal Facebook account, so Facebook
  rejected reusing it to verify a second (new) account — resolved by logging
  into the *existing* account instead of creating a new one, and later,
  separately, using a family member's number for one specific verification
  step where a conflict still occurred.
- After finally getting a Facebook login working and creating the Meta app,
  attempting to create a Business Portfolio (required to attach a WhatsApp
  use case to an app) failed outright with *"Your Facebook account is too
  new to create a business. Try again in an hour."* — an anti-fraud
  cooldown Meta applies to freshly created/reactivated accounts, undocumented
  anywhere in the setup flow itself. Simply waiting and retrying resolved it.

**Business verification — deliberately not pursued.** Full Meta Business
Verification (needed to message *any* number, not just pre-approved ones)
requires documentation issued to a registered legal business — GST
certificate, incorporation papers, or equivalent — which an individual
student project doesn't have and shouldn't fake. Given that constraint, this
app intentionally stays in **WhatsApp test mode**: messages can only reach a
small list (≈5) of numbers manually added and OTP-verified in the Meta
dashboard. That's a real functional limitation, but it's an acceptable one
here since the app only ever has two hardcoded users — the test-mode
recipient cap and the app's actual intended audience happen to line up.

**The 24-hour messaging window.** Even with a working Phone Number ID and a
permanent access token (generated via a Meta *System User*, which — unlike
the default 24-hour temporary token shown on the API setup page — doesn't
expire on its own), a real send from the app still failed at first. Cause:
Meta only allows free-form `type: "text"` messages within 24 hours of the
*recipient* last messaging the business number — outside that window, only
pre-approved message *templates* are allowed (confirmed this by successfully
sending Meta's built-in `hello_world` template via curl, while a plain
`text` send through the app's own code failed). This is exactly why
`WhatsAppService.sendGroceryList()` never hard-fails on the caller: if the
Cloud API call throws for *any* reason — unconfigured credentials, an
expired token, or the recipient being outside the messaging window — it
catches the exception and falls back to returning a `wa.me` deep link
instead, so the feature degrades gracefully to "tap send yourself" rather
than breaking outright.

## Architecture & Design Decisions

Honest note: this project is small and single-purpose enough that it never
needed load testing or rate limiting the way a public-facing project would
(a separate project, IET Scroll, uses Bucket4j for that at higher scale).
What follows are the actual design choices behind the structure, plus real
measured latency rather than a guess.

### A quick latency check
Using Postman's Collection Runner against `GET /api/groceries`, individual
requests came back at **87ms, 200ms, and 207ms** across a handful of runs —
noticeably more variance than you'd expect from raw JSON serialization of a
handful of rows. The likely explanation: the database is a remote Supabase
Postgres instance, not local, so each request pays a real network round-trip
to reach it — and that round-trip time is what's actually jittering here,
not the number of grocery items being returned (at this app's scale, a
handful of rows vs. a few dozen makes a negligible difference to query time
compared to the fixed network hop). A more rigorous version of this check
would pin the item count constant across runs and increase sample size to
get a stable p50/p95, but even this rough pass was enough to identify where
the time is actually going: network distance to the DB, not query
complexity.

- **Layered structure** (`controller` → `service` → `repository`), rather
  than putting persistence logic directly in controllers. Each service owns
  its business rules (e.g. "an item can only be fetched/updated/deleted by
  the user who owns it"); controllers stay thin translation layers between
  HTTP and that service.
- **DTOs in, DTOs/entities out, never raw entities in.** Request DTOs are
  separate types from response DTOs, even though they largely mirror the
  same fields — this keeps the wire contract stable if a JPA entity ever
  needs internal fields the client shouldn't see or set directly.
- **Per-user data isolation done at the query layer, not the controller.**
  Every repository method that touches a user's data takes the authenticated
  username as part of the query itself, sourced from
  `Authentication.getName()` in the controller. That means there's no code
  path where one user's request can accidentally load another user's item by
  ID — the ownership check is structural, not an `if` statement that could be
  forgotten in some new endpoint later.
- **Stateless JWT auth** instead of server-side sessions, since this API has
  no server-rendered pages and only two hardcoded accounts — no session
  store needed, `JwtAuthFilter` just validates the bearer token on every
  request.
- **Graceful degradation over hard failure** for the WhatsApp integration
  specifically (see above), and the same principle applies to the scheduled
  send feature — treated as a design decision, not just a bug fix, since an
  external platform being unavailable/misconfigured, or the server itself
  being asleep at the scheduled minute (see the Render free-tier caveat
  above), shouldn't be silent failures with no visibility — hence the send
  history endpoint logging every attempt across both channels.

**What's intentionally *not* here, and why:** no rate limiting (no public
traffic to protect against), no caching layer (dataset per user per day is
tiny), no queueing for the WhatsApp/email sends (synchronous calls are fine
at this volume — would revisit with an async queue + retry if this ever
needed to handle many households' worth of sends at once).

## Project layout
```
grocery-backend/
  src/main/java/com/vedant/grocery/
    config/       -> Spring Security + JWT setup
    controller/    -> REST endpoints
    dto/           -> request/response payloads
    exception/     -> centralized error handling
    model/         -> GroceryItem JPA entity
    repository/    -> Spring Data repository
    service/       -> business logic (grocery CRUD, WhatsApp send)
  src/main/resources/application.properties
  pom.xml
```
