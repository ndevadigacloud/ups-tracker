# UPS Tracker

Event-driven package shipment & delivery tracking system, modeled after a UPS-style
shipment lifecycle: create shipment → reserve facility capacity (saga) → hub scans → delivery.

## Architecture

- **shipment-service** (Spring Boot, port 8081) — REST API, MongoDB (`shipments` DB),
  publishes `shipment-created` to Kafka, consumes `capacity-reserved`/`capacity-rejected`
  and `scan-event`, streams live status to the browser over SSE, exposes a `$facet`
  aggregation-backed dashboard endpoint.
- **facility-service** (Spring Boot, port 8082) — MongoDB (`facilities` DB), consumes
  `shipment-created`, atomically reserves capacity via a Mongo `findAndModify` +
  `$expr` guard (prevents overbooking under concurrent requests), publishes the
  saga result back to Kafka.
- **frontend** (React + Vite, port 5173) — create-shipment form (react-hook-form),
  shipment list (React Query, polling), live tracking timeline (SSE via a custom hook),
  ops dashboard (Recharts), facility capacity view. Proxies `/api/*` to the two backends.
- **Kafka** — topics: `shipment-created`, `capacity-reserved`, `capacity-rejected`, `scan-event`.
- **MongoDB** — one database per service.

## Running locally

1. Start infra:
   ```
   docker compose up -d
   ```
   This starts Mongo, Kafka (KRaft mode), and Kafka UI at http://localhost:8090.

2. Start the backends (each in its own terminal):
   ```
   cd shipment-service && mvn spring-boot:run
   cd facility-service && mvn spring-boot:run
   ```
   `facility-service` seeds 3 demo facilities on first boot.

3. Start the frontend:
   ```
   cd frontend
   npm install
   npm run dev
   ```
   Open http://localhost:5173.

## Demo flow

1. Create a shipment on the home page (pick origin/destination facilities).
2. You're redirected to the tracking page — within a second or two the status flips to
   `CAPACITY_RESERVED` (or `CAPACITY_REJECTED` if you overload a facility), pushed live via SSE.
3. Click the "Simulate hub scan" buttons to push `scan-event`s through Kafka and watch the
   tracking timeline update in real time.
4. Check the Dashboard page for aggregate stats and the Facilities page for capacity utilization.
