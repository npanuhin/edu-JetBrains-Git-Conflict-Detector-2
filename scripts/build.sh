#!/bin/bash
set -e

./gradlew clean build -x test

cp build/libs/*.jar git-conflict-detector.jar

echo "Build completed. JAR created: git-conflict-detector.jar"
