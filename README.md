# Rasoi

Food delivery for a small set of Indian kitchens. Guests browse active restaurants and available dishes. Customers place a cash-on-delivery order. Admins move that order through the kitchen. Older Stripe rows, if any, stay readable, but new orders cannot use Stripe or a client-supplied payment reference.

## Tech stack

- Java 17, Spring Boot 3.3, Spring Web, Spring Data JPA, Spring Security, Spring Validation
- JWT authentication (8-hour tokens by default)
- SpringDoc OpenAPI (Swagger UI)
- MySQL 8, with a dedicated application user named `rasoi`
- React 18 (Vite), React Router v6, Axios, Tailwind CSS
- Maven, npm, and Docker Compose

## First-time configuration (PowerShell)

From this folder:

```powershell
cd "C:\freelance projects\Food delivery system\fooddeliverysystem"
Copy-Item .env.example .env
```

Generate values, then paste them into `.env` in place of the placeholders. `JWT_SECRET` is used as raw text and must be at least 32 characters. 

```powershell
$jwt = [Convert]::ToBase64String((1..48 | ForEach-Object { Get-Random -Maximum 256 }))
$db = -join ((48..57 + 65..90 + 97..122 | Get-Random -Count 24 | ForEach-Object { [char]$_ }))
$root = -join ((48..57 + 65..90 + 97..122 | Get-Random -Count 24 | ForEach-Object { [char]$_ }))
$admin = -join ((48..57 + 65..90 + 97..122 | Get-Random -Count 20 | ForEach-Object { [char]$_ }))
$customer = -join ((48..57 + 65..90 + 97..122 | Get-Random -Count 20 | ForEach-Object { [char]$_ }))
Write-Output "JWT_SECRET=$jwt"
Write-Output "DB_PASSWORD=$db"
Write-Output "MYSQL_ROOT_PASSWORD=$root"
Write-Output "ADMIN_PASSWORD=$admin"
Write-Output "DEMO_USER_PASSWORD=$customer"
```

Set `ADMIN_EMAIL` to the admin address you want. Leave `APP_DEMO_SEED=false` until you want the sample kitchens.

Changing `ADMIN_PASSWORD` or `DEMO_USER_PASSWORD` later does not rewrite a password that is already stored.

## Start, status, logs, stop, restart

```powershell
docker compose up --build -d
docker compose ps
docker compose logs -f backend
docker compose down
docker compose up -d
```

`docker compose down` stops the containers and keeps the MySQL volume. Orders and users are still there on the next `docker compose up -d`.

- App: http://localhost:5178
- API: http://localhost:8091/api
- Swagger: http://localhost:8091/swagger-ui.html

`WEB_PORT` and `API_PORT` in `.env` change those host ports. MySQL is not published to the host.

## Fresh demo

Set these in `.env`:

```text
APP_DEMO_SEED=true
DEMO_USER_PASSWORD=<the customer password you generated>
ADMIN_EMAIL=admin@fooddelivery.com
ADMIN_PASSWORD=<the admin password you generated>
```

Then create an empty database and start:

```powershell
docker compose down -v
docker compose up --build -d
```

`down -v` deletes the MySQL volume. Use it only when you want to throw away orders and users.

The first boot creates the admin from `ADMIN_EMAIL` / `ADMIN_PASSWORD` if that email is missing, plus two customers if they are missing:

| Email | Password | Role |
| --- | --- | --- |
| value of `ADMIN_EMAIL` | value of `ADMIN_PASSWORD` | ADMIN |
| user@fooddelivery.com | value of `DEMO_USER_PASSWORD` | USER |
| anita.desai@fooddelivery.com | value of `DEMO_USER_PASSWORD` | USER |

It also loads 6 restaurants and 48 dishes from `backend/src/main/resources/data.sql` when the restaurant table is empty. A later start does not insert them again and does not reset passwords. Set `APP_DEMO_SEED=false` after the first boot if you do not want the seeder to look for those demo customers again.

## Run without Docker

Load `.env` into the current PowerShell session, then start MySQL with user `rasoi` and the database `food_delivery`.

```powershell
Get-Content .env | ForEach-Object {
  if ($_ -match '^\s*#' -or $_ -notmatch '=') { return }
  $name, $value = $_.Split('=', 2)
  Set-Item -Path "Env:$($name.Trim())" -Value $value.Trim()
}
cd backend
mvn spring-boot:run
```

In another terminal:

```powershell
cd "C:\freelance projects\Food delivery system\fooddeliverysystem\frontend"
npm ci
npm run dev
```

Vite stays on http://localhost:5173 and proxies `/api` to http://localhost:8080. The API is http://localhost:8080/api and Swagger is http://localhost:8080/swagger-ui.html.

## Order rules

- A new order is cash on delivery. It starts as `PLACED` with payment `PENDING`.
- The server prices every line from the menu. The browser total is only a preview.
- Status moves `PLACED` to `CONFIRMED` to `PREPARING` to `OUT_FOR_DELIVERY` to `DELIVERED`.
- Cancel is allowed from `PLACED` or `CONFIRMED` only. `DELIVERED` and `CANCELLED` are final.
- Repeating the current status does nothing. Any other jump returns HTTP 409 and changes nothing.
- Cash is marked paid only when delivery succeeds. The dashboard revenue total is those collected cash payments, not pending, cancelled, or historical Stripe rows.
- Guests and customers see active restaurants and available dishes from those restaurants. Admins can still open hidden records.
- Removing a dish marks it unavailable so older orders keep their lines.
- A customer who asks for someone else's order gets HTTP 404. A missing or invalid token on a protected API gets HTTP 401.

## Checkout path

`OrderForm.jsx` posts `/api/orders` with the restaurant id, contact details, `paymentMethod: "COD"`, and dish ids with quantities. Nginx or the Vite dev server forwards that to `OrderController`, which validates the body and calls `OrderService.create`. The service rejects Stripe and any payment reference, loads the restaurant and each dish, checks that the kitchen is active and every dish belongs to it and is available, multiplies the stored prices by the quantities, and saves the order and its lines in one transaction. MySQL then holds a `PLACED` / `PENDING` cash order.
