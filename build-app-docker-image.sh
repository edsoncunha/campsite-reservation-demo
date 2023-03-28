#!/bin/sh

echo "Building Application Jar..."
./gradlew clean build -x test

echo "Building App image..."
docker build -t campsite-app:latest .

