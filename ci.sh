#!/bin/bash

set -e

./gradlew buildPlugin && ./gradlew :core:build
./gradlew createPublicationJar
./gradlew :core:publishAllPublicationsToCanvasmcRepository
./gradlew publishPlugin
