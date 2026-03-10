#!/bin/bash
set -e

REGISTRY=150506369517.dkr.ecr.us-east-1.amazonaws.com
TAG=latest

echo "🔨 Building and pushing all PayFlow services..."

# Función helper
build_push() {
  local SERVICE=$1
  local CONTEXT=$2
  local IMAGE=$REGISTRY/payflow/$SERVICE:$TAG

  echo ""
  echo "📦 [$SERVICE] Building..."
  podman build -t $IMAGE $CONTEXT

  echo "🚀 [$SERVICE] Pushing to ECR..."
  podman push $IMAGE

  echo "✅ [$SERVICE] Done → $IMAGE"
}

# Build JARs primero
echo "☕ Building all JARs..."
cd /home/emperador/projects/payflow
mvn clean package -DskipTests -q
echo "✅ JARs ready"

# Build & Push cada servicio
build_push auth-service        auth-service
build_push wallet-service      wallet-service
build_push transfer-service    transfer-service
build_push transaction-service transaction-service
build_push notification-service notification-service
build_push api-gateway         api-gateway
build_push frontend            frontend

echo ""
echo "🎉 All images pushed to ECR successfully!"
echo ""
echo "Images:"
for svc in auth-service wallet-service transfer-service \
           transaction-service notification-service api-gateway frontend; do
  echo "  → $REGISTRY/payflow/$svc:$TAG"
done
