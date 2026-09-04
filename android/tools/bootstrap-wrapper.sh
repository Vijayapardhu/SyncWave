#!/bin/sh
# Bootstrap the Gradle wrapper for the Android project.
# Needed exactly once after cloning or after this scaffold is generated.
#
# Usage: ./tools/bootstrap-wrapper.sh

set -e
cd "$(dirname "$0")/.."

if command -v gradle >/dev/null 2>&1; then
    echo "Running 'gradle wrapper'..."
    gradle wrapper --gradle-version 8.7 --distribution-type bin
    echo "Done. Try:  ./gradlew --version"
    exit 0
fi

echo "No system 'gradle' on PATH."
echo "Either install Gradle 8.7 (https://gradle.org/install/) and re-run,"
echo "or open the project in Android Studio and let it sync once — that will"
echo "generate the wrapper for you."
exit 1
