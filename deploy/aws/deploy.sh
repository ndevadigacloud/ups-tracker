#!/usr/bin/env bash
# End-to-end deploy: ECR stack -> build/push images -> main stack (VPC, ECS, ALB, DocumentDB).
# Safe to re-run: both `aws cloudformation deploy` calls are idempotent updates.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"

CONFIG_FILE="$SCRIPT_DIR/config.env"
if [[ ! -f "$CONFIG_FILE" ]]; then
  echo "Missing $CONFIG_FILE. Copy config.env.example to config.env and fill it in." >&2
  exit 1
fi
# shellcheck disable=SC1090
source "$CONFIG_FILE"

: "${AWS_REGION:?set in config.env}"
: "${STACK_PREFIX:?set in config.env}"
: "${MONGO_ATLAS_URI:?set in config.env}"

ECR_STACK="${STACK_PREFIX}-ecr"
MAIN_STACK="${STACK_PREFIX}-main"
IMAGE_TAG="$(date +%Y%m%d%H%M%S)"
ACCOUNT_ID="$(aws sts get-caller-identity --query Account --output text)"

echo "==> [1/5] Deploying ECR stack ($ECR_STACK)"
aws cloudformation deploy \
  --region "$AWS_REGION" \
  --stack-name "$ECR_STACK" \
  --template-file "$SCRIPT_DIR/ecr.yaml"

SHIPMENT_REPO_URI="$(aws cloudformation describe-stacks --region "$AWS_REGION" --stack-name "$ECR_STACK" \
  --query "Stacks[0].Outputs[?OutputKey=='ShipmentServiceRepoUri'].OutputValue" --output text)"
FACILITY_REPO_URI="$(aws cloudformation describe-stacks --region "$AWS_REGION" --stack-name "$ECR_STACK" \
  --query "Stacks[0].Outputs[?OutputKey=='FacilityServiceRepoUri'].OutputValue" --output text)"
FRONTEND_REPO_URI="$(aws cloudformation describe-stacks --region "$AWS_REGION" --stack-name "$ECR_STACK" \
  --query "Stacks[0].Outputs[?OutputKey=='FrontendRepoUri'].OutputValue" --output text)"

echo "==> [2/5] Logging in to ECR"
aws ecr get-login-password --region "$AWS_REGION" \
  | docker login --username AWS --password-stdin "${ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"

echo "==> [3/5] Building and pushing images (tag: $IMAGE_TAG)"
docker build -t "$SHIPMENT_REPO_URI:$IMAGE_TAG" "$ROOT_DIR/shipment-service"
docker push "$SHIPMENT_REPO_URI:$IMAGE_TAG"

docker build -t "$FACILITY_REPO_URI:$IMAGE_TAG" "$ROOT_DIR/facility-service"
docker push "$FACILITY_REPO_URI:$IMAGE_TAG"

docker build -t "$FRONTEND_REPO_URI:$IMAGE_TAG" "$ROOT_DIR/frontend"
docker push "$FRONTEND_REPO_URI:$IMAGE_TAG"

echo "==> [4/5] Deploying main stack ($MAIN_STACK)"
aws cloudformation deploy \
  --region "$AWS_REGION" \
  --stack-name "$MAIN_STACK" \
  --template-file "$SCRIPT_DIR/main.yaml" \
  --capabilities CAPABILITY_IAM \
  --parameter-overrides \
    ShipmentServiceImage="$SHIPMENT_REPO_URI:$IMAGE_TAG" \
    FacilityServiceImage="$FACILITY_REPO_URI:$IMAGE_TAG" \
    FrontendImage="$FRONTEND_REPO_URI:$IMAGE_TAG" \
    MongoAtlasUri="$MONGO_ATLAS_URI"

echo "==> [5/5] Done"
APP_URL="$(aws cloudformation describe-stacks --region "$AWS_REGION" --stack-name "$MAIN_STACK" \
  --query "Stacks[0].Outputs[?OutputKey=='AppUrl'].OutputValue" --output text)"
echo "App URL: $APP_URL"
