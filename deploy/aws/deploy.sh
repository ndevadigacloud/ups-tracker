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

# Fargate runs amd64 by default. Building with the plain `docker build` on an
# Apple Silicon Mac produces an arm64-only image, which fails to start on ECS
# with an exec-format/architecture error. buildx builds (and pushes, in one
# step via --push) a multi-arch manifest covering both, so the same image
# works whether it's built on Apple Silicon, Intel, or CI.
PLATFORMS="linux/amd64,linux/arm64"
BUILDER_NAME="ups-tracker-builder"
if ! docker buildx inspect "$BUILDER_NAME" >/dev/null 2>&1; then
  echo "    creating buildx builder '$BUILDER_NAME' (docker-container driver, required for multi-arch)"
  docker buildx create --name "$BUILDER_NAME" --driver docker-container --bootstrap
fi
docker buildx use "$BUILDER_NAME"

echo "==> [3/5] Building and pushing multi-arch images ($PLATFORMS, tag: $IMAGE_TAG)"
docker buildx build --platform "$PLATFORMS" -t "$SHIPMENT_REPO_URI:$IMAGE_TAG" --push "$ROOT_DIR/shipment-service"
docker buildx build --platform "$PLATFORMS" -t "$FACILITY_REPO_URI:$IMAGE_TAG" --push "$ROOT_DIR/facility-service"
docker buildx build --platform "$PLATFORMS" -t "$FRONTEND_REPO_URI:$IMAGE_TAG" --push "$ROOT_DIR/frontend"

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
KAFKA_UI_URL="$(aws cloudformation describe-stacks --region "$AWS_REGION" --stack-name "$MAIN_STACK" \
  --query "Stacks[0].Outputs[?OutputKey=='KafkaUiUrl'].OutputValue" --output text)"
echo "App URL:      $APP_URL"
echo "Kafka UI URL: $KAFKA_UI_URL"
