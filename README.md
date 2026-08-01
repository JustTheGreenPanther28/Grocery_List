# Grocery List Backend (Spring Boot + Spring Security)

REST API backing the Grocery List app: two hardcoded accounts, JWT-based auth,
per-date grocery items, and a WhatsApp send endpoint.

## Stack
- Java 17, Spring Boot 3.2.5
- Spring Security (JWT, stateless)
- Spring Data JPA + H2 in-memory database (data resets on restart — swap for
  MySQL/Postgres by changing `application.properties` if you want it to persist)
- `jjwt` for token generation/validation

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
{ "token": "eyJhbGciOi...", "username": "vedant", "expiresInMs": 86400000 }
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
  "waLink": "https://wa.me/919876543210?text=..."
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
