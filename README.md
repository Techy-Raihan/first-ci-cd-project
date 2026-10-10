# Order Management System

A modular Spring Boot application for learning backend development and for later use in a DevOps portfolio. It manages products, customers, stock, and the full order lifecycle.

> **Local learning project:** there is no authentication or authorization. Do not expose this application directly to the internet or use real customer data.

## Technology versions

- Java 21
- Spring Boot 3.5.16
- Maven 3.9.11 through Maven Wrapper
- PostgreSQL 14 or newer (17 is a good local choice)
- Flyway (version managed by Spring Boot)
- springdoc-openapi 2.8.17
- JUnit 5, Mockito, and AssertJ (versions managed by Spring Boot)

Spring Boot 3.5.16 supports Java 17 through 25, so Java 21 is supported. springdoc 2.8.x is the matching line for Spring Boot 3.5.x.

## Business rules

- SKUs and customer email addresses are unique ignoring letter case. They are stored as uppercase SKUs and lowercase emails.
- Prices must be positive, stock cannot be negative, and all money uses `BigDecimal` with two database decimal places.
- Products are deactivated rather than deleted, so historical order references remain valid.
- Orders require one or more different products. Repeated product IDs are rejected; the client must combine the quantities.
- The server reads current prices, stores SKU/name/price snapshots, calculates line totals and the order total, then deducts stock in one transaction.
- Product rows are locked with PostgreSQL pessimistic write locks in ID order. Concurrent orders cannot spend the same stock.
- Lifecycle: `CREATED -> CONFIRMED -> SHIPPED -> DELIVERED`.
- Only `CREATED` or `CONFIRMED` orders can be cancelled. Cancellation locks the order and products, restores stock once, and changes the status to `CANCELLED` in one transaction.

## Project structure

```text
src/main/java/com/example/orders/
  product/        product entity, repository, DTOs, service, controller, mapper
  customer/       customer feature
  order/          order and order-item feature, locking and lifecycle logic
  shared/         errors, pagination, timestamps, OpenAPI, dev seed data
src/main/resources/
  db/migration/   Flyway SQL migrations
  application*.yml
src/test/
  ...Test.java    fast unit and MVC tests
  ...IT.java      real PostgreSQL integration/concurrency tests
requests/
  order-management.http
```

## Prerequisites on Windows

Open PowerShell as your normal user. Install a Java 21 JDK if needed:

```powershell
winget install EclipseAdoptium.Temurin.21.JDK
java -version
```

If `java` is not found after installation, open a new PowerShell window. Set `JAVA_HOME` to the installed JDK folder if the wrapper asks for it. For example (adjust the folder to the installed version):

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.x-hotspot'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```

Install PostgreSQL using the official Windows installer from <https://www.postgresql.org/download/windows/>. During setup, keep port `5432`, install Command Line Tools, and remember the `postgres` administrator password. You can also find an available winget package first:

```powershell
winget search PostgreSQL.PostgreSQL
```

After installation, open a new PowerShell window and add the actual PostgreSQL `bin` folder to this session if `psql` is not found:

```powershell
$env:Path += ';C:\Program Files\PostgreSQL\17\bin'
psql --version
```

## Create the databases and user

Connect as the administrator. `psql` prompts for the password selected during installation:

```powershell
psql -U postgres -h localhost
```

Run these SQL statements inside `psql`:

```sql
CREATE USER order_app WITH PASSWORD 'choose_a_strong_local_password';
CREATE DATABASE order_management OWNER order_app;
CREATE DATABASE order_management_test OWNER order_app;
\q
```

The separate `_test` database is important. Integration tests refuse to clean any database whose name does not end in `_test`. Flyway clean is also disabled.

## Environment variables

`.env.example` is documentation only. Spring Boot does **not** automatically load `.env` files.

Set variables in the current PowerShell window before running the app:

```powershell
$env:ORDER_DB_URL = 'jdbc:postgresql://localhost:5432/order_management'
$env:ORDER_DB_USERNAME = 'order_app'
$env:ORDER_DB_PASSWORD = 'choose_a_strong_local_password'
$env:SPRING_PROFILES_ACTIVE = 'dev'
```

These values last only for that PowerShell session. To save them for future terminals, use `setx` and then open a new terminal:

```powershell
setx ORDER_DB_URL 'jdbc:postgresql://localhost:5432/order_management'
setx ORDER_DB_USERNAME 'order_app'
setx ORDER_DB_PASSWORD 'choose_a_strong_local_password'
setx SPRING_PROFILES_ACTIVE 'dev'
```

The `dev` profile runs an idempotent sample-data seeder. It checks unique SKU/email values before inserting and never runs without the `dev` profile. Omit the profile if you want an empty database.

## Run, test, and package

From this project directory:

```powershell
cd D:\order-management-system
.\mvnw.cmd test
.\mvnw.cmd clean package
.\mvnw.cmd spring-boot:run
```

The first wrapper command downloads Maven 3.9.11 and dependencies. Normal unit tests and packaging do not need PostgreSQL because the real-database integration tests are excluded unless the profile below is selected.

When the application starts, Flyway applies the schema and Hibernate validates that its entity mapping matches it.

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>
- Health check: <http://localhost:8080/actuator/health>

Only the basic health endpoint is exposed, without sensitive component details.

## Real PostgreSQL integration tests

Set dedicated test variables and activate the Maven profile:

```powershell
$env:ORDER_TEST_DB_URL = 'jdbc:postgresql://localhost:5432/order_management_test'
$env:ORDER_TEST_DB_USERNAME = 'order_app'
$env:ORDER_TEST_DB_PASSWORD = 'choose_a_strong_local_password'
.\mvnw.cmd verify -Ppostgres-it
```

This profile tests Flyway startup, full transaction rollback, concurrent stock deduction, and concurrent cancellation. It deletes test rows between tests only after verifying the active database name ends with `_test`.

## Complete example flow

The examples below use PowerShell and assume an empty database. Keep the returned IDs instead of assuming they are `1`.

```powershell
$base = 'http://localhost:8080/api/v1'

$customer = Invoke-RestMethod -Method Post -Uri "$base/customers" -ContentType 'application/json' -Body (@{
  name = 'Ravi Kumar'; email = 'ravi@example.com'; phone = '+91 98765 43210'
  deliveryAddress = '12 Park Road, Pune, Maharashtra 411001'
} | ConvertTo-Json)

$keyboard = Invoke-RestMethod -Method Post -Uri "$base/products" -ContentType 'application/json' -Body (@{
  sku = 'KEY-100'; name = 'Mechanical Keyboard'; description = 'Quiet switches'
  price = 4999.00; stockQuantity = 10
} | ConvertTo-Json)

$mouse = Invoke-RestMethod -Method Post -Uri "$base/products" -ContentType 'application/json' -Body (@{
  sku = 'MOUSE-100'; name = 'Wireless Mouse'; description = 'USB-C charging'
  price = 1999.00; stockQuantity = 20
} | ConvertTo-Json)

$orderBody = @{
  customerId = $customer.id
  items = @(
    @{ productId = $keyboard.id; quantity = 1 },
    @{ productId = $mouse.id; quantity = 2 }
  )
} | ConvertTo-Json -Depth 4
$order = Invoke-RestMethod -Method Post -Uri "$base/orders" -ContentType 'application/json' -Body $orderBody

Invoke-RestMethod -Method Post -Uri "$base/orders/$($order.id)/confirm"
Invoke-RestMethod -Method Post -Uri "$base/orders/$($order.id)/ship"
Invoke-RestMethod -Method Post -Uri "$base/orders/$($order.id)/deliver"
Invoke-RestMethod -Method Get -Uri "$base/orders/$($order.id)"
```

The total is calculated from stored product prices. Price, SKU, and name in the returned order items are historical snapshots.

## Cancellation and stock restoration example

Create a second order with one keyboard, record its stock after creation, cancel it, and read the product again:

```powershell
$cancelBody = @{
  customerId = $customer.id
  items = @(@{ productId = $keyboard.id; quantity = 1 })
} | ConvertTo-Json -Depth 4
$cancelOrder = Invoke-RestMethod -Method Post -Uri "$base/orders" -ContentType 'application/json' -Body $cancelBody
$afterOrder = Invoke-RestMethod -Uri "$base/products/$($keyboard.id)"
$cancelled = Invoke-RestMethod -Method Post -Uri "$base/orders/$($cancelOrder.id)/cancel"
$afterCancel = Invoke-RestMethod -Uri "$base/products/$($keyboard.id)"
$afterOrder.stockQuantity
$afterCancel.stockQuantity
```

The second value should be exactly one greater. Repeating the cancellation returns `409 INVALID_ORDER_TRANSITION` and does not restore stock again.

## API summary

All endpoints start with `/api/v1`:

- Products: `POST /products`, `GET /products`, `GET /products/{id}`, `PUT /products/{id}`, `DELETE /products/{id}` (deactivate)
- Customers: `POST /customers`, `GET /customers`, `GET /customers/{id}`, `PUT /customers/{id}`
- Orders: `POST /orders`, `GET /orders`, `GET /orders/{id}`
- Lifecycle: `POST /orders/{id}/confirm`, `/ship`, `/deliver`, `/cancel`

Lists accept `page`, `size` (1-100), `sort`, and `direction` (`asc` or `desc`). Products also accept `search`; orders accept `customerId` and `status`. Each endpoint rejects sort fields outside its documented allow-list.

## Manual API requests

Open `requests/order-management.http` in IntelliJ IDEA or a VS Code REST Client extension. Run requests in order and copy returned IDs into the variables at the top when your database does not use IDs `1`, `2`, and `3`.

## Troubleshooting

- **`JAVA_HOME is not set`:** set it to the JDK 21 folder, not its `bin` folder, then verify `& "$env:JAVA_HOME\bin\java.exe" -version`.
- **`psql` is not recognized:** add `C:\Program Files\PostgreSQL\<version>\bin` to `Path`, or run `psql.exe` by full path.
- **Connection refused:** open Windows Services, verify the `postgresql-x64-*` service is running, and confirm port `5432`.
- **Password authentication failed:** make the environment-variable password match the `order_app` database user. Avoid special PowerShell interpolation by using single quotes.
- **Database does not exist:** create both databases using the SQL above and confirm with `psql -U order_app -h localhost -d order_management`.
- **Flyway validation error:** do not hand-edit tables. Check migration history with `SELECT * FROM flyway_schema_history ORDER BY installed_rank;`.
- **Port 8080 already in use:** run `$env:SERVER_PORT = '8081'` before starting.
- **Wrapper download blocked:** configure your network/proxy or install Maven 3.9.x temporarily and run `mvn test`. Do not commit downloaded credentials or proxy passwords.
- **Dev samples do not appear:** confirm `$env:SPRING_PROFILES_ACTIVE` is `dev`. Existing sample records are not overwritten.

## Known limitations and future work

- No authentication, authorization, audit user, payment processing, frontend, or outbound notifications.
- Customer contact and address data are stored as plain application data; use fake local data only.
- No idempotency key for order creation. A client retry can create a separate order if the first response is lost.
- Product updates set absolute stock. A later inventory feature should record stock movements instead.
- No order search by date range, reporting, returns, partial fulfilment, or soft deletion of customers.
- The API returns Spring Data's page JSON shape. A stable custom page DTO can be added if this becomes a public contract.
- CI/CD, Docker, Kubernetes, Jenkins, GitHub Actions, Terraform, and cloud deployment are deliberately deferred to the next phase.

## Ready-for-CI/CD checklist

- [ ] `java -version` reports Java 21.
- [ ] `.\mvnw.cmd test` passes without PostgreSQL.
- [ ] `.\mvnw.cmd clean package` creates the executable JAR.
- [ ] PostgreSQL development and `_test` databases are separate.
- [ ] `.\mvnw.cmd verify -Ppostgres-it` passes against the `_test` database.
- [ ] Application starts and Flyway reports migration success.
- [ ] `/actuator/health` returns `{"status":"UP"}`.
- [ ] Swagger UI opens and the customer/product/order flow succeeds.
- [ ] A cancelled order restores stock once; a repeated cancellation returns 409.
- [ ] Concurrent integration test proves two orders cannot oversell five units.
- [ ] No real secrets or `.env` file are committed.
