#!/bin/bash

set -e

./gradlew clean
./gradlew buildPlugin
./gradlew createPublicationJar
./gradlew :core:publishAllPublicationsToCanvasmcRepository
./gradlew publishPlugin
