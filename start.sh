#!/bin/sh

echo "Building Application Jar..."
./gradlew clean build -x test

echo "Building web app image..."
docker build -t campingsite-app:latest .

echo "Starting containers"
docker-compose up

