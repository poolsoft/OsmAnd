#!/bin/bash
# Test script: Gradle source precedence testi

echo "=== Gradle carlauncher variant source files test ==="
echo ""

# Clean
./gradlew :OsmAnd:clean > /dev/null 2>&1

# Build with verbose
echo "Building carlauncher variant..."
./gradlew :OsmAnd:assembleCarlauncherOpengldebugFatDebug --info 2>&1 | \
    grep -i "MapActivity\.java" | \
    grep -v "^>" | \
    head -20

echo ""
echo "=== End of test ==="
