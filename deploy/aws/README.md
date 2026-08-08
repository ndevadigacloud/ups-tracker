# AWS deployment (CloudFormation)

Deploys UPS Tracker to ECS Fargate behind a single ALB, using path-based
routing so the frontend's relative `/api/*` fetches keep working unchanged:

- `/api/shipments*`, `/api/simulate*` → shipment-service (port 8081)
- `/api/facilities*` → facility-service (port 8082)
- everything else → frontend (nginx serving the built React app)

Kafka runs as its own single-task Fargate service, discoverable internally
at `kafka.ups.local:9092` via a Cloud Map private DNS namespace. Both
backend services connect to one MongoDB Atlas cluster, each using its own
database (`shipments`, `facilities`) — no database server is provisioned by
CloudFormation.

Simplifications made deliberately for a portfolio deploy (call these out if
asked in an interview): public subnets only (no NAT gateway, so no idle
cost), single Kafka task (no HA/replication), no HTTPS/ACM cert, no
autoscaling.

## Prerequisites

- AWS CLI v2, configured (`aws configure`) with a user/role that can create
  VPCs, ECS, ALB, ECR, IAM roles, Cloud Map, and CloudWatch Logs.
- Docker, running locally.
- A MongoDB Atlas cluster (free tier is fine): create it at
  https://cloud.mongodb.com, add a database user, and allow network access
  from `0.0.0.0/0` (Atlas requires an allowlist; the ECS tasks get public IPs
  that aren't static, so this is the simplest option for a demo — tighten it
  later with a NAT gateway + static EIP if you want).

## Setup

```bash
cd deploy/aws
cp config.env.example config.env
# edit config.env: set AWS_REGION and MONGO_ATLAS_URI (no db name at the end)
```

## Deploy

```bash
./deploy.sh
```

This will:
1. Deploy `ecr.yaml` (3 ECR repos).
2. Build and push the shipment-service, facility-service, and frontend
   Docker images, tagged with a timestamp.
3. Deploy `main.yaml` (VPC, ECS cluster, Kafka service, both app services,
   ALB) with those image URIs as parameters.
4. Print the ALB URL — open it in a browser.

Re-running `./deploy.sh` after code changes rebuilds and pushes new image
tags and updates the stack in place (ECS does a rolling deployment).

## Tracing a shipment through CloudWatch Logs

All four services log to one log group, `/ecs/ups-tracker`, with a stream
prefix per service (`shipment-service`, `facility-service`, `kafka`,
`frontend`) - see `LogGroup`/`LogConfiguration` in `main.yaml`. Every
business-event log line includes the shipment id, so you can trace one
shipment's whole lifecycle - across both services and the async Kafka hops
in between - with a single CloudWatch Logs Insights query against the log
group:

```
fields @timestamp, @log, @message
| filter @message like /<shipmentId>/
| sort @timestamp asc
```

That should show, in order: the `POST /api/shipments` request in
shipment-service, the `ShipmentCreatedEvent` publish, facility-service's
capacity check (reserved/rejected), the `CapacityResultEvent` publish, and
shipment-service consuming it and pushing the SSE update - which is exactly
the saga flow described in the main README.

## Cleanup

```bash
./cleanup.sh
```

Deletes the main stack (VPC, ECS, ALB — this is where all the hourly cost
lives), empties the ECR repos, then deletes the ECR stack. Nothing in this
deployment has a resource that costs money once both stacks are deleted.
Your MongoDB Atlas cluster is separate and unaffected — clean that up in the
Atlas console if you're done with it.
