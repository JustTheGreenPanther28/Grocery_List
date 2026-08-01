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
| vedant   | grocery123  |
| family   | grocery456  |

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
{ "username": "vedant", "password": "grocery123" }
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
{ "date": "2026-08-01", "toNumber": "919876543210" }

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
"# Grocery_List" 
"# Grocery_List" 
"# Grocery_List" 
