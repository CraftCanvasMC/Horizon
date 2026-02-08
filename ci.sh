#!/bin/bash

set -e

./gradlew clean
./gradlew createPublicationJar
./gradlew :core:publishAllPublicationsToCanvasmcRepository
