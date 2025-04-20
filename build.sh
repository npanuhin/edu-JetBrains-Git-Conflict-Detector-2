#!/bin/bash
set -e

./gradlew clean build

cp build/libs/*.jar git-conflict-detector.jar

echo "Build completed. JAR created: git-conflict-detector.jar"
