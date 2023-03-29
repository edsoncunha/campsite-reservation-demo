#!/bin/sh

echo "Building Application Jar..."
./gradlew clean build -x test

echo "Starting containers"
docker-compose up

