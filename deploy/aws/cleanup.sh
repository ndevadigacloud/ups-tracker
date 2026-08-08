#!/usr/bin/env bash
# Tears down everything deploy.sh created, in reverse order. Idempotent -
# safe to re-run if it fails partway (e.g. re-run just to retry the ECR delete).
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

CONFIG_FILE="$SCRIPT_DIR/config.env"
if [[ ! -f "$CONFIG_FILE" ]]; then
  echo "Missing $CONFIG_FILE - can't determine AWS_REGION/STACK_PREFIX." >&2
  exit 1
fi
# shellcheck disable=SC1090
source "$CONFIG_FILE"

: "${AWS_REGION:?set in config.env}"
: "${STACK_PREFIX:?set in config.env}"

ECR_STACK="${STACK_PREFIX}-ecr"
MAIN_STACK="${STACK_PREFIX}-main"

read -rp "This will delete stacks '$MAIN_STACK' and '$ECR_STACK' in $AWS_REGION. Continue? [y/N] " CONFIRM
if [[ "$CONFIRM" != "y" && "$CONFIRM" != "Y" ]]; then
  echo "Aborted."
  exit 0
fi

echo "==> [1/3] Deleting main stack ($MAIN_STACK) - VPC, ECS services, ALB, DocumentDB"
if aws cloudformation describe-stacks --region "$AWS_REGION" --stack-name "$MAIN_STACK" >/dev/null 2>&1; then
  aws cloudformation delete-stack --region "$AWS_REGION" --stack-name "$MAIN_STACK"
  aws cloudformation wait stack-delete-complete --region "$AWS_REGION" --stack-name "$MAIN_STACK"
else
  echo "    $MAIN_STACK does not exist, skipping."
fi

echo "==> [2/3] Emptying ECR repositories (CloudFormation can't delete non-empty repos)"
for REPO in ups-tracker/shipment-service ups-tracker/facility-service ups-tracker/frontend; do
  if aws ecr describe-repositories --region "$AWS_REGION" --repository-names "$REPO" >/dev/null 2>&1; then
    IMAGE_IDS="$(aws ecr list-images --region "$AWS_REGION" --repository-name "$REPO" --query 'imageIds[*]' --output json)"
    if [[ "$IMAGE_IDS" != "[]" ]]; then
      aws ecr batch-delete-image --region "$AWS_REGION" --repository-name "$REPO" --image-ids "$IMAGE_IDS" >/dev/null
    fi
    echo "    emptied $REPO"
  fi
done

echo "==> [3/3] Deleting ECR stack ($ECR_STACK)"
if aws cloudformation describe-stacks --region "$AWS_REGION" --stack-name "$ECR_STACK" >/dev/null 2>&1; then
  aws cloudformation delete-stack --region "$AWS_REGION" --stack-name "$ECR_STACK"
  aws cloudformation wait stack-delete-complete --region "$AWS_REGION" --stack-name "$ECR_STACK"
else
  echo "    $ECR_STACK does not exist, skipping."
fi

echo "==> Done. Verify in the AWS Console that no ECS/ALB/DocumentDB/VPC resources remain billing."
