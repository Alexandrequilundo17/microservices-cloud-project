#!/bin/bash

USERNAME="alexq113"
VERSION="1.0.0"

SERVICES=("user-service" "product-service" "order-service" "api-gateway")

for SERVICE in "${SERVICES[@]}"; do
  echo "=========================================="
  echo "Building $SERVICE..."
  docker build -t $SERVICE:$VERSION ./$SERVICE

  echo "Tagging $SERVICE..."
  docker tag $SERVICE:$VERSION $USERNAME/$SERVICE:$VERSION
  docker tag $SERVICE:$VERSION $USERNAME/$SERVICE:latest

  echo "Pushing $SERVICE..."
  docker push $USERNAME/$SERVICE:$VERSION
  docker push $USERNAME/$SERVICE:latest

  echo "$SERVICE done!"
done

echo "=========================================="
echo "All services pushed to DockerHub!"
